package be.florien.anyflow.view.player.songlist

import android.Manifest
import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.Observable
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import be.florien.anyflow.BR
import be.florien.anyflow.R
import be.florien.anyflow.databinding.FragmentSongListBinding
import be.florien.anyflow.databinding.ItemSongBinding
import be.florien.anyflow.di.ActivityScope
import be.florien.anyflow.local.DownloadHelper
import be.florien.anyflow.persistence.local.model.SongDisplay
import be.florien.anyflow.player.PlayerService
import be.florien.anyflow.view.BaseFragment
import be.florien.anyflow.view.player.PlayerActivity
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import javax.inject.Inject

/**
 * Display a list of songs and play it upon selection.
 */
@ActivityScope
class SongListFragment : BaseFragment() {
    override fun getTitle(): String = getString(R.string.player_playing_now)

    @Inject
    lateinit var vm: SongListFragmentVm

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var binding: FragmentSongListBinding
    private var isListFollowingCurrentSong = true
    private var isFirstScroll = true
    private var shouldScroll = false
    private var shouldHideLoading = false
    private var isLoadingVisible = false
    private val mainThreadHandler = Handler()
    private var songWaitingToBeDownloaded: SongDisplay? = null
    internal var animDuration: Long = 200L

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
        (activity as PlayerActivity).activityComponent.inject(this)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSongListBinding.inflate(inflater, container, false)
        (activity as PlayerActivity).activityComponent.inject(this)
        binding.vm = vm
        vm.refreshSongs()
        binding.songList.adapter = SongAdapter().apply {
            submitList(vm.pagedAudioQueue)
        }

        linearLayoutManager = LinearLayoutManager(activity)
        binding.songList.layoutManager = linearLayoutManager
        binding.songList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateCurrentSongDisplay()

                if (isListFollowingCurrentSong && linearLayoutManager.findFirstVisibleItemPosition() != vm.listPosition) {
                    updateScrollPosition()
                }
            }
        })
        binding.songList.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                isListFollowingCurrentSong = if (e.action == MotionEvent.ACTION_MOVE) false else isListFollowingCurrentSong
                return false
            }
        })
        animDuration = binding.songList.itemAnimator?.changeDuration ?: animDuration

        binding.currentSongDisplay.root.setBackgroundResource(R.color.selected)
        binding.currentSongDisplay.root.setOnClickListener {
            isListFollowingCurrentSong = true
            binding.songList.stopScroll()
            updateScrollPosition()
        }
        binding.currentSongDisplay.options.visibility = View.GONE
        binding.currentSongDisplay.optionsContainer.visibility = View.GONE

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            binding.currentSongDisplay.root.elevation = resources.getDimension(R.dimen.smallDimen)
            binding.loadingText.elevation = resources.getDimension(R.dimen.mediumDimen)
        }
        requireActivity().bindService(Intent(requireActivity(), PlayerService::class.java), vm.connection, Context.BIND_AUTO_CREATE)
        vm.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(observable: Observable, id: Int) {
                when (id) {
                    BR.pagedAudioQueue -> {
                        if (vm.pagedAudioQueue != null) {
                            (binding.songList.adapter as SongAdapter).submitList(vm.pagedAudioQueue)
                            if (vm.listPositionLoaded) {
                                updateLoadingVisibility(false)
                            }
                        } else {
                            updateLoadingVisibility(true)
                        }
                    }
                    BR.listPosition -> {
                        (binding.songList.adapter as SongAdapter).setSelectedPosition(vm.listPosition)
                        if (vm.listPositionLoaded) {
                            updateCurrentSongDisplay()
                            updateScrollPosition()
                        }
                    }
                    BR.listPositionLoaded -> {
                        if (vm.pagedAudioQueue != null && vm.listPositionLoaded && shouldScroll) {
                            updateLoadingVisibility(false)
                            if (isFirstScroll) {
                                isFirstScroll = false
                                linearLayoutManager.scrollToPositionWithOffset(vm.listPosition, 0)
                            } else {
                                val listener = object : RecyclerView.OnChildAttachStateChangeListener {

                                    override fun onChildViewDetachedFromWindow(view: View) {
                                    }

                                    override fun onChildViewAttachedToWindow(view: View) {
                                        binding.songList.removeOnChildAttachStateChangeListener(this)
                                        shouldHideLoading = true
                                        linearLayoutManager.scrollToPositionWithOffset(vm.listPosition, 0)
                                    }
                                }
                                binding.songList.addOnChildAttachStateChangeListener(listener)
                                mainThreadHandler.postDelayed({
                                    binding.songList.removeOnChildAttachStateChangeListener(listener)
                                    shouldHideLoading = true
                                    linearLayoutManager.scrollToPositionWithOffset(vm.listPosition, 0)
                                }, 500)
                                shouldScroll = false
                            }
                        }
                    }
                }
            }
        })
        shouldHideLoading = true
        return binding.root
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == DownloadHelper.REQUEST_WRITING && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            songWaitingToBeDownloaded?.let { vm.askForDownload(it) }
        }
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
            binding.songList.stopScroll()

            if (vm.listPositionLoaded && vm.pagedAudioQueue != null) {
                shouldHideLoading = true
                mainThreadHandler.postDelayed({ linearLayoutManager.scrollToPositionWithOffset(vm.listPosition, 0) }, 100)
            } else if (vm.listPosition in 0 until (vm.pagedAudioQueue?.size ?: 0)) {
                vm.pagedAudioQueue?.loadAround(vm.listPosition)
                shouldScroll = true
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
            holder.isCurrentSong = position == vm.listPosition
            val song = getItem(position)
            if (song != null) {
                holder.bind(song)
            }
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
            binding.root.setOnClickListener { vm.play(adapterPosition) }
            binding.options.setOnCheckedChangeListener { _, isChecked ->

                val constraintSet = ConstraintSet()
                constraintSet.clone(requireContext(), R.layout.item_song)

                val connectionToDelete = if (isChecked) ConstraintSet.TOP else ConstraintSet.BOTTOM
                val connectionToAdd = if (isChecked) ConstraintSet.BOTTOM else ConstraintSet.TOP
                constraintSet.clear(R.id.optionsContainer, connectionToDelete)
                constraintSet.connect(R.id.optionsContainer, connectionToAdd, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)

                val transition = ChangeBounds()
                transition.interpolator = LinearInterpolator()
                transition.duration = animDuration

                TransitionManager.beginDelayedTransition(binding.rootContainer, transition)
                constraintSet.applyTo(binding.rootContainer)
            }
            binding.download.setOnClickListener {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    songWaitingToBeDownloaded = binding.song
                    if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        AlertDialog.Builder(requireContext())
                                .setMessage(R.string.write_permission_explanation)
                                .setNegativeButton(R.string.refuse) { dialog, _ ->
                                    dialog.dismiss()
                                }
                                .setPositiveButton(R.string.accord) { _, _ ->
                                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), DownloadHelper.REQUEST_WRITING)
                                }
                                .show()

                    } else {
                        ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), DownloadHelper.REQUEST_WRITING)
                    }
                } else {
                    binding.song?.let { vm.askForDownload(it) }
                    binding.download.isEnabled = false
                }
            }
        }

        fun bind(song: SongDisplay) {
            binding.song = song
            binding.options.isChecked = false
            binding.download.isEnabled = !vm.fileExist(song)
        }

        var isCurrentSong: Boolean = false
            set(value) {
                field = value
                val backgroundColor = if (field) R.color.selected else R.color.unselected
                val color = ResourcesCompat.getColor(resources, backgroundColor, null)
                binding.rootContainer.setBackgroundColor(color)
                binding.options.setBackgroundColor(color)
                binding.optionsContainer.setBackgroundColor(color)
            }
    }
}