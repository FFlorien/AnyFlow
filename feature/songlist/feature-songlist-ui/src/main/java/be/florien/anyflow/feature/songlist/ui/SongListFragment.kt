package be.florien.anyflow.feature.songlist.ui

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.ComponentName
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
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.common.base.BaseFragment
import be.florien.anyflow.common.di.ActivityScope
import be.florien.anyflow.common.di.ViewModelFactoryProvider
import be.florien.anyflow.common.image.isVisiblePresent
import be.florien.anyflow.component.menu.MenuCoordinatorHolder
import be.florien.anyflow.component.viewholder.PodcastViewHolder
import be.florien.anyflow.component.viewholder.PodcastViewHolderListener
import be.florien.anyflow.component.viewholder.PodcastViewHolderProvider
import be.florien.anyflow.component.viewholder.QueueItemViewHolderListener
import be.florien.anyflow.component.viewholder.QueueItemViewHolderProvider
import be.florien.anyflow.component.viewholder.SongViewHolder
import be.florien.anyflow.component.viewholder.SwipeActionViewHolder
import be.florien.anyflow.feature.player.service.PlayerService
import be.florien.anyflow.feature.podcast.ui.PodcastInfoFragment
import be.florien.anyflow.feature.song.base.domain.model.BaseSongInfoRow
import be.florien.anyflow.feature.song.ui.SongInfoFragment
import be.florien.anyflow.feature.songlist.base.domain.model.QueueItemInfoRow
import be.florien.anyflow.feature.songlist.ui.databinding.FragmentSongListBinding
import be.florien.anyflow.management.queue.model.Chapter
import be.florien.anyflow.management.queue.model.ErrorDisplay
import be.florien.anyflow.management.queue.model.PodcastEpisodeDisplay
import be.florien.anyflow.management.queue.model.QueueItemDisplay
import be.florien.anyflow.management.queue.model.SongDisplay
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


/**
 * Display a list of songs and play it upon selection.
 */
@ActivityScope
class SongListFragment : BaseFragment(), DialogInterface.OnDismissListener,
    QueueItemViewHolderListener, QueueItemViewHolderProvider, PodcastViewHolderListener,
    PodcastViewHolderProvider {
    override fun getTitle(): String = getString(R.string.player_playing_now)

    lateinit var viewModel: SongListViewModel

    private lateinit var binding: FragmentSongListBinding
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var currentSongViewHolder: SongViewHolder
    private lateinit var currentPodcastViewHolder: PodcastViewHolder
    private var shouldHideLoading = false
    private var isLoadingVisible = false
    private var visibilityJob: Job? = null
    private var currentLoadState: LoadState = LoadState.Loading

    private val queueItemAdapter: QueueItemAdapter
        get() = binding.songList.adapter as QueueItemAdapter

    private val orderMenu by lazy {
        OrderMenuHolder(viewModel.isOrdered.value == true, requireContext()) {
            if (viewModel.isOrdered.value == true) {
                viewModel.randomOrder()
            } else {
                viewModel.classicOrder()
            }
        }
    }
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
                clear(R.id.currentQueueItemDisplay, ConstraintSet.BOTTOM)
                connect(
                    R.id.currentQueueItemDisplay,
                    ConstraintSet.TOP,
                    R.id.songList,
                    ConstraintSet.TOP
                )
            }
    private val bottomSet: ConstraintSet
        get() =
            ConstraintSet().apply {
                clone(binding.root as ConstraintLayout)
                clear(R.id.currentQueueItemDisplay, ConstraintSet.TOP)
                connect(
                    R.id.currentQueueItemDisplay,
                    ConstraintSet.BOTTOM,
                    R.id.songList,
                    ConstraintSet.BOTTOM
                )
            }

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(
            requireContext(),
            ComponentName(requireContext(), PlayerService::class.java)
        )
        val oui = MediaController.Builder(requireContext(), sessionToken).buildAsync()
        oui.addListener({
            viewModel.player = oui.get()
        }, MoreExecutors.directExecutor())

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
                (requireActivity() as ViewModelFactoryProvider).viewModelFactory
            )[SongListViewModel::class.java]
        binding = FragmentSongListBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        currentSongViewHolder =
            SongViewHolder(binding.root as ViewGroup, this, this, binding.currentSongDisplay)
        currentSongViewHolder.isCurrent = true
        currentPodcastViewHolder =
            PodcastViewHolder(
                binding.root as ViewGroup,
                this,
                this,
                this,
                this,
                binding.currentPodcastDisplay,
                false
            )
        currentPodcastViewHolder.isCurrent = true


        (requireActivity() as MenuCoordinatorHolder).menuCoordinator.addMenuHolder(orderMenu)
        (requireActivity() as MenuCoordinatorHolder).menuCoordinator.addMenuHolder(searchMenuHolder)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.songList.adapter = QueueItemAdapter(this, this, this, this)

        lifecycleScope.launch {
            (queueItemAdapter).loadStateFlow.collectLatest {
                if (it.refresh == currentLoadState) {
                    visibilityJob?.cancel()
                } else {
                    visibilityJob = lifecycleScope.launch {
                        delay(50)
                        currentLoadState = it.refresh
                        updateLoadingVisibility(it.refresh is LoadState.Loading)
                    }
                }
            }
        }
        linearLayoutManager = LinearLayoutManager(activity)
        binding.songList.layoutManager = linearLayoutManager
        binding.songList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateCurrentItemDisplay()
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
                    (rv.findContainingViewHolder(childView) as? SwipeActionViewHolder) ?: return
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
                        if (currentSongViewHolder.topView.translationX < -1.0) {
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

        binding.currentQueueItemDisplay.elevation = resources.getDimension(R.dimen.smallDimen)
        binding.currentSongDisplayTouch.elevation = resources.getDimension(R.dimen.mediumDimen)
        binding.loadingText.elevation = resources.getDimension(R.dimen.mediumDimen)
        viewModel.pagedAudioQueue.observe(viewLifecycleOwner) {
            if (it != null) {
                queueItemAdapter.submitData(viewLifecycleOwner.lifecycle, it)
                scrollToCurrentSong()
            }
        }
        viewModel.currentSongDisplay.observe(viewLifecycleOwner) {
            currentSongViewHolder.bind(it)
        }
        viewModel.currentPodcastDisplay.observe(viewLifecycleOwner) {
            currentPodcastViewHolder.bind(it)
        }
        viewModel.listPosition.observe(viewLifecycleOwner) {
            queueItemAdapter.setSelectedPosition(it)
            updateCurrentItemDisplay()
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
        viewModel.isOrdered.observe(viewLifecycleOwner) {
            orderMenu.changeState(it)
        }
        viewModel.searchProgression.observe(viewLifecycleOwner) {
            if (it >= 0) {
                linearLayoutManager.scrollToPositionWithOffset(
                    viewModel.searchResults.value?.value?.get(
                        it
                    )?.toInt() ?: it, 0
                )
            }
            updateCurrentItemDisplay()
        }
        viewModel.playlistListDisplayedFor.observe(viewLifecycleOwner) {
            if (it != null) {
                viewModel.navigator.displayPlaylistSelection(
                    childFragmentManager,
                    it.first,
                    it.second.toTagType(),
                    it.third
                )
            }
        }
        viewModel.shortcuts.observe(viewLifecycleOwner) {
            updateShortcuts()
        }
        shouldHideLoading = true
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshSongs()
        viewModel.refreshShortcuts()
        searchMenuHolder.isVisible = true
        orderMenu.isVisible = true
    }

    override fun onPause() {
        super.onPause()
        searchMenuHolder.isVisible = false
        orderMenu.isVisible = false
    }

    override fun onDestroy() {
        super.onDestroy()
        (requireActivity() as MenuCoordinatorHolder).menuCoordinator.removeMenuHolder(orderMenu)
        (requireActivity() as MenuCoordinatorHolder).menuCoordinator.removeMenuHolder(
            searchMenuHolder
        )
    }

    override fun onDismiss(dialog: DialogInterface?) {
        viewModel.clearPlaylistDisplay()
    }

    /**
     * ViewHolder listener
     */

    override fun onItemClick(position: Int) {
        viewModel.select(position)
        currentSongViewHolder.binding.songLayout.songInfo.translationX = 0F
    }

    override fun onShortcut(
        item: QueueItemDisplay,
        row: QueueItemInfoRow<*, *>
    ) {
        viewModel.executeAction(item, row)
    }

    override fun onInfoDisplayAsked(item: QueueItemDisplay) {
        when (item) {
            is SongDisplay -> SongInfoFragment(item.id).show(childFragmentManager, "info")
            is PodcastEpisodeDisplay -> PodcastInfoFragment(item.id).show(childFragmentManager, "podcastInfo")
            ErrorDisplay -> Unit
        }
    }

    override fun onShortcutOpened(position: Int?) {
        if (position != null) {
            val start = linearLayoutManager.findFirstVisibleItemPosition()
            val stop = linearLayoutManager.findLastVisibleItemPosition()
            for (i in start..stop) {
                val songInfoViewHolder =
                    binding.songList.findViewHolderForAdapterPosition(i) as SongViewHolder
                if (i != position && songInfoViewHolder.binding.songLayout.songInfo.translationX != 0F) {
                    songInfoViewHolder.resetSwipePosition()
                }
            }

            if (position == viewModel.listPosition.value) {
                currentSongViewHolder.openShortcutWhenSwiped()
                this@SongListFragment.binding.currentSongDisplayTouch.translationX =
                    this@SongListFragment.binding.currentSongDisplay.actionsPadding.right - this@SongListFragment.binding.currentSongDisplay.root.width.toFloat()
            } else {
                currentSongViewHolder.resetSwipePosition()
                this@SongListFragment.binding.currentSongDisplayTouch.translationX = 0F

            }
        } else {
            val start = linearLayoutManager.findFirstVisibleItemPosition()
            val stop = linearLayoutManager.findLastVisibleItemPosition()
            for (i in start..stop) {
                val songInfoViewHolder =
                    this@SongListFragment.binding.songList.findViewHolderForAdapterPosition(i) as? SongViewHolder
                if (songInfoViewHolder?.isCurrent == false && songInfoViewHolder.binding.songLayout.songInfo.translationX != 0F) {
                    songInfoViewHolder.resetSwipePosition()
                }
            }
        }
    }

    override fun onCurrentShortcutsClosed() {
        currentSongViewHolder.resetSwipePosition()
    }

    /**
     * ViewHolder's provider
     */

    override fun getArtUrl(item: QueueItemDisplay): String = when (item) {
        is SongDisplay -> viewModel.getSongArtUrl(item.albumId)
        is PodcastEpisodeDisplay -> viewModel.getPodcastArtUrl(item.podcastId)
        ErrorDisplay -> ""
    }

    override fun getShortcuts(): List<BaseSongInfoRow> =
        viewModel.shortcuts.value ?: emptyList()

    override fun getCurrentPositionFor() = viewModel.listPosition.value ?: -1

    override fun getCurrentTranslationX() =
        currentSongViewHolder.binding.songLayout.songInfo.translationX

    /**
     * Private methods
     */

    private fun updateCurrentItemDisplay() {
        val isSong = viewModel.currentQueueItemDisplay.value is SongDisplay

        val firstVisibleItemPosition = if (isSong) {
            linearLayoutManager.findFirstCompletelyVisibleItemPosition()
        } else {
            linearLayoutManager.findFirstVisibleItemPosition()
        }
        val lastVisibleItemPosition = if (isSong) {
            linearLayoutManager.findLastCompletelyVisibleItemPosition()
        } else {
            linearLayoutManager.findLastVisibleItemPosition()
        }

        val listPosition = viewModel.listPosition.value ?: 0
        if (
            listPosition in firstVisibleItemPosition..lastVisibleItemPosition
            || (viewModel.searchProgression.value ?: -1) >= 0
        ) {
            binding.currentQueueItemDisplay.isVisiblePresent(false)
            binding.currentSongDisplayTouch.isVisiblePresent(false)
        } else {
            binding.currentSongDisplay.root.isVisiblePresent(isSong)
            binding.currentPodcastDisplay.root.isVisiblePresent(!isSong)

            binding.currentQueueItemDisplay.isVisiblePresent(true)
            binding.currentSongDisplayTouch.isVisiblePresent(true)

            if (listPosition <= firstVisibleItemPosition) {
                topSet.applyTo(binding.root as ConstraintLayout?)
            } else {
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
                            binding.loadingText.isVisiblePresent(true)
                        }

                        override fun onAnimationRepeat(animation: Animator) {}

                        override fun onAnimationEnd(animation: Animator) {
                            binding.loadingText.isVisiblePresent(shouldLoadingBeVisible)
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

    private fun updateShortcuts() {
        for (childIndex in 0 until binding.songList.childCount) {
            val holder =
                (binding.songList.getChildViewHolder(binding.songList.getChildAt(childIndex)) as SongViewHolder)
            holder.setShortcuts()
        }
        currentSongViewHolder.setShortcuts()
    }

    override fun onChapterClicked(time: Long) { //todo viewmodel this ?
        viewModel.player?.seekTo(time * 1000)
    }

    override val chapterObservable: LiveData<Chapter?>
        get() = viewModel.currentChapter
}