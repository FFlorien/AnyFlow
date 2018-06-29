package be.florien.ampacheplayer.view.player.filter.display

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import be.florien.ampacheplayer.R
import be.florien.ampacheplayer.databinding.FragmentFilterBinding
import be.florien.ampacheplayer.persistence.local.model.Filter
import be.florien.ampacheplayer.view.player.PlayerActivity
import be.florien.ampacheplayer.view.player.filter.addition.AddFilterFragment
import be.florien.ampacheplayer.view.player.filter.selectType.AddFilterTypeFragment
import javax.inject.Inject


class FilterFragment : Fragment() {
    @Inject
    lateinit var vm: FilterFragmentVM

    private lateinit var binding: FragmentFilterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as PlayerActivity).activityComponent.inject(this)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentFilterBinding.inflate(inflater, container, false)
        binding.vm = vm
        binding.filterList.layoutManager = LinearLayoutManager(requireContext())
        binding.filterList.adapter = FilterListAdapter()
        return binding.root
    }

    inner class FilterListAdapter : RecyclerView.Adapter<FilterViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder = FilterViewHolder()

        override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
            if (position == 0) {
                (holder.itemView as TextView).text = "Clear filters"
                holder.itemView.setOnClickListener { vm.clearFilters() }
            }else if (position == itemCount-1) {
                (holder.itemView as TextView).text = "Add filters"
                holder.itemView.setOnClickListener {
                    requireActivity()
                            .supportFragmentManager
                            .beginTransaction()
                            .replace(R.id.container, AddFilterTypeFragment(), AddFilterTypeFragment::class.java.simpleName)
                            .commitNow()
                }
            } else {
                holder.bind(vm.currentFilters[position -1])
                holder.itemView.setOnClickListener(null)
            }
        }

        override fun getItemCount(): Int = vm.currentFilters.size + 2
    }

    inner class FilterViewHolder : RecyclerView.ViewHolder(layoutInflater.inflate(R.layout.item_filter_active, binding.filterList, false)) {

        fun bind(filter: Filter<*>) {
            (itemView as TextView).text = "${filter.clause} ${filter.argument?.toString()}"
        }
    }
}