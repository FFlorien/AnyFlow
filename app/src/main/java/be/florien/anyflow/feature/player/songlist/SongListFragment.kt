package be.florien.anyflow.feature.player.songlist

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.R
import be.florien.anyflow.data.local.model.SongDisplay
import be.florien.anyflow.databinding.FragmentSongListBinding
import be.florien.anyflow.databinding.ItemSongBinding
import be.florien.anyflow.feature.BaseFragment
import be.florien.anyflow.feature.observeNullable
import be.florien.anyflow.feature.observeValue
import be.florien.anyflow.feature.player.PlayerActivity
import be.florien.anyflow.injection.ActivityScope
import be.florien.anyflow.player.PlayerService
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import timber.log.Timber

/**
 * Display a list of songs and play it upon selection.
 */
@ActivityScope
class SongListFragment : BaseFragment() {
    override fun getTitle(): String = getString(R.string.player_playing_now)

    lateinit var viewModel: SongListViewModel

    private lateinit var binding: FragmentSongListBinding
    private lateinit var linearLayoutManager: LinearLayoutManager
    private var shouldHideLoading = false
    private var isLoadingVisible = false
    private val mainThreadHandler = Handler()

    private val topSet: ConstraintSet
        get() =
            ConstraintSet().apply {
                clone(binding.root as ConstraintLayout)
                clear(R.id.currentSongDisplay, ConstraintSet.BOTTOM)
                connect(R.id.currentSongDisplay, ConstraintSet.TOP, R.id.songList, ConstraintSet.TOP)
            }
    private val bottomSet: ConstraintSet
        get() =
            ConstraintSet().apply {
                clone(binding.root as ConstraintLayout)
                clear(R.id.currentSongDisplay, ConstraintSet.TOP)
                connect(R.id.currentSongDisplay, ConstraintSet.BOTTOM, R.id.songList, ConstraintSet.BOTTOM)
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = ViewModelProvider(this, (requireActivity() as PlayerActivity).viewModelFactory).get(SongListViewModel::class.java)
        binding = FragmentSongListBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.songList.adapter = SongAdapter().apply {
            submitList(viewModel.pagedAudioQueue.value)
        }

        linearLayoutManager = LinearLayoutManager(activity)
        binding.songList.layoutManager = linearLayoutManager
        binding.songList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateCurrentSongDisplay()
            }
        })
        binding.songList.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                if (e.action == MotionEvent.ACTION_MOVE && !viewModel.isFollowingCurrentSong.value) {
                    viewModel.isFollowingCurrentSong.value = false
                }
                return false
            }
        })

        binding.currentSongDisplay.root.setBackgroundResource(R.color.selected)
        binding.currentSongDisplay.root.setOnClickListener {
            viewModel.isFollowingCurrentSong.value = true
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            binding.currentSongDisplay.root.elevation = resources.getDimension(R.dimen.smallDimen)
            binding.loadingText.elevation = resources.getDimension(R.dimen.mediumDimen)
        }
        requireActivity().bindService(Intent(requireActivity(), PlayerService::class.java), viewModel.connection, Context.BIND_AUTO_CREATE)
        viewModel.pagedAudioQueue.observeNullable(viewLifecycleOwner) {
            if (it != null) {
                (binding.songList.adapter as SongAdapter).submitList(it)
                updateLoadingVisibility(true)
            }
        }
        viewModel.listPosition.observeValue(viewLifecycleOwner) {
            (binding.songList.adapter as SongAdapter).setSelectedPosition(it)
            updateScrollPosition()
        }
        viewModel.listPositionLoaded.observeNullable(viewLifecycleOwner) {
            if (viewModel.pagedAudioQueue.value != null && it == true) {
                updateScrollPosition()
            }
        }
        viewModel.isFollowingCurrentSong.observeValue(viewLifecycleOwner) {
            if (it) {
                updateScrollPosition()
            }
        }
        shouldHideLoading = true
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshSongs()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.destroy()
        requireActivity().unbindService(viewModel.connection)
    }

    private fun updateCurrentSongDisplay() {
        val firstVisibleItemPosition = linearLayoutManager.findFirstCompletelyVisibleItemPosition()
        val lastVisibleItemPosition = linearLayoutManager.findLastCompletelyVisibleItemPosition()

        if (viewModel.listPosition.value in firstVisibleItemPosition..lastVisibleItemPosition) {
            binding.currentSongDisplay.root.visibility = View.GONE
        } else {

            if (binding.currentSongDisplay.root.visibility != View.VISIBLE) {
                binding.currentSongDisplay.root.visibility = View.VISIBLE
            }

            if (viewModel.listPosition.value < firstVisibleItemPosition) {
                topSet.applyTo(binding.root as ConstraintLayout?)
            } else if (viewModel.listPosition.value > lastVisibleItemPosition) {
                bottomSet.applyTo(binding.root as ConstraintLayout?)
            }
        }
    }

    private fun updateScrollPosition() {
        Timber.i("Is considering the need to scroll")
        if (viewModel.isFollowingCurrentSong.value) {
            binding.songList.stopScroll()

            if (viewModel.listPositionLoaded.value == true && viewModel.pagedAudioQueue.value != null) {
                Timber.i("Ready to scroll to ${viewModel.listPosition.value}")
                shouldHideLoading = true
                mainThreadHandler.postDelayed({
                    linearLayoutManager.scrollToPositionWithOffset(viewModel.listPosition.value, 0)
                    updateLoadingVisibility(false)
                }, 300)
            } else {
                Timber.i("Preparing scroll to ${viewModel.listPosition.value}")
                viewModel.prepareScrollToCurrent()
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

                            override fun onAnimationStart(animation: Animator?) {
                                binding.loadingText.visibility = View.VISIBLE
                            }

                            override fun onAnimationRepeat(animation: Animator?) {}

                            override fun onAnimationEnd(animation: Animator?) {
                                binding.loadingText.visibility = if (shouldLoadingBeVisible) View.VISIBLE else View.GONE
                            }

                            override fun onAnimationCancel(animation: Animator?) {}
                        })
                    }
                    .start()
        }
    }


    inner class SongAdapter : PagedListAdapter<SongDisplay, SongViewHolder>(object : DiffUtil.ItemCallback<SongDisplay>() {
        override fun areItemsTheSame(oldItem: SongDisplay, newItem: SongDisplay) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: SongDisplay, newItem: SongDisplay): Boolean =
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
            private val binding: ItemSongBinding = ItemSongBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false))
        : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener { viewModel.play(adapterPosition) }
        }

        fun bind(song: SongDisplay?) {
            binding.song = song
        }

        var isCurrentSong: Boolean = false
            set(value) {
                field = value
                val backgroundColor = if (field) R.color.selected else R.color.unselected
                binding.root.setBackgroundColor(ResourcesCompat.getColor(resources, backgroundColor, null))
            }
    }
}