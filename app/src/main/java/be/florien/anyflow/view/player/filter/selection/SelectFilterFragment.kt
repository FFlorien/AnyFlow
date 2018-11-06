package be.florien.anyflow.view.player.filter.selection

import android.annotation.SuppressLint
import android.arch.paging.PagedListAdapter
import android.content.Context
import android.databinding.Observable
import android.os.Bundle
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.util.DiffUtil
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import be.florien.anyflow.BR
import be.florien.anyflow.R
import be.florien.anyflow.databinding.FragmentSelectFilterBinding
import be.florien.anyflow.databinding.ItemSelectFilterGridBinding
import be.florien.anyflow.databinding.ItemSelectFilterListBinding
import be.florien.anyflow.di.ActivityScope
import be.florien.anyflow.di.UserScope
import be.florien.anyflow.view.player.filter.BaseFilterFragment
import be.florien.anyflow.view.player.filter.BaseFilterVM
import be.florien.anyflow.view.player.filter.selectType.ALBUM_NAME
import be.florien.anyflow.view.player.filter.selectType.ARTIST_NAME
import be.florien.anyflow.view.player.filter.selectType.GENRE_NAME
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView

@ActivityScope
@UserScope
class SelectFilterFragment @SuppressLint("ValidFragment")
constructor(private var filterType: String) : BaseFilterFragment() {
    override fun getTitle(): String = when (filterType) {
        ALBUM_NAME -> "Select albums"
        ARTIST_NAME -> "Select artists"
        GENRE_NAME -> "Select genres"
        else -> "Select filters"
    }

    override val baseVm: BaseFilterVM
        get() = vm

    companion object {
        private const val FILTER_TYPE = "TYPE"
    }

    lateinit var vm: SelectFilterFragmentVM
    private lateinit var fragmentBinding: FragmentSelectFilterBinding

    constructor() : this(GENRE_NAME)

    init {
        arguments?.let {
            filterType = it.getString(FILTER_TYPE)
        }
        if (arguments == null) {
            arguments = Bundle().apply {
                putString(FILTER_TYPE, filterType)
            }
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        vm = when (filterType) {
            ALBUM_NAME -> SelectFilterFragmentAlbumVM(requireActivity())
            ARTIST_NAME -> SelectFilterFragmentArtistVM(requireActivity())
            else -> SelectFilterFragmentGenreVM(requireActivity())
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentBinding = FragmentSelectFilterBinding.inflate(inflater, container, false)
        fragmentBinding.vm = vm
        fragmentBinding.filterList.layoutManager = if (vm.itemDisplayType == SelectFilterFragmentVM.ITEM_LIST) {
            LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        } else {
            GridLayoutManager(activity, 3)
        }
        fragmentBinding.filterList.adapter = FilterListAdapter()
        ResourcesCompat.getDrawable(resources, R.drawable.sh_divider, requireActivity().theme)?.let {
            fragmentBinding.filterList.addItemDecoration(DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL).apply { setDrawable(it) })
            if (vm.itemDisplayType == SelectFilterFragmentVM.ITEM_GRID) {
                fragmentBinding.filterList.addItemDecoration(DividerItemDecoration(requireActivity(), DividerItemDecoration.HORIZONTAL).apply { setDrawable(it) })
            }
        }
        vm.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(observable: Observable, id: Int) {
                when (id) {
                    BR.values -> (fragmentBinding.filterList.adapter as FilterListAdapter).submitList(vm.values)
                }
            }
        })
        return fragmentBinding.root
    }

    inner class FilterListAdapter : PagedListAdapter<SelectFilterFragmentVM.FilterItem, FilterViewHolder>(object : DiffUtil.ItemCallback<SelectFilterFragmentVM.FilterItem>() {
        override fun areItemsTheSame(oldItem: SelectFilterFragmentVM.FilterItem, newItem: SelectFilterFragmentVM.FilterItem) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: SelectFilterFragmentVM.FilterItem, newItem: SelectFilterFragmentVM.FilterItem): Boolean = oldItem.isSelected == newItem.isSelected && oldItem.artUrl == newItem.artUrl && oldItem.displayName == newItem.displayName

    }), FastScrollRecyclerView.SectionedAdapter {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder = if (vm.itemDisplayType == SelectFilterFragmentVM.ITEM_LIST) FilterListViewHolder() else FilterGridViewHolder()

        override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        override fun getSectionName(position: Int): String = vm.values?.get(position)?.displayName?.first()?.toUpperCase()?.toString()
                ?: ""
    }

    abstract inner class FilterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        abstract fun bind(filter: SelectFilterFragmentVM.FilterItem?)

        protected fun setBackground(view: View, filter: SelectFilterFragmentVM.FilterItem?) {
            view.setBackgroundColor(ResourcesCompat.getColor(resources, if (filter?.isSelected == true) R.color.selected else R.color.unselected, requireActivity().theme))
        }
    }

    inner class FilterListViewHolder(private val itemFilterTypeBinding: ItemSelectFilterListBinding
                                     = ItemSelectFilterListBinding.inflate(layoutInflater, fragmentBinding.filterList, false))
        : FilterViewHolder(itemFilterTypeBinding.root) {

        override fun bind(filter: SelectFilterFragmentVM.FilterItem?) {
            itemFilterTypeBinding.vm = vm
            itemFilterTypeBinding.item = filter
            setBackground(itemFilterTypeBinding.root, filter)
            filter?.let {
                itemFilterTypeBinding.root.setOnClickListener {
                    vm.changeFilterSelection(filter)
                    setBackground(itemFilterTypeBinding.root, filter)
                }
            }
        }
    }

    inner class FilterGridViewHolder(private val itemFilterTypeBinding: ItemSelectFilterGridBinding
                                     = ItemSelectFilterGridBinding.inflate(layoutInflater, fragmentBinding.filterList, false))
        : FilterViewHolder(itemFilterTypeBinding.root) {

        override fun bind(filter: SelectFilterFragmentVM.FilterItem?) {
            itemFilterTypeBinding.vm = vm
            itemFilterTypeBinding.item = filter
            setBackground(itemFilterTypeBinding.root, filter)
            filter?.let {
                itemFilterTypeBinding.root.setOnClickListener {
                    vm.changeFilterSelection(filter)
                    setBackground(itemFilterTypeBinding.root, filter)
                }
            }
        }
    }
}
