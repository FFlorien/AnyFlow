package be.florien.ampacheplayer.view.player.filter

import android.databinding.DataBindingUtil
import android.databinding.Observable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import be.florien.ampacheplayer.BR
import be.florien.ampacheplayer.R
import be.florien.ampacheplayer.databinding.FragmentFilterBinding
import be.florien.ampacheplayer.databinding.ItemFilterTypeBinding
import be.florien.ampacheplayer.view.player.PlayerActivity
import javax.inject.Inject

/**
 * Created by FlamentF on 08-Jan-18.
 */
class FilterFragment : Fragment() {
    @Inject lateinit var vm: FilterFragmentVM
    private lateinit var fragmentBinding: FragmentFilterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as PlayerActivity).activityComponent.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_filter, container, false)
        fragmentBinding.vm = vm
        fragmentBinding.filterList.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        fragmentBinding.filterList.adapter = FilterListAdapter()
        vm.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(observable: Observable, id: Int) {
                when (id) {
                    BR.filterList -> fragmentBinding.filterList.adapter.notifyDataSetChanged()
                }
            }
        })
        return fragmentBinding.root
    }

    inner class FilterListAdapter : RecyclerView.Adapter<FilterViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): FilterViewHolder = FilterViewHolder()

        override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
            holder.bind(vm.getFilterList()[position])
        }

        override fun getItemCount(): Int = vm.getFilterList().size
    }

    inner class FilterViewHolder(
            private val itemFilterTypeBinding: ItemFilterTypeBinding = DataBindingUtil.inflate(layoutInflater, R.layout.item_filter_type, fragmentBinding.filterList, false)
    ) : RecyclerView.ViewHolder(itemFilterTypeBinding.root) {
        fun bind(filterType: String) {
            itemFilterTypeBinding.filterValue = filterType
            itemFilterTypeBinding.filterName.text = filterType
        }
    }
}
