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
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.*
import be.florien.anyflow.R
import be.florien.anyflow.databinding.FragmentSelectFilterBinding
import be.florien.anyflow.databinding.ItemSelectFilterGridBinding
import be.florien.anyflow.databinding.ItemSelectFilterListBinding
import be.florien.anyflow.extension.viewModelFactory
import be.florien.anyflow.feature.menu.SearchMenuHolder
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

    override fun getTitle(): String = when (filterType) {
        ALBUM_ID -> getString(R.string.filter_title_album)
        ARTIST_ID -> getString(R.string.filter_title_album_artist)
        GENRE_ID -> getString(R.string.filter_title_genre)
        SONG_ID -> getString(R.string.filter_title_song)
        PLAYLIST_ID -> getString(R.string.filter_title_song)
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
        val menuHolder = SearchMenuHolder {
            val currentValue = viewModel.isSearching.value ?: false
            viewModel.isSearching.value = !currentValue
        }
        menuCoordinator.addMenuHolder(menuHolder)
        viewModel.isSearching.observe(this) {
            val imm: InputMethodManager? = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            if (it) {
                Handler(Looper.getMainLooper()).postDelayed({
                    fragmentBinding.search.requestFocus()
                    imm?.showSoftInput(fragmentBinding.search, InputMethodManager.SHOW_IMPLICIT)
                }, 200)
            } else {
                imm?.hideSoftInputFromWindow(fragmentBinding.root.windowToken, 0)
            }
        }
        menuHolder.isVisible = viewModel.hasSearch
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = ViewModelProvider(this, requireActivity().viewModelFactory).get(
                when (filterType) {
                    ALBUM_ID -> SelectFilterAlbumViewModel::class.java
                    ARTIST_ID -> SelectFilterArtistViewModel::class.java
                    GENRE_ID -> SelectFilterGenreViewModel::class.java
                    SONG_ID -> SelectFilterSongViewModel::class.java
                    DOWNLOAD_ID -> SelectFilterDownloadedViewModel::class.java
                    else -> SelectFilterPlaylistViewModel::class.java
                }
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        fragmentBinding = FragmentSelectFilterBinding.inflate(inflater, container, false)
        fragmentBinding.lifecycleOwner = viewLifecycleOwner
        fragmentBinding.viewModel = viewModel
        fragmentBinding.filterList.layoutManager = if (viewModel.itemDisplayType == SelectFilterViewModel.ITEM_LIST) {
            LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        } else {
            GridLayoutManager(activity, 3)
        }
        fragmentBinding.filterList.adapter = FilterListAdapter()
        ResourcesCompat.getDrawable(resources, R.drawable.sh_divider, requireActivity().theme)?.let {
            fragmentBinding.filterList.addItemDecoration(DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL).apply { setDrawable(it) })
            if (viewModel.itemDisplayType == SelectFilterViewModel.ITEM_GRID) {
                fragmentBinding.filterList.addItemDecoration(DividerItemDecoration(requireActivity(), DividerItemDecoration.HORIZONTAL).apply { setDrawable(it) })
            }
        }
        viewModel.values.observe(viewLifecycleOwner) {
            (fragmentBinding.filterList.adapter as FilterListAdapter).submitData(lifecycle, it)
        }
        return fragmentBinding.root
    }

    inner class FilterListAdapter : PagingDataAdapter<SelectFilterViewModel.FilterItem, FilterViewHolder>(object : DiffUtil.ItemCallback<SelectFilterViewModel.FilterItem>() {
        override fun areItemsTheSame(oldItem: SelectFilterViewModel.FilterItem, newItem: SelectFilterViewModel.FilterItem) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: SelectFilterViewModel.FilterItem, newItem: SelectFilterViewModel.FilterItem): Boolean =
                oldItem.artUrl == newItem.artUrl && oldItem.displayName == newItem.displayName && oldItem.isSelected == newItem.isSelected

    }), FastScrollRecyclerView.SectionedAdapter {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder =
            if (viewModel.itemDisplayType == SelectFilterViewModel.ITEM_LIST) FilterListViewHolder() else FilterGridViewHolder()

        override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
            val filter = getItem(position) ?: return
            holder.bind(filter)
        }

        override fun getSectionName(position: Int): String =
                snapshot()[position]?.displayName?.firstOrNull()?.uppercaseChar()?.toString()
                        ?: ""
    }

    abstract inner class FilterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        abstract fun bind(filter: SelectFilterViewModel.FilterItem)

        protected fun setBackground(view: View, filter: SelectFilterViewModel.FilterItem?) {
            view.setBackgroundColor(ResourcesCompat.getColor(resources, if (filter?.isSelected == true) R.color.selected else R.color.unselected, requireActivity().theme))
        }
    }

    inner class FilterListViewHolder(
            private val itemFilterTypeBinding: ItemSelectFilterListBinding
            = ItemSelectFilterListBinding.inflate(layoutInflater, fragmentBinding.filterList, false)
    ) : FilterViewHolder(itemFilterTypeBinding.root) {

        override fun bind(filter: SelectFilterViewModel.FilterItem) {
            itemFilterTypeBinding.vm = viewModel
            itemFilterTypeBinding.lifecycleOwner = viewLifecycleOwner
            itemFilterTypeBinding.item = filter
            setBackground(itemFilterTypeBinding.root, filter)
            itemFilterTypeBinding.root.setOnClickListener {
                viewModel.changeFilterSelection(filter)
                setBackground(itemFilterTypeBinding.root, filter)
            }
        }
    }

    inner class FilterGridViewHolder(
            private val itemFilterTypeBinding: ItemSelectFilterGridBinding
            = ItemSelectFilterGridBinding.inflate(layoutInflater, fragmentBinding.filterList, false)
    ) : FilterViewHolder(itemFilterTypeBinding.root) {

        override fun bind(filter: SelectFilterViewModel.FilterItem) {
            itemFilterTypeBinding.vm = viewModel
            itemFilterTypeBinding.lifecycleOwner = viewLifecycleOwner
            itemFilterTypeBinding.item = filter
            setBackground(itemFilterTypeBinding.root, filter)
            itemFilterTypeBinding.root.setOnClickListener {
                viewModel.changeFilterSelection(filter)
                setBackground(itemFilterTypeBinding.root, filter)
            }
        }
    }
}
