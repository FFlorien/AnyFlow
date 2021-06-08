package be.florien.anyflow.feature.player.filter.selection

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.*
import be.florien.anyflow.R
import be.florien.anyflow.databinding.FragmentSelectFilterBinding
import be.florien.anyflow.databinding.ItemSelectFilterGridBinding
import be.florien.anyflow.databinding.ItemSelectFilterListBinding
import be.florien.anyflow.extension.viewModelFactory
import be.florien.anyflow.feature.BaseFragment
import be.florien.anyflow.feature.player.filter.selectType.SelectFilterTypeViewModel.Companion.ALBUM_ID
import be.florien.anyflow.feature.player.filter.selectType.SelectFilterTypeViewModel.Companion.ARTIST_ID
import be.florien.anyflow.feature.player.filter.selectType.SelectFilterTypeViewModel.Companion.GENRE_ID
import be.florien.anyflow.injection.ActivityScope
import be.florien.anyflow.injection.UserScope
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@ActivityScope
@UserScope
class SelectFilterFragment @SuppressLint("ValidFragment")
constructor(private var filterType: String) : BaseFragment() {
    override fun getTitle(): String = when (filterType) {
        ALBUM_ID -> getString(R.string.filter_title_album)
        ARTIST_ID -> getString(R.string.filter_title_album_artist)
        GENRE_ID -> getString(R.string.filter_title_genre)
        else -> getString(R.string.filter_title_main)
    }

    companion object {
        private const val FILTER_TYPE = "TYPE"
    }

    lateinit var viewModel: SelectFilterViewModel
    private lateinit var fragmentBinding: FragmentSelectFilterBinding

    constructor() : this(GENRE_ID)

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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = ViewModelProvider(this, requireActivity().viewModelFactory).get(
                when (filterType) {
                    ALBUM_ID -> SelectFilterAlbumViewModel::class.java
                    ARTIST_ID -> SelectFilterArtistViewModel::class.java
                    else -> SelectFilterGenreViewModel::class.java
                })
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
            (fragmentBinding.filterList.adapter as FilterListAdapter).submitData(viewLifecycleOwner.lifecycle, it)
        }
        fragmentBinding.search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                lifecycleScope.launch {
                    delay(200)
                    viewModel.values.observe(viewLifecycleOwner) {
                        (fragmentBinding.filterList.adapter as FilterListAdapter).submitData(viewLifecycleOwner.lifecycle, it)
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                viewModel.values.removeObservers(this@SelectFilterFragment)
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

        })
        return fragmentBinding.root
    }

    inner class FilterListAdapter : PagingDataAdapter<SelectFilterViewModel.FilterItem, FilterViewHolder>(object : DiffUtil.ItemCallback<SelectFilterViewModel.FilterItem>() {
        override fun areItemsTheSame(oldItem: SelectFilterViewModel.FilterItem, newItem: SelectFilterViewModel.FilterItem) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: SelectFilterViewModel.FilterItem, newItem: SelectFilterViewModel.FilterItem): Boolean = oldItem.isSelected == newItem.isSelected && oldItem.artUrl == newItem.artUrl && oldItem.displayName == newItem.displayName

    }), FastScrollRecyclerView.SectionedAdapter {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder = if (viewModel.itemDisplayType == SelectFilterViewModel.ITEM_LIST) FilterListViewHolder() else FilterGridViewHolder()

        override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        override fun getSectionName(position: Int): String =
            snapshot()[position]?.displayName?.firstOrNull()?.uppercaseChar()?.toString()
                ?: ""
    }

    abstract inner class FilterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        abstract fun bind(filter: SelectFilterViewModel.FilterItem?)

        protected fun setBackground(view: View, filter: SelectFilterViewModel.FilterItem?) {
            view.setBackgroundColor(ResourcesCompat.getColor(resources, if (filter?.isSelected == true) R.color.selected else R.color.unselected, requireActivity().theme))
        }
    }

    inner class FilterListViewHolder(private val itemFilterTypeBinding: ItemSelectFilterListBinding
                                     = ItemSelectFilterListBinding.inflate(layoutInflater, fragmentBinding.filterList, false))
        : FilterViewHolder(itemFilterTypeBinding.root) {

        override fun bind(filter: SelectFilterViewModel.FilterItem?) {
            itemFilterTypeBinding.vm = viewModel
            itemFilterTypeBinding.lifecycleOwner = viewLifecycleOwner
            itemFilterTypeBinding.item = filter
            setBackground(itemFilterTypeBinding.root, filter)
            filter?.let {
                itemFilterTypeBinding.root.setOnClickListener {
                    viewModel.changeFilterSelection(filter)
                    setBackground(itemFilterTypeBinding.root, filter)
                }
            }
        }
    }

    inner class FilterGridViewHolder(private val itemFilterTypeBinding: ItemSelectFilterGridBinding
                                     = ItemSelectFilterGridBinding.inflate(layoutInflater, fragmentBinding.filterList, false))
        : FilterViewHolder(itemFilterTypeBinding.root) {

        override fun bind(filter: SelectFilterViewModel.FilterItem?) {
            itemFilterTypeBinding.vm = viewModel
            itemFilterTypeBinding.lifecycleOwner = viewLifecycleOwner
            itemFilterTypeBinding.item = filter
            setBackground(itemFilterTypeBinding.root, filter)
            filter?.let {
                itemFilterTypeBinding.root.setOnClickListener {
                    viewModel.changeFilterSelection(filter)
                    setBackground(itemFilterTypeBinding.root, filter)
                }
            }
        }
    }
}
