package be.florien.anyflow.feature.player.songlist

import android.animation.Animator
import android.animation.ObjectAnimator
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
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.R
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.databinding.FragmentSongListBinding
import be.florien.anyflow.feature.BaseFragment
import be.florien.anyflow.feature.menu.implementation.SearchSongMenuHolder
import be.florien.anyflow.feature.player.PlayerActivity
import be.florien.anyflow.feature.player.info.InfoActions
import be.florien.anyflow.feature.player.info.song.SongInfoFragment
import be.florien.anyflow.feature.playlist.selection.SelectPlaylistFragment
import be.florien.anyflow.injection.ActivityScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


/**
 * Display a list of songs and play it upon selection.
 */
@ActivityScope
class SongListFragment : BaseFragment(), DialogInterface.OnDismissListener,
    SongListViewHolderListener, SongListViewHolderProvider {
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
            ViewModelProvider(
                this,
                (requireActivity() as PlayerActivity).viewModelFactory
            )[SongListViewModel::class.java]
        binding = FragmentSongListBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        currentSongViewHolder =
            SongViewHolder(binding.root as ViewGroup, this, this, null, binding.currentSongDisplay)
        currentSongViewHolder.isCurrentSong = true
        (requireActivity() as PlayerActivity).menuCoordinator.addMenuHolder(searchMenuHolder)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.songList.adapter = SongAdapter(this, this, this::onItemClick)

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
        binding.songList.addOnItemTouchListener(object : SongListTouchAdapter(),
            RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                return onInterceptTouchEvent(e)
            }

            override fun onTouchEvent(rv: RecyclerView, event: MotionEvent) {
                val childView = rv.findChildViewUnder(downTouchX, downTouchY) ?: return
                val viewHolder =
                    (rv.findContainingViewHolder(childView) as? SongViewHolder) ?: return
                onTouch(viewHolder, event)
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
            }
        })
        binding.currentSongDisplay.songLayout.songInfo.setBackgroundResource(R.color.selected)
        binding.currentSongDisplayTouch.setOnClickListener {
            scrollToCurrentSong()
        }
        binding.currentSongDisplayTouch.setOnTouchListener(object : SongListTouchAdapter(),
            View.OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                onTouch(currentSongViewHolder, event)
                when (event.actionMasked) {
                    MotionEvent.ACTION_UP -> {
                        if (!hasSwiped) {
                            v.performClick()
                        }
                        if (currentSongViewHolder.itemInfoView.translationX < -1.0) {
                            binding.currentSongDisplayTouch.translationX =
                                binding.currentSongDisplay.songLayout.songInfo.translationX
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
        })

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

    /**
     * ViewHolder listener
     */

    override fun onQuickAction(
        item: SongInfo,
        action: InfoActions.ActionType,
        field: InfoActions.FieldType
    ) {
        viewModel.executeSongAction(item, action, field)
    }

    override fun onInfoDisplayAsked(item: SongInfo) {
        SongInfoFragment(item).show(childFragmentManager, "info")
    }

    override fun onQuickActionOpened(position: Int?) {
        if (position != null) {
            val start = linearLayoutManager.findFirstVisibleItemPosition()
            val stop = linearLayoutManager.findLastVisibleItemPosition()
            for (i in start..stop) {
                val songViewHolder =
                    binding.songList.findViewHolderForAdapterPosition(i) as? SongViewHolder
                if (i != position && songViewHolder != null && songViewHolder.binding.songLayout.songInfo.translationX != 0F) {
                    songViewHolder.swipeToClose()
                }
            }

            if (position == viewModel.listPosition.value) {
                currentSongViewHolder.openQuickActionWhenSwiped()
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
    }

    override fun onCurrentSongQuickActionClosed() {
        currentSongViewHolder.swipeToClose()
    }

    /**
     * ViewHolder's provider
     */

    override fun getArtUrl(id: Long): String = viewModel.getArtUrl(id)

    override fun getQuickActions(): List<InfoActions.InfoRow> =
        viewModel.quickActions.value ?: emptyList()

    override fun getCurrentPosition() = viewModel.listPosition.value ?: -1

    override fun getCurrentSongTranslationX() =
        currentSongViewHolder.binding.songLayout.songInfo.translationX

    /**
     * Private methods
     */

    private fun onItemClick(position: Int) {
        viewModel.play(position)
        currentSongViewHolder.binding.songLayout.songInfo.translationX = 0F
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
}