package be.florien.anyflow.feature.player.filter.selectType

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.R
import be.florien.anyflow.databinding.FragmentSelectFilterTypeBinding
import be.florien.anyflow.extension.viewModelFactory
import be.florien.anyflow.feature.player.filter.BaseFilterFragment
import be.florien.anyflow.feature.player.filter.FilterActions
import be.florien.anyflow.feature.player.info.InfoAdapter

class SelectFilterTypeFragment : BaseFilterFragment() {
    override fun getTitle(): String = getString(R.string.filter_title_main)
    override val filterActions: FilterActions
        get() = viewModel
    lateinit var viewModel: SelectFilterTypeViewModel
    private lateinit var fragmentBinding: FragmentSelectFilterTypeBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = ViewModelProvider(
            requireActivity(),
            requireActivity().viewModelFactory
        )[SelectFilterTypeViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentBinding = FragmentSelectFilterTypeBinding.inflate(inflater, container, false)
        fragmentBinding.lifecycleOwner = viewLifecycleOwner
        fragmentBinding.viewModel = viewModel
        fragmentBinding.filterList.layoutManager =
            LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        val infoAdapter = InfoAdapter(viewModel::executeAction)
        fragmentBinding.filterList.adapter = infoAdapter
        viewModel.infoRows.observe(viewLifecycleOwner) {
            infoAdapter.submitList(it)
        }
        return fragmentBinding.root
    }

    override fun onResume() {
        super.onResume()
        viewModel.filter = null //todo
    }
}
