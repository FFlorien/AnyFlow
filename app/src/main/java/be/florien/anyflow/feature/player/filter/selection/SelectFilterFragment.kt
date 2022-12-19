package be.florien.anyflow.feature.player.filter.selection

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.*
import be.florien.anyflow.R
import be.florien.anyflow.databinding.FragmentSelectFilterBinding
import be.florien.anyflow.databinding.ItemSelectFilterListBinding
import be.florien.anyflow.extension.viewModelFactory
import be.florien.anyflow.feature.BaseSelectableAdapter
import be.florien.anyflow.feature.menu.implementation.SearchMenuHolder
import be.florien.anyflow.feature.menu.implementation.SelectAllMenuHolder
import be.florien.anyflow.feature.menu.implementation.SelectNoneMenuHolder
import be.florien.anyflow.feature.player.details.DetailViewHolder
import be.florien.anyflow.feature.player.details.DetailViewHolderListener
import be.florien.anyflow.feature.player.details.ItemInfoTouchAdapter
import be.florien.anyflow.feature.player.filter.BaseFilterFragment
import be.florien.anyflow.feature.player.filter.FilterActions
import be.florien.anyflow.feature.player.filter.selectType.SelectFilterTypeViewModel
import be.florien.anyflow.injection.ActivityScope
import be.florien.anyflow.injection.UserScope
import com.google.android.material.snackbar.Snackbar
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView

@ActivityScope
@UserScope
class SelectFilterFragment @SuppressLint("ValidFragment")
constructor(private var filterType: String = SelectFilterTypeViewModel.GENRE_ID) :
    BaseFilterFragment(),
    DetailViewHolderListener<SelectFilterViewModel.FilterItem> {

    companion object {
        private const val FILTER_TYPE = "TYPE"
    }

    override val filterActions: FilterActions
        get() = viewModel
    lateinit var viewModel: SelectFilterViewModel
    private lateinit var fragmentBinding: FragmentSelectFilterBinding
    private val searchMenuHolder by lazy {
        SearchMenuHolder(viewModel.isSearching.value == true, requireContext()) {
            val currentValue = viewModel.isSearching.value ?: false
            viewModel.isSearching.value = !currentValue
        }
    }
    private val selectAllMenuHolder by lazy {
        SelectAllMenuHolder {
            viewModel.selectAllInSelection()
        }
    }
    private val selectNoneMenuHolder by lazy {
        SelectNoneMenuHolder {
            viewModel.selectNoneInSelection()
        }
    }

    override fun getTitle(): String = when (filterType) {
        SelectFilterTypeViewModel.ALBUM_ID -> getString(R.string.filter_title_album)
        SelectFilterTypeViewModel.ARTIST_ID -> getString(R.string.filter_title_album_artist)
        SelectFilterTypeViewModel.GENRE_ID -> getString(R.string.filter_title_genre)
        SelectFilterTypeViewModel.SONG_ID -> getString(R.string.filter_title_song)
        SelectFilterTypeViewModel.PLAYLIST_ID -> getString(R.string.filter_title_playlist)
        SelectFilterTypeViewModel.DOWNLOAD_ID -> getString(R.string.filter_title_downloaded)
        else -> getString(R.string.filter_title_main)
    }

    init {
        arguments?.let {
            filterType = it.getString(FILTER_TYPE, SelectFilterTypeViewModel.GENRE_ID)
        }
        if (arguments == null) {
            arguments = Bundle().apply {
                putString(FILTER_TYPE, filterType)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        menuCoordinator.addMenuHolder(searchMenuHolder)
        menuCoordinator.addMenuHolder(selectAllMenuHolder)
        menuCoordinator.addMenuHolder(selectNoneMenuHolder)
        viewModel.isSearching.observe(this) {
            searchMenuHolder.changeState(!it)
            val imm: InputMethodManager? =
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            if (it) {
                Handler(Looper.getMainLooper()).postDelayed({
                    fragmentBinding.search.requestFocus()
                    imm?.showSoftInput(fragmentBinding.search, InputMethodManager.SHOW_IMPLICIT)
                }, 200)
            } else {
                imm?.hideSoftInputFromWindow(fragmentBinding.root.windowToken, 0)
            }
        }
        viewModel.hasFilterOfThisType.observe(this) { isAnythingSelected ->
            selectAllMenuHolder.isVisible = !isAnythingSelected
            selectNoneMenuHolder.isVisible = isAnythingSelected
        }
        viewModel.errorMessage.observe(this) {
            if (it > 0) {
                Snackbar.make(fragmentBinding.filterList, it, Snackbar.LENGTH_SHORT).show()
                viewModel.errorMessage.value = -1
            }
        }
        searchMenuHolder.isVisible = viewModel.hasSearch
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = ViewModelProvider(this, requireActivity().viewModelFactory)[
                when (filterType) {
                    SelectFilterTypeViewModel.ALBUM_ID -> SelectFilterAlbumViewModel::class.java
                    SelectFilterTypeViewModel.ARTIST_ID -> SelectFilterArtistViewModel::class.java
                    SelectFilterTypeViewModel.GENRE_ID -> SelectFilterGenreViewModel::class.java
                    SelectFilterTypeViewModel.SONG_ID -> SelectFilterSongViewModel::class.java
                    SelectFilterTypeViewModel.DOWNLOAD_ID -> SelectFilterDownloadedViewModel::class.java
                    else -> SelectFilterPlaylistViewModel::class.java
                }]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentBinding = FragmentSelectFilterBinding.inflate(inflater, container, false)
        fragmentBinding.lifecycleOwner = viewLifecycleOwner
        fragmentBinding.viewModel = viewModel
        fragmentBinding.filterList.layoutManager =
            LinearLayoutManager(activity, RecyclerView.VERTICAL, false)

        fragmentBinding.filterList.adapter = FilterListAdapter(
            viewModel::hasFilter,
            viewModel::toggleFilterSelection,
            this
        )
        ResourcesCompat.getDrawable(resources, R.drawable.sh_divider, requireActivity().theme)
            ?.let {
                fragmentBinding.filterList.addItemDecoration(
                    DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL)
                        .apply { setDrawable(it) })
            }
        viewModel.values.observe(viewLifecycleOwner) {
            (fragmentBinding.filterList.adapter as FilterListAdapter).submitData(lifecycle, it)
            fragmentBinding.filterList.addOnItemTouchListener(object : ItemInfoTouchAdapter(),
                RecyclerView.OnItemTouchListener {
                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    return onInterceptTouchEvent(e)
                }

                override fun onTouchEvent(rv: RecyclerView, event: MotionEvent) {
                    val childView = rv.findChildViewUnder(downTouchX, downTouchY) ?: return
                    val viewHolder =
                        (rv.findContainingViewHolder(childView) as? FilterViewHolder) ?: return
                    onTouch(viewHolder, event)
                }

                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                }
            })
        }
        return fragmentBinding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        menuCoordinator.removeMenuHolder(searchMenuHolder)
        menuCoordinator.removeMenuHolder(selectAllMenuHolder)
        menuCoordinator.removeMenuHolder(selectNoneMenuHolder)
    }

    override fun onInfoDisplayAsked(item: SelectFilterViewModel.FilterItem) {
        //TODO("Not yet implemented") SongInfoFragment(item).show(childFragmentManager, "info")
    }

    class FilterListAdapter(
        override val isSelected: (SelectFilterViewModel.FilterItem) -> Boolean,
        override val setSelected: (SelectFilterViewModel.FilterItem) -> Unit,
        private val detailListener: DetailViewHolderListener<SelectFilterViewModel.FilterItem>
    ) : PagingDataAdapter<SelectFilterViewModel.FilterItem, FilterViewHolder>(object :
        DiffUtil.ItemCallback<SelectFilterViewModel.FilterItem>() {
        override fun areItemsTheSame(
            oldItem: SelectFilterViewModel.FilterItem,
            newItem: SelectFilterViewModel.FilterItem
        ) = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: SelectFilterViewModel.FilterItem,
            newItem: SelectFilterViewModel.FilterItem
        ): Boolean =
            oldItem.artUrl == newItem.artUrl && oldItem.displayName == newItem.displayName && oldItem.isSelected == newItem.isSelected
    }), FastScrollRecyclerView.SectionedAdapter,
        BaseSelectableAdapter<SelectFilterViewModel.FilterItem> {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder =
            FilterViewHolder(parent, detailListener, setSelected)

        override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
            val filter = getItem(position) ?: return
            val isSelected = isSelected(filter)
            holder.bind(filter, isSelected)
        }

        override fun getSectionName(position: Int): String =
            snapshot()[position]?.displayName?.firstOrNull()?.uppercaseChar()?.toString() ?: ""
    }

    class FilterViewHolder(
        parent: ViewGroup,
        detailListener: DetailViewHolderListener<SelectFilterViewModel.FilterItem>,
        override val onSelectChange: (SelectFilterViewModel.FilterItem) -> Unit,
        private val binding: ItemSelectFilterListBinding = ItemSelectFilterListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    ) : DetailViewHolder<SelectFilterViewModel.FilterItem>(
        detailListener,
        binding.root
    ),
        BaseSelectableAdapter.BaseSelectableViewHolder<SelectFilterViewModel.FilterItem, SelectFilterViewModel.FilterItem> {

        override val itemInfoView: View
            get() = binding.info
        override val infoIconView: View
            get() = binding.infoView
        override val item: SelectFilterViewModel.FilterItem?
            get() = binding.item

        init {
            binding.lifecycleOwner = parent.findViewTreeLifecycleOwner()
            setClickListener()
        }

        override fun bind(item: SelectFilterViewModel.FilterItem, isSelected: Boolean) {
            binding.item = item
            setSelection(isSelected)
            itemInfoView.setOnClickListener {
                onSelectChange(item)
            }
        }

        override fun setSelection(isSelected: Boolean) {
            binding.selected = isSelected
        }

        override fun getCurrentId(): SelectFilterViewModel.FilterItem? = binding.item
    }
}
