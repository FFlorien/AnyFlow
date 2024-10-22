package be.florien.anyflow.feature.library.ui.info

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.florien.anyflow.common.ui.data.info.InfoActions
import be.florien.anyflow.common.ui.info.InfoAdapter
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.common.ui.info.InfoRow
import be.florien.anyflow.feature.library.ui.BaseFilteringFragment
import be.florien.anyflow.feature.library.ui.LibraryViewModel
import be.florien.anyflow.feature.library.ui.cancelChanges
import be.florien.anyflow.feature.library.ui.databinding.FragmentSelectFilterTypeBinding
import be.florien.anyflow.management.filters.model.Filter

abstract class LibraryInfoFragment<IA : InfoActions<Filter<*>?>>(var parentFilter: Filter<*>? = null) :
    BaseFilteringFragment() {

    override val libraryViewModel: LibraryViewModel
        get() = viewModel
    override val navigator: Navigator
        get() = viewModel.navigator
    lateinit var viewModel: LibraryInfoViewModel<IA>
    private lateinit var fragmentBinding: FragmentSelectFilterTypeBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = getLibraryInfoViewModel() //todo handle this gracefully !
        viewModel.filterNavigation = parentFilter
    }

    abstract fun getLibraryInfoViewModel(): LibraryInfoViewModel<IA>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentBinding = FragmentSelectFilterTypeBinding.inflate(inflater, container, false)
        fragmentBinding.lifecycleOwner = viewLifecycleOwner
        fragmentBinding.filterList.layoutManager =
            LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        val infoAdapter = InfoAdapter(this::executeAction)
        fragmentBinding.filterList.adapter = infoAdapter
        viewModel.infoRows.observe(viewLifecycleOwner) { infoRowList ->
            infoAdapter.submitList(infoRowList.map { it.toInfoRow() })
        }
        return fragmentBinding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        if (viewModel.filterNavigation == null) {
            viewModel.cancelChanges()
        }
    }

    private fun executeAction(row: InfoRow) {
        if (row.tag is InfoActions.InfoRow) {
            executeAction(row.tag as InfoActions.InfoRow)
        }
    }

    abstract fun executeAction(row: InfoActions.InfoRow)

    abstract fun InfoActions.InfoRow.toInfoRow(): InfoRow
}
