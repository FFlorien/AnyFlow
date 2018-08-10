package be.florien.ampacheplayer.view.player.filter.addition

import android.annotation.SuppressLint
import android.content.Context
import android.databinding.Observable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import be.florien.ampacheplayer.BR
import be.florien.ampacheplayer.databinding.FragmentAddFilterBinding
import be.florien.ampacheplayer.databinding.ItemAddFilterBinding
import be.florien.ampacheplayer.di.ActivityScope
import be.florien.ampacheplayer.di.UserScope
import be.florien.ampacheplayer.view.player.filter.selectType.ALBUM_NAME
import be.florien.ampacheplayer.view.player.filter.selectType.ARTIST_NAME
import be.florien.ampacheplayer.view.player.filter.selectType.GENRE_NAME
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView

@ActivityScope
@UserScope
class AddFilterFragment @SuppressLint("ValidFragment")
constructor(private var filterType: String) : Fragment() { //todo reduce type available to warn when isn't complete

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
        fragmentBinding.filterList.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
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
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder = FilterViewHolder()

        override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
            holder.bind(vm.getDisplayedValues()[position])
        }

        override fun getItemCount(): Int = vm.getDisplayedValues().size

        override fun getSectionName(position: Int): String = vm.getDisplayedValues()[position].displayName.first().toUpperCase().toString()
    }

    inner class FilterViewHolder(private val itemFilterTypeBinding: ItemAddFilterBinding
                                 = ItemAddFilterBinding.inflate(layoutInflater, fragmentBinding.filterList, false))
        : RecyclerView.ViewHolder(itemFilterTypeBinding.root) {

        fun bind(filter: AddFilterFragmentVM.FilterItem) {
            itemFilterTypeBinding.vm = vm
            itemFilterTypeBinding.item = filter
        }
    }
}
