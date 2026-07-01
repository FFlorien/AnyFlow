package be.florien.anyflow.feature.filter.saved.ui

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import be.florien.anyflow.common.di.AnyFlowViewModelFactory
import be.florien.anyflow.common.navigation.BottomNavDestination


fun EntryProviderScope<NavKey>.savedFiltersEntry(
    viewModelFactory: AnyFlowViewModelFactory
) {
    entry<BottomNavDestination.FilterHistory> {
        val viewModel =
            viewModel<SavedFilterGroupViewModel>(factory = viewModelFactory)
        val state = viewModel.state.collectAsStateWithLifecycle().value
        SavedFilterGroupScreen(state, viewModel::changeForSavedGroup)
    }
}