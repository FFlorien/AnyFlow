package be.florien.anyflow.feature.player.filter.selection

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
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
import be.florien.anyflow.databinding.ItemSelectFilterGridBinding
import be.florien.anyflow.databinding.ItemSelectFilterListBinding
import be.florien.anyflow.extension.viewModelFactory
import be.florien.anyflow.feature.BaseSelectableAdapter
import be.florien.anyflow.feature.menu.implementation.SearchMenuHolder
import be.florien.anyflow.feature.player.filter.BaseFilterFragment
import be.florien.anyflow.feature.player.filter.BaseFilterViewModel
import be.florien.anyflow.feature.player.filter.selectType.SelectFilterTypeViewModel.Companion.ALBUM_ID
import be.florien.anyflow.feature.player.filter.selectType.SelectFilterTypeViewModel.Companion.ARTIST_ID
import be.florien.anyflow.feature.player.filter.selectType.SelectFilterTypeViewModel.Companion.DOWNLOAD_ID
import be.florien.anyflow.feature.player.filter.selectType.SelectFilterTypeViewModel.Companion.GENRE_ID
import be.florien.anyflow.feature.player.filter.selectType.SelectFilterTypeViewModel.Companion.PLAYLIST_ID
import be.florien.anyflow.feature.player.filter.selectType.SelectFilterTypeViewModel.Companion.SONG_ID
import be.florien.anyflow.injection.ActivityScope
import be.florien.anyflow.injection.UserScope
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView

@ActivityScope
@UserScope
class SelectFilterFragment @SuppressLint("ValidFragment")
constructor(private var filterType: String = GENRE_ID) : BaseFilterFragment() {

    companion object {
        private const val FILTER_TYPE = "TYPE"
    }

    override val baseViewModel: BaseFilterViewModel
        get() = viewModel
    lateinit var viewModel: SelectFilterViewModel
    private lateinit var fragmentBinding: FragmentSelectFilterBinding
    private val searchMenuHolder by lazy {
        SearchMenuHolder(viewModel.isSearching.value == true, requireContext()) {
            val currentValue = viewModel.isSearching.value ?: false
            viewModel.isSearching.value = !currentValue
        }
    }

    override fun getTitle(): String = when (filterType) {
        ALBUM_ID -> getString(R.string.filter_title_album)
        ARTIST_ID -> getString(R.string.filter_title_album_artist)
        GENRE_ID -> getString(R.string.filter_title_genre)
        SONG_ID -> getString(R.string.filter_title_song)
        PLAYLIST_ID -> getString(R.string.filter_title_playlist)
        DOWNLOAD_ID -> getString(R.string.filter_title_downloaded)
        else -> getString(R.string.filter_title_main)
    }

    init {
        arguments?.let {
            filterType = it.getString(FILTER_TYPE, GENRE_ID)
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
        searchMenuHolder.isVisible = viewModel.hasSearch
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = ViewModelProvider(this, requireActivity().viewModelFactory)[
                when (filterType) {
                    ALBUM_ID -> SelectFilterAlbumViewModel::class.java
                    ARTIST_ID -> SelectFilterArtistViewModel::class.java
                    GENRE_ID -> SelectFilterGenreViewModel::class.java
                    SONG_ID -> SelectFilterSongViewModel::class.java
                    DOWNLOAD_ID -> SelectFilterDownloadedViewModel::class.java
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
            if (viewModel.itemDisplayType == SelectFilterViewModel.ITEM_LIST) {
                LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
            } else {
                GridLayoutManager(activity, 3)
            }

        fragmentBinding.filterList.adapter = FilterListAdapter(
            viewModel.itemDisplayType,
            viewModel,
            viewModel::hasFilter,
            viewModel::toggleFilterSelection
        )
        ResourcesCompat.getDrawable(resources, R.drawable.sh_divider, requireActivity().theme)
            ?.let {
                fragmentBinding.filterList.addItemDecoration(
                    DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL)
                        .apply { setDrawable(it) })
                if (viewModel.itemDisplayType == SelectFilterViewModel.ITEM_GRID) {
                    fragmentBinding.filterList.addItemDecoration(
                        DividerItemDecoration(requireActivity(), DividerItemDecoration.HORIZONTAL)
                            .apply { setDrawable(it) })
                }
            }
        viewModel.values.observe(viewLifecycleOwner) {
            (fragmentBinding.filterList.adapter as FilterListAdapter).submitData(lifecycle, it)
        }
        return fragmentBinding.root
    }

    class FilterListAdapter(
        private val itemDisplayType: Int,
        private val viewModel: SelectFilterViewModel,
        override val isSelected: (SelectFilterViewModel.FilterItem) -> Boolean,
        override val setSelected: (SelectFilterViewModel.FilterItem) -> Unit
    ) :
        PagingDataAdapter<SelectFilterViewModel.FilterItem, FilterViewHolder>(object :
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
            if (itemDisplayType == SelectFilterViewModel.ITEM_LIST) {
                FilterListViewHolder(parent, viewModel, setSelected)
            } else {
                FilterGridViewHolder(parent, viewModel, setSelected)
            }

        override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
            val filter = getItem(position) ?: return
            val isSelected = isSelected(filter)
            holder.bind(filter, isSelected)
        }

        override fun getSectionName(position: Int): String =
            snapshot()[position]?.displayName?.firstOrNull()?.uppercaseChar()?.toString() ?: ""
    }

    abstract class FilterViewHolder(view: View) : RecyclerView.ViewHolder(view),
        BaseSelectableAdapter.BaseSelectableViewHolder<SelectFilterViewModel.FilterItem, SelectFilterViewModel.FilterItem>

    class FilterListViewHolder(
        parent: ViewGroup,
        viewModel: SelectFilterViewModel,
        override val onSelectChange: (SelectFilterViewModel.FilterItem) -> Unit,
        private val itemFilterTypeBinding: ItemSelectFilterListBinding
        = ItemSelectFilterListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    ) : FilterViewHolder(itemFilterTypeBinding.root) {

        init {
            itemFilterTypeBinding.vm = viewModel
            itemFilterTypeBinding.lifecycleOwner = parent.findViewTreeLifecycleOwner()
        }

        override fun bind(item: SelectFilterViewModel.FilterItem, isSelected: Boolean) {
            itemFilterTypeBinding.item = item
            setSelection(isSelected)
            itemFilterTypeBinding.root.setOnClickListener {
                onSelectChange(item)
            }
        }

        override fun setSelection(isSelected: Boolean) {
            itemFilterTypeBinding.selected = isSelected
        }

        override fun getCurrentId(): SelectFilterViewModel.FilterItem? = itemFilterTypeBinding.item
    }

    class FilterGridViewHolder(
        parent: ViewGroup,
        viewModel: SelectFilterViewModel,
        override val onSelectChange: (SelectFilterViewModel.FilterItem) -> Unit,
        private val itemFilterTypeBinding: ItemSelectFilterGridBinding
        = ItemSelectFilterGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    ) : FilterViewHolder(itemFilterTypeBinding.root) {

        init {
            itemFilterTypeBinding.vm = viewModel
        }

        override fun bind(item: SelectFilterViewModel.FilterItem, isSelected: Boolean) {
            itemFilterTypeBinding.lifecycleOwner = itemFilterTypeBinding.lifecycleOwner
            itemFilterTypeBinding.item = item
            setSelection(isSelected)
            itemFilterTypeBinding.root.setOnClickListener {
                onSelectChange(item)
            }
        }

        override fun setSelection(isSelected: Boolean) {
            itemFilterTypeBinding.selected = isSelected
        }

        override fun getCurrentId(): SelectFilterViewModel.FilterItem? = itemFilterTypeBinding.item
    }
}
