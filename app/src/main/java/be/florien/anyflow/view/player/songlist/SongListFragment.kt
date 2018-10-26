package be.florien.anyflow.view.player.songlist

import android.animation.ObjectAnimator
import android.arch.paging.PagedListAdapter
import android.content.Context
import android.content.Intent
import android.databinding.Observable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.util.DiffUtil
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import be.florien.anyflow.BR
import be.florien.anyflow.R
import be.florien.anyflow.databinding.FragmentSongListBinding
import be.florien.anyflow.databinding.ItemSongBinding
import be.florien.anyflow.di.ActivityScope
import be.florien.anyflow.persistence.local.model.SongDisplay
import be.florien.anyflow.player.PlayerService
import be.florien.anyflow.view.BaseFragment
import be.florien.anyflow.view.player.PlayerActivity
import timber.log.Timber
import javax.inject.Inject

/**
 * Display a list of accounts and play it upon selection.
 */
@ActivityScope
class SongListFragment : BaseFragment() {
    override fun getTitle(): String = "Now playing"

    @Inject
    lateinit var vm: SongListFragmentVm

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var binding: FragmentSongListBinding
    private var isListFollowingCurrentSong = true
    private var shouldUpdateLoading = false

    private val topSet by lazy {
        ConstraintSet().apply {
            clone(binding.root as ConstraintLayout)
            clear(R.id.currentSongDisplay, ConstraintSet.BOTTOM)
            connect(R.id.currentSongDisplay, ConstraintSet.TOP, R.id.songList, ConstraintSet.TOP)
        }
    }
    private val bottomSet by lazy {
        ConstraintSet().apply {
            clone(binding.root as ConstraintLayout)
            clear(R.id.currentSongDisplay, ConstraintSet.TOP)
            connect(R.id.currentSongDisplay, ConstraintSet.BOTTOM, R.id.songList, ConstraintSet.BOTTOM)
        }
    }

    init {
        Timber.tag(SongListFragment::class.java.simpleName)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as PlayerActivity).activityComponent.inject(this)
        setHasOptionsMenu(true)
        val intent = Intent(requireContext(), be.florien.anyflow.persistence.UpdateService::class.java)
        intent.data = Uri.parse(be.florien.anyflow.persistence.UpdateService.UPDATE_ALL)
        requireActivity().startService(intent)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSongListBinding.inflate(inflater, container, false)
        (activity as PlayerActivity).activityComponent.inject(this)
        binding.vm = vm
        vm.refreshSongs() //todo communication between service and VM
        binding.songList.adapter = SongAdapter().apply {
            submitList(vm.pagedAudioQueue)
        }

        linearLayoutManager = LinearLayoutManager(activity)
        binding.songList.layoutManager = linearLayoutManager
        binding.songList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateCurrentSongDisplay()

                if (shouldUpdateLoading) {
                    Timber.v("should update loading from scroll")
                    updateLoadingVisibility()
                }
            }
        })
        binding.songList.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView?, e: MotionEvent?): Boolean {
                isListFollowingCurrentSong = vm.listPosition in linearLayoutManager.findFirstCompletelyVisibleItemPosition()..linearLayoutManager.findLastCompletelyVisibleItemPosition()
                return false
            }
        })

        binding.currentSongDisplay.root.setBackgroundResource(R.color.selected)
        binding.currentSongDisplay.root.setOnClickListener {
            isListFollowingCurrentSong = true
            binding.songList.stopScroll()
            linearLayoutManager.scrollToPositionWithOffset(vm.listPosition, 0)
            vm.pagedAudioQueue?.loadAround(vm.listPosition)
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            binding.currentSongDisplay.root.elevation = resources.getDimension(R.dimen.smallDimen)
        }
        requireActivity().bindService(Intent(requireActivity(), PlayerService::class.java), vm.connection, Context.BIND_AUTO_CREATE)
        vm.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(observable: Observable, id: Int) {
                when (id) {
                    BR.pagedAudioQueue -> {
                        isListFollowingCurrentSong = true
                        (binding.songList.adapter as SongAdapter).submitList(vm.pagedAudioQueue)
                    }
                    BR.listPosition -> {
                        val songAdapter = binding.songList.adapter as SongAdapter
                        songAdapter.setSelectedPosition(vm.listPosition)
                        updateCurrentSongDisplay()
                        updateScrollPosition()
                    }
                    BR.upToDate -> {
                        shouldUpdateLoading = true
                        Timber.v("loading: uptodate updated to ${vm.upToDate}")

                        if (!vm.upToDate) {
                            Timber.v("should update loading from vm")
                            updateLoadingVisibility()
                        }
                    }
                }
            }
        })
        updateCurrentSongDisplay()
        updateScrollPosition()
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        vm.destroy()
        requireActivity().unbindService(vm.connection)
    }

    private fun updateCurrentSongDisplay() {
        val firstVisibleItemPosition = linearLayoutManager.findFirstCompletelyVisibleItemPosition()
        val lastVisibleItemPosition = linearLayoutManager.findLastCompletelyVisibleItemPosition()
        if (vm.listPosition in firstVisibleItemPosition..lastVisibleItemPosition) {
            binding.currentSongDisplay.root.visibility = View.GONE
            isListFollowingCurrentSong = true
        } else {

            if (binding.currentSongDisplay.root.visibility != View.VISIBLE) {
                binding.currentSongDisplay.root.visibility = View.VISIBLE
            }

            if (vm.listPosition < firstVisibleItemPosition) {
                topSet.applyTo(binding.root as ConstraintLayout?)
            } else if (vm.listPosition > lastVisibleItemPosition) {
                bottomSet.applyTo(binding.root as ConstraintLayout?)
            }
        }
    }

    private fun updateScrollPosition() {
        if (isListFollowingCurrentSong) {
            linearLayoutManager.scrollToPositionWithOffset(vm.listPosition, 0)
            vm.pagedAudioQueue?.loadAround(vm.listPosition)
        }
    }

    private fun updateLoadingVisibility() {
        val startValue = if (vm.upToDate) 1f else 0f
        val endValue = if (vm.upToDate) 0f else 1f

        Timber.v("loading current alpha: ${binding.loadingText.alpha} and desired: $endValue")
        if (binding.loadingText.alpha != endValue) {
            Timber.v("loading alpha: $endValue")
            ObjectAnimator.ofFloat(binding.loadingText, "alpha", startValue, endValue).apply {
                duration = 300
                interpolator = AccelerateDecelerateInterpolator()
            }.start()
        }
            shouldUpdateLoading = false
    }


    inner class SongAdapter : PagedListAdapter<SongDisplay, SongViewHolder>(object : DiffUtil.ItemCallback<SongDisplay>() {
        override fun areItemsTheSame(oldItem: SongDisplay, newItem: SongDisplay) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: SongDisplay, newItem: SongDisplay): Boolean = oldItem.artistName == newItem.artistName && oldItem.albumName == newItem.albumName && oldItem.title == newItem.title

    }) {

        private var lastPosition = 0

        override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
            holder.isCurrentSong = position == vm.listPosition
            holder.bind(getItem(position), position)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = SongViewHolder(parent)

        fun setSelectedPosition(position: Int) {
            notifyItemChanged(lastPosition)
            notifyItemChanged(position)
            lastPosition = position
        }
    }

    inner class SongViewHolder(
            parent: ViewGroup,
            private val binding: ItemSongBinding = ItemSongBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        : RecyclerView.ViewHolder(binding.root) {

        private var songPosition: Int = 0

        init {
            binding.root.setOnClickListener { vm.play(songPosition) }
        }

        fun bind(song: SongDisplay?, position: Int) {
            this.songPosition = position
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