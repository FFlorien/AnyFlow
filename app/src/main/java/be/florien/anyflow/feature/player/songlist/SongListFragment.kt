package be.florien.anyflow.feature.player.songlist

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.R
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.databinding.FragmentSongListBinding
import be.florien.anyflow.databinding.ItemSongBinding
import be.florien.anyflow.feature.BaseFragment
import be.florien.anyflow.feature.menu.implementation.SearchSongMenuHolder
import be.florien.anyflow.feature.player.PlayerActivity
import be.florien.anyflow.feature.player.info.InfoFragment
import be.florien.anyflow.feature.playlist.selection.SelectPlaylistFragment
import be.florien.anyflow.injection.ActivityScope
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue


/**
 * Display a list of songs and play it upon selection.
 */
@ActivityScope
class SongListFragment : BaseFragment(), DialogInterface.OnDismissListener {
    override fun getTitle(): String = getString(R.string.player_playing_now)

    lateinit var viewModel: SongListViewModel

    private lateinit var binding: FragmentSongListBinding
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var currentSongViewHolder: SongViewHolder
    private var shouldScrollToCurrent = false
    private var shouldHideLoading = false
    private var isLoadingVisible = false

    private val searchMenuHolder by lazy {
        SearchSongMenuHolder(viewModel.isSearching.value == true, requireContext()) {
            val currentState = viewModel.isSearching.value == true
            viewModel.isSearching.value = !currentState
        }
    }

    private val topSet: ConstraintSet
        get() =
            ConstraintSet().apply {
                clone(binding.root as ConstraintLayout)
                clear(R.id.currentSongDisplay, ConstraintSet.BOTTOM)
                connect(
                    R.id.currentSongDisplay,
                    ConstraintSet.TOP,
                    R.id.songList,
                    ConstraintSet.TOP
                )
            }
    private val bottomSet: ConstraintSet
        get() =
            ConstraintSet().apply {
                clone(binding.root as ConstraintLayout)
                clear(R.id.currentSongDisplay, ConstraintSet.TOP)
                connect(
                    R.id.currentSongDisplay,
                    ConstraintSet.BOTTOM,
                    R.id.songList,
                    ConstraintSet.BOTTOM
                )
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel =
            ViewModelProvider(this, (requireActivity() as PlayerActivity).viewModelFactory)[SongListViewModel::class.java]
        binding = FragmentSongListBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        currentSongViewHolder =
            SongViewHolder(binding.root as ViewGroup, binding.currentSongDisplay)
        currentSongViewHolder.isCurrentSong = true
        (requireActivity() as PlayerActivity).menuCoordinator.addMenuHolder(searchMenuHolder)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.songList.adapter = SongAdapter()

        viewLifecycleOwner.lifecycleScope.launch {
            (binding.songList.adapter as SongAdapter).loadStateFlow.collectLatest {
                updateLoadingVisibility(it.refresh is LoadState.Loading)
            }
        }
        linearLayoutManager = LinearLayoutManager(activity)
        binding.songList.layoutManager = linearLayoutManager
        binding.songList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateCurrentSongDisplay()
            }
        })
        binding.songList.addOnItemTouchListener(SongInfoTouchListener())
        binding.currentSongDisplay.songLayout.songInfo.setBackgroundResource(R.color.selected)
        binding.currentSongDisplayTouch.setOnClickListener {
            scrollToCurrentSong()
        }
        binding.currentSongDisplayTouch.setOnTouchListener(SongInfoTouchListener())

        binding.currentSongDisplay.root.elevation = resources.getDimension(R.dimen.smallDimen)
        binding.currentSongDisplayTouch.elevation = resources.getDimension(R.dimen.mediumDimen)
        binding.loadingText.elevation = resources.getDimension(R.dimen.mediumDimen)
        viewModel.pagedAudioQueue.observe(viewLifecycleOwner) {
            if (it != null) {
                (binding.songList.adapter as SongAdapter)
                    .submitData(viewLifecycleOwner.lifecycle, it)
                shouldScrollToCurrent = true
            }
        }
        viewModel.currentSong.observe(viewLifecycleOwner) {
            currentSongViewHolder.bind(it)
        }
        viewModel.listPosition.observe(viewLifecycleOwner) {
            (binding.songList.adapter as SongAdapter).setSelectedPosition(it)
            updateCurrentSongDisplay()
            if (shouldScrollToCurrent) {
                scrollToCurrentSong()
                shouldScrollToCurrent = false
            }
        }
        viewModel.isSearching.observe(viewLifecycleOwner) {
            searchMenuHolder.changeState(!it)
            val imm: InputMethodManager? =
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            if (it) {
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.search.requestFocus()
                    imm?.showSoftInput(binding.search, InputMethodManager.SHOW_IMPLICIT)
                }, 200)
            } else {
                imm?.hideSoftInputFromWindow(binding.root.windowToken, 0)
            }
        }
        viewModel.searchProgression.observe(viewLifecycleOwner) {
            if (it >= 0) {
                linearLayoutManager.scrollToPositionWithOffset(
                    viewModel.searchResults.value?.value?.get(
                        it
                    )?.toInt() ?: it, 0
                )
            }
            updateCurrentSongDisplay()
        }
        viewModel.playlistListDisplayedFor.observe(viewLifecycleOwner) {
            if (it > 0 && childFragmentManager.findFragmentByTag("playlist") == null) {
                SelectPlaylistFragment(it).show(childFragmentManager, "playlist")
            }
        }
        viewModel.quickActions.observe(viewLifecycleOwner) {
            updateQuickActions()
        }
        shouldHideLoading = true
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshSongs()
        viewModel.refreshQuickActions()
        searchMenuHolder.isVisible = true
    }

    override fun onPause() {
        super.onPause()
        searchMenuHolder.isVisible = false
    }

    override fun onDestroy() {
        super.onDestroy()
        (requireActivity() as PlayerActivity).menuCoordinator.removeMenuHolder(searchMenuHolder)
    }

    override fun onDismiss(dialog: DialogInterface?) {
        viewModel.clearPlaylistDisplay()
    }

    private fun updateCurrentSongDisplay() {
        val firstVisibleItemPosition = linearLayoutManager.findFirstCompletelyVisibleItemPosition()
        val lastVisibleItemPosition = linearLayoutManager.findLastCompletelyVisibleItemPosition()

        if (
            viewModel.listPosition.value in firstVisibleItemPosition..lastVisibleItemPosition
            || (viewModel.searchProgression.value ?: -1) >= 0
        ) {
            binding.currentSongDisplay.root.visibility = View.GONE
            binding.currentSongDisplayTouch.visibility = View.GONE
        } else {

            if (binding.currentSongDisplay.root.visibility != View.VISIBLE) {
                binding.currentSongDisplay.root.visibility = View.VISIBLE
                binding.currentSongDisplayTouch.visibility = View.VISIBLE
            }

            if ((viewModel.listPosition.value ?: 0) < firstVisibleItemPosition) {
                topSet.applyTo(binding.root as ConstraintLayout?)
            } else if ((viewModel.listPosition.value ?: 0) > lastVisibleItemPosition) {
                bottomSet.applyTo(binding.root as ConstraintLayout?)
            }
        }
    }

    private fun updateLoadingVisibility(shouldLoadingBeVisible: Boolean) {
        if (shouldLoadingBeVisible != isLoadingVisible) {
            isLoadingVisible = shouldLoadingBeVisible
            val startValue = if (shouldLoadingBeVisible) 0f else 1f
            val endValue = if (shouldLoadingBeVisible) 1f else 0f

            ObjectAnimator
                .ofFloat(binding.loadingText, "alpha", startValue, endValue).apply {
                    duration = 300
                    interpolator = AccelerateDecelerateInterpolator()
                }.apply {
                    addListener(object : Animator.AnimatorListener {

                        override fun onAnimationStart(animation: Animator) {
                            binding.loadingText.visibility = View.VISIBLE
                        }

                        override fun onAnimationRepeat(animation: Animator) {}

                        override fun onAnimationEnd(animation: Animator) {
                            binding.loadingText.visibility =
                                if (shouldLoadingBeVisible) View.VISIBLE else View.GONE
                        }

                        override fun onAnimationCancel(animation: Animator) {}
                    })
                }
                .start()
        }
    }

    private fun scrollToCurrentSong() {
        binding.songList.stopScroll()

        shouldHideLoading = true
        Handler(Looper.getMainLooper()).postDelayed({
            linearLayoutManager.scrollToPositionWithOffset(viewModel.listPosition.value ?: 0, 0)
            updateLoadingVisibility(false)
        }, 300)
    }

    private fun updateQuickActions() {
        for (childIndex in 0 until binding.songList.childCount) {
            val holder =
                (binding.songList.getChildViewHolder(binding.songList.getChildAt(childIndex)) as SongViewHolder)
            holder.setQuickActions()
        }
        currentSongViewHolder.setQuickActions()
    }

    inner class SongAdapter :
        PagingDataAdapter<SongInfo, SongViewHolder>(object : DiffUtil.ItemCallback<SongInfo>() {
            override fun areItemsTheSame(oldItem: SongInfo, newItem: SongInfo) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: SongInfo, newItem: SongInfo): Boolean =
                oldItem.artistName == newItem.artistName
                        && oldItem.albumName == newItem.albumName
                        && oldItem.title == newItem.title

        }), FastScrollRecyclerView.SectionedAdapter {

        private var lastPosition = 0

        override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
            holder.isCurrentSong = position == viewModel.listPosition.value
            holder.bind(getItem(position))
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = SongViewHolder(parent)

        fun setSelectedPosition(position: Int) {
            notifyItemChanged(lastPosition)
            notifyItemChanged(position)
            lastPosition = position
        }

        override fun getSectionName(position: Int): String = position.toString()
    }

    inner class SongViewHolder(
        parent: ViewGroup,
        internal val binding: ItemSongBinding = ItemSongBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
    ) : RecyclerView.ViewHolder(binding.root) {

        private var startingTranslationX: Float = 0f
        var isCurrentSong: Boolean = false

        init {
            if (parent is RecyclerView) {
                binding.songLayout.songInfo.setOnClickListener {
                    viewModel.play(absoluteAdapterPosition)
                    binding.songLayout.songInfo.translationX = 0F
                    currentSongViewHolder.binding.songLayout.songInfo.translationX = 0F
                }
            }
            binding.songLayout.songInfo.setOnLongClickListener {
                swipeForInfo()
                return@setOnLongClickListener true
            }
            setQuickActions()
        }

        internal fun setQuickActions() {
            val childCount = binding.songActions.childCount
            if (childCount > 2) {
                binding.songActions.removeViews(2, childCount - 2)
            }
            for (action in (viewModel.quickActions.value?.reversed() ?: emptyList())) {
                binding.songActions.addView(
                    (LayoutInflater.from(binding.songLayout.cover.context)
                        .inflate(R.layout.item_action, binding.songActions, false))
                        .apply {
                            findViewById<ImageView>(R.id.action).setImageResource(action.actionType.iconRes)
                            findViewById<ImageView>(R.id.field).setImageResource(action.fieldType.iconRes)
                            setOnClickListener {
                                val song = binding.song
                                if (song != null)
                                    viewModel.executeSongAction(
                                        song,
                                        action.actionType,
                                        action.fieldType
                                    )
                                swipeToClose()
                            }
                        })
            }
        }

        fun bind(song: SongInfo?) {
            binding.song = song
            binding.art = song?.albumId?.let { viewModel.getArtUrl(it) }
            binding.songLayout.songInfo.translationX = if (isCurrentSong) {
                currentSongViewHolder.binding.songLayout.songInfo.translationX
            } else {
                0F
            }
            binding.songLayout.current = isCurrentSong
        }

        fun openInfoWhenSwiped() {
            if (binding.root.visibility == View.VISIBLE && binding.songLayout.songInfo.translationX > binding.infoView.right - 10 && startingTranslationX == 0f) {
                val song = binding.song ?: return
                InfoFragment(song).show(childFragmentManager, "info")
            }
        }

        fun swipeToClose() {
            ObjectAnimator.ofFloat(binding.songLayout.songInfo, View.TRANSLATION_X, 0f).apply {
                duration = 300L
                interpolator = DecelerateInterpolator()
                start()
            }
            ObjectAnimator.ofFloat(binding.songActions, View.TRANSLATION_X, 0f).apply {
                duration = 300L
                interpolator = DecelerateInterpolator()
                start()
            }
            if (isCurrentSong && this != currentSongViewHolder) {
                currentSongViewHolder.swipeToClose()
            }
            startingTranslationX = 0F
        }

        fun swipeToOpen() {
            if (this != currentSongViewHolder) {
                val start = linearLayoutManager.findFirstVisibleItemPosition()
                val stop = linearLayoutManager.findLastVisibleItemPosition()
                val except = absoluteAdapterPosition
                for (i in start..stop) {
                    val songViewHolder =
                        this@SongListFragment.binding.songList.findViewHolderForAdapterPosition(i) as? SongViewHolder
                    if (i != except && songViewHolder != null && songViewHolder.binding.songLayout.songInfo.translationX != 0F) {
                        songViewHolder.swipeToClose()
                    }
                }

                if (isCurrentSong) {
                    currentSongViewHolder.swipeToOpen()
                    this@SongListFragment.binding.currentSongDisplayTouch.translationX =
                        this@SongListFragment.binding.currentSongDisplay.actionsPadding.right - this@SongListFragment.binding.currentSongDisplay.root.width.toFloat()
                } else {
                    currentSongViewHolder.swipeToClose()
                    this@SongListFragment.binding.currentSongDisplayTouch.translationX = 0F

                }
            } else {
                val start = linearLayoutManager.findFirstVisibleItemPosition()
                val stop = linearLayoutManager.findLastVisibleItemPosition()
                for (i in start..stop) {
                    val songViewHolder =
                        this@SongListFragment.binding.songList.findViewHolderForAdapterPosition(i) as? SongViewHolder
                    if (songViewHolder?.isCurrentSong == false && songViewHolder.binding.songLayout.songInfo.translationX != 0F) {
                        songViewHolder.swipeToClose()
                    }
                }
            }
            val translationXEnd = binding.actionsPadding.right - itemView.width.toFloat()
            ObjectAnimator.ofFloat(binding.songLayout.songInfo, View.TRANSLATION_X, translationXEnd)
                .apply {
                    duration = 100L
                    interpolator = DecelerateInterpolator()
                    start()
                }
            startingTranslationX = translationXEnd
        }

        fun swipeForMove(translateX: Float) {
            if (translateX > 0F) {
                val translationToSeeActions = (binding.infoView.right).toFloat()
                val translationToFollowMove = startingTranslationX + translateX
                binding.songLayout.songInfo.translationX =
                    minOf(translationToSeeActions, translationToFollowMove)
                        .coerceAtLeast(startingTranslationX)
            } else if (!viewModel.quickActions.value.isNullOrEmpty()) {
                val translationToSeeActions =
                    (binding.actionsPadding.right - itemView.width).toFloat()
                val translationToFollowMove = startingTranslationX + translateX
                binding.songLayout.songInfo.translationX =
                    maxOf(translationToSeeActions, translationToFollowMove).coerceAtMost(0f)
            }
        }

        private fun swipeForInfo() {
            ObjectAnimator.ofFloat(
                binding.songLayout.songInfo,
                View.TRANSLATION_X,
                binding.infoView.right.toFloat()
            ).apply {
                duration = 200L
                interpolator = DecelerateInterpolator()
                repeatCount = 1
                repeatMode = ValueAnimator.REVERSE
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {}

                    override fun onAnimationEnd(animation: Animator) {}

                    override fun onAnimationCancel(animation: Animator) {}

                    override fun onAnimationRepeat(animation: Animator) {
                        val song = binding.song ?: return
                        InfoFragment(song).show(childFragmentManager, "info")
                    }
                })
                start()
            }
        }
    }

    inner class SongInfoTouchListener : RecyclerView.OnItemTouchListener, View.OnTouchListener {
        private var downTouchX: Float = -1f
        private var downTouchY: Float = -1f
        private var lastTouchX: Float = -1f
        private var lastDeltaX: Float = -1f
        private var hasSwiped: Boolean = false

        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
            return onInterceptTouchEvent(e)
        }

        override fun onTouchEvent(rv: RecyclerView, event: MotionEvent) {
            val childView = rv.findChildViewUnder(downTouchX, downTouchY) ?: return
            val viewHolder = (rv.findContainingViewHolder(childView) as? SongViewHolder) ?: return
            onTouch(viewHolder, event)
        }

        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        }

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            onTouch(currentSongViewHolder, event)
            when (event.actionMasked) {
                MotionEvent.ACTION_UP -> {
                    if (!hasSwiped) {
                        v.performClick()
                    }
                    if (lastDeltaX < -1.0) {
                        binding.currentSongDisplayTouch.translationX =
                            binding.currentSongDisplay.actionsPadding.right - binding.currentSongDisplay.root.width.toFloat()
                    } else {
                        binding.currentSongDisplayTouch.translationX = 0F
                    }
                    onInterceptTouchEvent(event)
                }
                MotionEvent.ACTION_MOVE -> {
                    onInterceptTouchEvent(event)
                }
                else -> onInterceptTouchEvent(event)
            }
            return true
        }

        private fun onTouch(viewHolder: SongViewHolder, event: MotionEvent) {
            when (event.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    viewHolder.swipeForMove(event.x - downTouchX)
                }
                MotionEvent.ACTION_UP -> {
                    if (lastDeltaX < -1.0) {
                        viewHolder.swipeToOpen()
                    } else {
                        viewHolder.openInfoWhenSwiped()
                        viewHolder.swipeToClose()
                    }
                }
            }
        }

        private fun onInterceptTouchEvent(e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downTouchX = e.x
                    lastTouchX = e.x
                    downTouchY = e.y
                    hasSwiped = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = downTouchX - e.x
                    val deltaY = downTouchY - e.y
                    hasSwiped = hasSwiped || deltaX.absoluteValue > deltaY.absoluteValue
                    lastDeltaX = e.x - lastTouchX
                    lastTouchX = e.x
                    return hasSwiped
                }
                MotionEvent.ACTION_UP -> {
                    val stopSwipe = hasSwiped
                    downTouchX = -1f
                    downTouchY = -1f
                    lastTouchX = -1f
                    lastDeltaX = -1f
                    return stopSwipe
                }
            }
            return false
        }
    }
}