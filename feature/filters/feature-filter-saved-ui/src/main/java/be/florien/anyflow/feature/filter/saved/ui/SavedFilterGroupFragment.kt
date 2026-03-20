package be.florien.anyflow.feature.filter.saved.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.florien.anyflow.common.di.viewModelFactory
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.common.resources.theming.AppTheme
import be.florien.anyflow.feature.library.ui.BaseFilteringFragment
import be.florien.anyflow.feature.library.ui.LibraryViewModel
import be.florien.anyflow.feature.library.ui.R

class SavedFilterGroupFragment : BaseFilteringFragment() {

    private lateinit var viewModel: SavedFilterGroupViewModel
    override val libraryViewModel: LibraryViewModel
        get() = viewModel
    override val navigator: Navigator
        get() = viewModel.navigator

    override fun getTitle(): String = getString(R.string.filter_title_saved)

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = ViewModelProvider(
            this,
            requireActivity().viewModelFactory
        )[SavedFilterGroupViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            val state = viewModel.state.collectAsStateWithLifecycle().value
            AppTheme {
                SavedFilterGroupScreen(state, viewModel::changeForSavedGroup)
            }
        }
    }
}