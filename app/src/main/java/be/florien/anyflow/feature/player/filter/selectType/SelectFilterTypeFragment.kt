package be.florien.anyflow.feature.player.filter.selectType

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.R
import be.florien.anyflow.databinding.FragmentSelectFilterTypeBinding
import be.florien.anyflow.databinding.ItemSelectFilterTypeBinding
import be.florien.anyflow.extension.viewModelFactory
import be.florien.anyflow.feature.player.PlayerActivity
import be.florien.anyflow.feature.player.filter.BaseFilterFragment
import be.florien.anyflow.feature.player.filter.BaseFilterViewModel
import be.florien.anyflow.feature.player.filter.selection.SelectFilterFragment

class SelectFilterTypeFragment : BaseFilterFragment() {
    override fun getTitle(): String = getString(R.string.filter_title_main)
    override val baseViewModel: BaseFilterViewModel
        get() = viewModel
    lateinit var viewModel: SelectFilterTypeViewModel
    private lateinit var fragmentBinding: FragmentSelectFilterTypeBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = ViewModelProvider(requireActivity(), requireActivity().viewModelFactory)[SelectFilterTypeViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        fragmentBinding = FragmentSelectFilterTypeBinding.inflate(inflater, container, false)
        fragmentBinding.lifecycleOwner = viewLifecycleOwner
        fragmentBinding.viewModel = viewModel
        fragmentBinding.filterList.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        fragmentBinding.filterList.adapter = FilterListAdapter()
        ResourcesCompat.getDrawable(resources, R.drawable.sh_divider, requireActivity().theme)?.let {
            fragmentBinding.filterList.addItemDecoration(DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL).apply { setDrawable(it) })
            fragmentBinding.filterList.addItemDecoration(DividerItemDecoration(requireActivity(), DividerItemDecoration.HORIZONTAL).apply { setDrawable(it) })
        }
        return fragmentBinding.root
    }

    inner class FilterListAdapter : RecyclerView.Adapter<FilterTypeViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterTypeViewHolder = FilterTypeViewHolder()

        override fun onBindViewHolder(holder: FilterTypeViewHolder, position: Int) {
            holder.bind(viewModel.filtersIds[position], viewModel.filtersNames[position], viewModel.filtersImages[position])
        }

        override fun getItemCount(): Int = viewModel.filtersIds.size
    }

    inner class FilterTypeViewHolder(
            private val itemFilterTypeBinding: ItemSelectFilterTypeBinding = ItemSelectFilterTypeBinding.inflate(layoutInflater, fragmentBinding.filterList, false)
    ) : RecyclerView.ViewHolder(itemFilterTypeBinding.root) {

        fun bind(type: String, @StringRes name: Int, @DrawableRes drawableRes: Int) {
            itemFilterTypeBinding.lifecycleOwner = viewLifecycleOwner
            itemFilterTypeBinding.filterName.setText(name)
            itemFilterTypeBinding.imageView.setImageResource(drawableRes)
            itemView.setOnClickListener {
                (activity as PlayerActivity).supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.container, SelectFilterFragment(type), SelectFilterFragment::class.java.simpleName)
                        .addToBackStack(null)
                        .commit()
            }
        }
    }
}
