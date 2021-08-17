package be.florien.anyflow.feature.player.songlist

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.R
import be.florien.anyflow.data.view.Song
import be.florien.anyflow.databinding.FragmentSongListBinding
import be.florien.anyflow.databinding.ItemSongBinding
import be.florien.anyflow.feature.BaseFragment
import be.florien.anyflow.feature.menu.SearchSongMenuHolder
import be.florien.anyflow.feature.player.PlayerActivity
import be.florien.anyflow.feature.player.songlist.RecyclerTouchListener.OnRowClickListener
import be.florien.anyflow.feature.player.songlist.RecyclerTouchListener.OnSwipeOptionsClickListener
import be.florien.anyflow.injection.ActivityScope
import be.florien.anyflow.player.PlayerService
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView


/**
 * Display a list of songs and play it upon selection.
 */
@ActivityScope
class SongListFragment : BaseFragment() {
    override fun getTitle(): String = getString(R.string.player_playing_now)

    lateinit var viewModel: SongListViewModel

    private lateinit var binding: FragmentSongListBinding
    private lateinit var linearLayoutManager: LinearLayoutManager
    private var shouldScrollToCurrent = false
    private var shouldHideLoading = false
    private var isLoadingVisible = false

    private val searchMenuHolder = SearchSongMenuHolder {
        val currentState = viewModel.isSearching.value == true
        viewModel.isSearching.value = !currentState
    }

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
        (requireActivity() as PlayerActivity).menuCoordinator.addMenuHolder(searchMenuHolder)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewModel = ViewModelProvider(this, (requireActivity() as PlayerActivity).viewModelFactory).get(SongListViewModel::class.java)
        binding = FragmentSongListBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.songList.adapter = SongAdapter()

        linearLayoutManager = LinearLayoutManager(activity)
        binding.songList.layoutManager = linearLayoutManager
        binding.songList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateCurrentSongDisplay()
            }
        })
        /*val helper = object : SwipeHelper(binding.songList, view.context) {
            override fun instantiateUnderlayButton(viewHolder: RecyclerView.ViewHolder, underlayButtons: MutableList<UnderlayButton>) {
                underlayButtons.add(
                        UnderlayButton("Tutu", ResourcesCompat.getColor(resources, R.color.accent, null), object : UnderlayButtonClickListener {
                            override fun onClick(pos: Int) {
                                iLog("click at $pos")
                            }
                        })
                )
            }
        }*/

        val touchListener = RecyclerTouchListener(this, binding.songList)
        touchListener
                .setClickable(object : OnRowClickListener {
                    override fun onRowClicked(position: Int) {
                        Toast.makeText(requireContext(), "$position", Toast.LENGTH_SHORT).show()
                    }

                    override fun onIndependentViewClicked(independentViewID: Int, position: Int) {}
                })
                .setSwipeOptionViews(R.id.tutu, R.id.info)
                .setSwipeable(R.id.songInfo, R.id.songOptions, object : OnSwipeOptionsClickListener {
                    override fun onSwipeOptionClicked(viewID: Int, position: Int) {
                        when (viewID) {
                            R.id.tutu -> Toast.makeText(requireContext(), "Tutu for $position", Toast.LENGTH_SHORT).show()
                            R.id.info -> Toast.makeText(requireContext(), "Info for $position", Toast.LENGTH_SHORT).show()
                        }
                    }
                })
        binding.songList.addOnItemTouchListener(touchListener)

        binding.currentSongDisplay.songInfo.setBackgroundResource(R.color.selected)
        binding.currentSongDisplayTouch.setOnClickListener {
            scrollToCurrentSong()
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            binding.currentSongDisplay.root.elevation = resources.getDimension(R.dimen.smallDimen)
            binding.loadingText.elevation = resources.getDimension(R.dimen.mediumDimen)
        }
        requireActivity().bindService(Intent(requireActivity(), PlayerService::class.java), viewModel.connection, Context.BIND_AUTO_CREATE)
        viewModel.pagedAudioQueue.observe(viewLifecycleOwner) {
            if (it != null) {
                (binding.songList.adapter as SongAdapter).submitData(viewLifecycleOwner.lifecycle, it)
                shouldScrollToCurrent = true
            }
            updateLoadingVisibility(true)
        }
        viewModel.listPosition.observe(viewLifecycleOwner) {
            (binding.songList.adapter as SongAdapter).setSelectedPosition(it)
            updateLoadingVisibility(false)
            updateCurrentSongDisplay()
            if (shouldScrollToCurrent) {
                scrollToCurrentSong()
                shouldScrollToCurrent = false
            }
        }
        viewModel.isSearching.observe(viewLifecycleOwner) {
            val imm: InputMethodManager? = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
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
                linearLayoutManager.scrollToPositionWithOffset(viewModel.searchResults.value?.value?.get(it)?.toInt() ?: it, 0)
            }
            updateCurrentSongDisplay()
        }
        shouldHideLoading = true
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshSongs()
        searchMenuHolder.isVisible = true
    }

    override fun onPause() {
        super.onPause()
        searchMenuHolder.isVisible = false
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().unbindService(viewModel.connection)
        (requireActivity() as PlayerActivity).menuCoordinator.removeMenuHolder(searchMenuHolder)
    }

    private fun updateCurrentSongDisplay() {
        val firstVisibleItemPosition = linearLayoutManager.findFirstCompletelyVisibleItemPosition()
        val lastVisibleItemPosition = linearLayoutManager.findLastCompletelyVisibleItemPosition()

        if (viewModel.listPosition.value in firstVisibleItemPosition..lastVisibleItemPosition || (viewModel.searchProgression.value ?: -1) >= 0) {
            binding.currentSongDisplay.root.visibility = View.GONE
            binding.currentSongDisplayTouch.visibility = View.GONE
        } else {

            if (binding.currentSongDisplay.root.visibility != View.VISIBLE) {
                binding.currentSongDisplay.root.visibility = View.VISIBLE
                binding.currentSongDisplayTouch.visibility = View.VISIBLE
            }

            if (viewModel.listPosition.value ?: 0 < firstVisibleItemPosition) {
                topSet.applyTo(binding.root as ConstraintLayout?)
            } else if (viewModel.listPosition.value ?: 0 > lastVisibleItemPosition) {
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

                            override fun onAnimationStart(animation: Animator?) {
                                binding.loadingText.visibility = View.VISIBLE
                            }

                            override fun onAnimationRepeat(animation: Animator?) {}

                            override fun onAnimationEnd(animation: Animator?) {
                                binding.loadingText.visibility =
                                        if (shouldLoadingBeVisible) View.VISIBLE else View.GONE
                            }

                            override fun onAnimationCancel(animation: Animator?) {}
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

    inner class SongAdapter :
            PagingDataAdapter<Song, SongViewHolder>(object : DiffUtil.ItemCallback<Song>() {
                override fun areItemsTheSame(oldItem: Song, newItem: Song) = oldItem.id == newItem.id

                override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean =
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
                    .inflate(LayoutInflater.from(parent.context), parent, false)
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener { viewModel.play(absoluteAdapterPosition) }
        }

        fun bind(song: Song?) {
            binding.song = song
        }

        var isCurrentSong: Boolean = false
            set(value) {
                field = value
                val backgroundColor = if (field) R.color.selected else R.color.unselected
                binding.songInfo.setBackgroundColor(
                        ResourcesCompat.getColor(
                                resources,
                                backgroundColor,
                                null
                        )
                )
            }
    }
}