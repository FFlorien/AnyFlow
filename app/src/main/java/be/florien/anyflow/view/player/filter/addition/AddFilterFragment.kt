package be.florien.anyflow.view.player.filter.addition

import android.annotation.SuppressLint
import android.content.Context
import android.databinding.Observable
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import be.florien.anyflow.BR
import be.florien.anyflow.databinding.FragmentAddFilterBinding
import be.florien.anyflow.databinding.ItemAddFilterBinding
import be.florien.anyflow.databinding.ItemAddFilterGridBinding
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
class AddFilterFragment @SuppressLint("ValidFragment")
constructor(private var filterType: String) : BaseFilterFragment() {
    override val baseVm: BaseFilterVM
        get() = vm

    companion object {
        private const val FILTER_TYPE = "TYPE"
    }

    lateinit var vm: AddFilterFragmentVM<*>
    private lateinit var fragmentBinding: FragmentAddFilterBinding

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
            ALBUM_NAME -> AddFilterFragmentAlbumVM(requireActivity())
            ARTIST_NAME -> AddFilterFragmentArtistVM(requireActivity())
            else -> AddFilterFragmentGenreVM(requireActivity())
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentBinding = FragmentAddFilterBinding.inflate(inflater, container, false)
        fragmentBinding.vm = vm
        fragmentBinding.filterList.layoutManager = if (vm.itemDisplayType == AddFilterFragmentVM.ITEM_LIST) {
            LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        } else {
            GridLayoutManager(activity, 3)
        }
        fragmentBinding.filterList.adapter = FilterListAdapter()
        vm.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(observable: Observable, id: Int) {
                when (id) {
                    BR.displayedValues -> fragmentBinding.filterList.adapter.notifyDataSetChanged()
                }
            }
        })
        return fragmentBinding.root
    }

    inner class FilterListAdapter : RecyclerView.Adapter<FilterViewHolder>(), FastScrollRecyclerView.SectionedAdapter {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder = if (vm.itemDisplayType == AddFilterFragmentVM.ITEM_LIST) FilterListViewHolder() else FilterGridViewHolder()

        override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
            holder.bind(vm.getDisplayedValues()[position])
        }

        override fun getItemCount(): Int = vm.getDisplayedValues().size

        override fun getSectionName(position: Int): String = vm.getDisplayedValues()[position].displayName.first().toUpperCase().toString()
    }

    abstract class FilterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        abstract fun bind(filter: AddFilterFragmentVM.FilterItem)
    }

    inner class FilterListViewHolder(private val itemFilterTypeBinding: ItemAddFilterBinding
                                     = ItemAddFilterBinding.inflate(layoutInflater, fragmentBinding.filterList, false))
        : FilterViewHolder(itemFilterTypeBinding.root) {

        override fun bind(filter: AddFilterFragmentVM.FilterItem) {
            itemFilterTypeBinding.vm = vm
            itemFilterTypeBinding.item = filter
        }
    }

    inner class FilterGridViewHolder(private val itemFilterTypeBinding: ItemAddFilterGridBinding
                                     = ItemAddFilterGridBinding.inflate(layoutInflater, fragmentBinding.filterList, false))
        : FilterViewHolder(itemFilterTypeBinding.root) {

        override fun bind(filter: AddFilterFragmentVM.FilterItem) {
            itemFilterTypeBinding.vm = vm
            itemFilterTypeBinding.item = filter
        }
    }
}
