package be.florien.anyflow.view.player.filter.selectType

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import be.florien.anyflow.R
import be.florien.anyflow.databinding.FragmentAddFilterBinding
import be.florien.anyflow.databinding.ItemAddFilterTypeBinding
import be.florien.anyflow.di.ActivityScope
import be.florien.anyflow.di.UserScope
import be.florien.anyflow.view.player.PlayerActivity
import be.florien.anyflow.view.player.filter.addition.AddFilterFragment
import javax.inject.Inject

/**
 * Created by FlamentF on 08-Jan-18.
 */
@ActivityScope
@UserScope
class AddFilterTypeFragment : Fragment() {
    @Inject lateinit var vm: AddFilterTypeFragmentVM
    private lateinit var fragmentBinding: FragmentAddFilterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as PlayerActivity).activityComponent.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentBinding = FragmentAddFilterBinding.inflate(inflater, container, false)
        fragmentBinding.filterList.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        fragmentBinding.filterList.adapter = FilterListAdapter()
        return fragmentBinding.root
    }

    inner class FilterListAdapter : RecyclerView.Adapter<FilterTypeViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterTypeViewHolder = FilterTypeViewHolder()

        override fun onBindViewHolder(holder: FilterTypeViewHolder, position: Int) {
            holder.bind(vm.filtersNames[position])
        }

        override fun getItemCount(): Int = vm.filtersNames.size
    }

    inner class FilterTypeViewHolder(
            private val itemFilterTypeBinding: ItemAddFilterTypeBinding = ItemAddFilterTypeBinding.inflate(layoutInflater, fragmentBinding.filterList, false)
    ) : RecyclerView.ViewHolder(itemFilterTypeBinding.root) {

        fun bind(type: String) {
            itemFilterTypeBinding.filterName.text = type
            itemView.setOnClickListener {
                (activity as PlayerActivity).supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.container, AddFilterFragment(type), AddFilterFragment::class.java.simpleName)
                        .commitNow()
            }
        }
    }
}
