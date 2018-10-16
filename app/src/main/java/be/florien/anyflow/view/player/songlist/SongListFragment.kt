package be.florien.anyflow.view.player.songlist

import android.arch.paging.PagedListAdapter
import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.databinding.Observable
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.graphics.drawable.Animatable2Compat
import android.support.graphics.drawable.AnimatedVectorDrawableCompat
import android.support.v4.app.Fragment
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.util.DiffUtil
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import be.florien.anyflow.BR
import be.florien.anyflow.R
import be.florien.anyflow.databinding.FragmentSongListBinding
import be.florien.anyflow.databinding.ItemSongBinding
import be.florien.anyflow.di.ActivityScope
import be.florien.anyflow.persistence.local.model.SongDisplay
import be.florien.anyflow.player.PlayerService
import be.florien.anyflow.view.player.PlayerActivity
import javax.inject.Inject

/**
 * Display a list of accounts and play it upon selection.
 */
@ActivityScope
class SongListFragment : Fragment() {

    @Inject
    lateinit var vm: SongListFragmentVm

    private lateinit var linearLayoutManager: LinearLayoutManager
    private var binding: FragmentSongListBinding? = null
    private var isListFollowingCurrentSong = true
    private var isUserScroll = false
    private var orderingMenu: MenuItem? = null
    var isOrderIconRandom = true

    private val drawableOrdered: AnimatedVectorDrawableCompat
        get() = AnimatedVectorDrawableCompat.create(requireActivity(), R.drawable.ic_order_ordered)?.apply {
            registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) {
                    super.onAnimationEnd(drawable)
                    orderingMenu?.icon = drawableRandom
                    isOrderIconRandom = true
                }
            })
        } ?: throw IllegalStateException("Error parsing the vector drawable")

    private val drawableRandom: AnimatedVectorDrawableCompat
        get() = AnimatedVectorDrawableCompat.create(requireActivity(), R.drawable.ic_order_random)?.apply {
            registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) {
                    super.onAnimationEnd(drawable)
                    orderingMenu?.icon = drawableOrdered
                    isOrderIconRandom = false
                }
            })
        } ?: throw IllegalStateException("Error parsing the vector drawable")


    private val topSet by lazy {
        ConstraintSet().apply {
            clone(binding?.root as ConstraintLayout)
            clear(R.id.currentSongDisplay, ConstraintSet.BOTTOM)
            connect(R.id.currentSongDisplay, ConstraintSet.TOP, R.id.songList, ConstraintSet.TOP)
        }
    }
    private val bottomSet by lazy {
        ConstraintSet().apply {
            clone(binding?.root as ConstraintLayout)
            clear(R.id.currentSongDisplay, ConstraintSet.TOP)
            connect(R.id.currentSongDisplay, ConstraintSet.BOTTOM, R.id.songList, ConstraintSet.BOTTOM)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as PlayerActivity).activityComponent.inject(this)
        setHasOptionsMenu(true)
        val intent = Intent(requireContext(), be.florien.anyflow.persistence.UpdateService::class.java)
        intent.data = Uri.parse(be.florien.anyflow.persistence.UpdateService.UPDATE_ALL)
        requireActivity().startService(intent)

        vm.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                when (propertyId) {
                    BR.isOrderRandom -> {
                        changeOrderMenu()
                    }
                }
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_song_list, container, false)
        binding = DataBindingUtil.bind(view)
        binding?.vm = vm
        vm.refreshSongs() //todo communication between service and VM
        binding?.songList?.adapter = SongAdapter().apply {
            submitList(vm.pagedAudioQueue)
        }
        linearLayoutManager = LinearLayoutManager(activity)
        binding?.songList?.layoutManager = linearLayoutManager
        binding?.songList?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateCurrentSongDisplay(true)
            }
        })
        binding?.songList?.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView?, e: MotionEvent?): Boolean {
                isUserScroll = true
                return false
            }
        })
        binding?.currentSongDisplay?.root?.setBackgroundResource(R.color.selected)
        binding?.currentSongDisplay?.root?.setOnClickListener {
            isListFollowingCurrentSong = true
            binding?.songList?.stopScroll()
            linearLayoutManager.scrollToPositionWithOffset(vm.listPosition, 0)
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            binding?.currentSongDisplay?.root?.elevation = resources.getDimension(R.dimen.small_dimen)
        }
        requireActivity().bindService(Intent(requireActivity(), PlayerService::class.java), vm.connection, Context.BIND_AUTO_CREATE)
        vm.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(observable: Observable, id: Int) {
                when (id) {
                    BR.pagedAudioQueue -> (binding?.songList?.adapter as SongAdapter).submitList(vm.pagedAudioQueue)
                    BR.listPosition -> {
                        val songAdapter = binding?.songList?.adapter as? SongAdapter
                        songAdapter?.setSelectedPosition(vm.listPosition)
                        updateCurrentSongDisplay(false)
                    }
                }
            }
        })
        updateCurrentSongDisplay(false)
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_song_list, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        orderingMenu = menu.findItem(R.id.order)
        changeOrderMenu()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.order -> {
                if (vm.isOrderRandom) {
                    vm.classicOrder()
                } else {
                    vm.randomOrder()
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        vm.destroy()
        requireActivity().unbindService(vm.connection)
    }

    private fun updateCurrentSongDisplay(isFromScrollListener: Boolean) {
        val firstVisibleItemPosition = linearLayoutManager.findFirstCompletelyVisibleItemPosition()
        val lastVisibleItemPosition = linearLayoutManager.findLastCompletelyVisibleItemPosition()
        if (vm.listPosition in firstVisibleItemPosition..lastVisibleItemPosition) {
            binding?.currentSongDisplay?.root?.visibility = View.GONE
        } else {
            if (isUserScroll) {
                isListFollowingCurrentSong = false
            }

            if (binding?.currentSongDisplay?.root?.visibility != View.VISIBLE) {
                binding?.currentSongDisplay?.root?.visibility = View.VISIBLE

                if (vm.listPosition < firstVisibleItemPosition) {
                    topSet.applyTo(binding?.root as ConstraintLayout?)
                } else if (vm.listPosition > lastVisibleItemPosition) {
                    bottomSet.applyTo(binding?.root as ConstraintLayout?)
                }
            }
        }

        if (isListFollowingCurrentSong and !isFromScrollListener) {
            linearLayoutManager.scrollToPositionWithOffset(vm.listPosition, 0)
        }

        isUserScroll = false
    }

    private fun changeOrderMenu() {
        orderingMenu?.run {
            setTitle(if (vm.isOrderRandom) {
                R.string.menu_order_classic
            } else {
                R.string.menu_order_random
            })
            if (icon == null) {
                icon = if (vm.isOrderRandom) drawableRandom else drawableOrdered
                isOrderIconRandom = vm.isOrderRandom
            } else if (vm.isOrderRandom != isOrderIconRandom) {
                (icon as? Animatable)?.start()
            }
        }
    }


    inner class SongAdapter : PagedListAdapter<SongDisplay, SongViewHolder>(object : DiffUtil.ItemCallback<SongDisplay>() {
        override fun areItemsTheSame(oldItem: SongDisplay, newItem: SongDisplay) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: SongDisplay, newItem: SongDisplay): Boolean = oldItem.artistName == newItem.artistName && oldItem.albumName == newItem.albumName && oldItem.title == newItem.title

    }) {

        init {
            registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                    if (lastPosition in fromPosition..(fromPosition + itemCount)) {
                        setSelectedPosition(lastPosition + (fromPosition - toPosition))
                    }
                }
            })
        }

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