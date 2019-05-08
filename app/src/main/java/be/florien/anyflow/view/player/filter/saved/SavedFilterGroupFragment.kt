package be.florien.anyflow.view.player.filter.saved

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.Observable
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.R
import be.florien.anyflow.databinding.FragmentSavedFilterGroupBinding
import be.florien.anyflow.databinding.ItemFilterGroupBinding
import be.florien.anyflow.persistence.local.model.FilterGroup
import be.florien.anyflow.view.player.PlayerActivity
import be.florien.anyflow.view.player.filter.BaseFilterFragment
import be.florien.anyflow.view.player.filter.BaseFilterVM

class SavedFilterGroupFragment : BaseFilterFragment() {

    private lateinit var vm: SavedFilterGroupVM
    override val baseVm: BaseFilterVM
        get() = vm

    private lateinit var binding: FragmentSavedFilterGroupBinding

    override fun getTitle(): String = getString(R.string.filter_title_saved)

    override fun onAttach(context: Context) {
        super.onAttach(context)
        vm = SavedFilterGroupVM(requireActivity() as PlayerActivity)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSavedFilterGroupBinding.inflate(inflater, container, false)
        binding.savedList.layoutManager = GridLayoutManager(requireContext(), 3, RecyclerView.VERTICAL, false)
        binding.savedList.adapter = FilterGroupAdapter()
        vm.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                binding.savedList.adapter?.notifyDataSetChanged()
            }

        })
        return binding.root
    }

    inner class FilterGroupViewHolder(
            container: ViewGroup,
            private val binding: ItemFilterGroupBinding = ItemFilterGroupBinding.inflate(LayoutInflater.from(container.context), container, false))
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(filterGroup: FilterGroup, coverUrls: List<String>) {
            binding.filterGroup = filterGroup
            binding.cover1Url = coverUrls[0]
            binding.cover2Url = coverUrls[1]
            binding.cover3Url = coverUrls[2]
            binding.cover4Url = coverUrls[3]
        }
    }

    inner class FilterGroupAdapter : RecyclerView.Adapter<FilterGroupViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterGroupViewHolder = FilterGroupViewHolder(parent)

        override fun getItemCount(): Int = vm.filterGroups.size

        override fun onBindViewHolder(holder: FilterGroupViewHolder, position: Int) {
            holder.bind(vm.filterGroups[position], vm.imageForGroups[position])

        }

    }
}