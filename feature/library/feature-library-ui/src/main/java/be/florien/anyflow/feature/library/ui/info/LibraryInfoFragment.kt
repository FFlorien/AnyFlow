package be.florien.anyflow.feature.library.ui.info

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.florien.anyflow.common.base.BaseFragment
import be.florien.anyflow.common.di.viewModelFactory
import be.florien.anyflow.common.resources.theming.AppTheme
import be.florien.anyflow.feature.library.ui.R
import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.management.filters.domain.model.FilterParam
import kotlinx.collections.immutable.persistentListOf
import kotlin.random.Random

class LibraryInfoFragment(
    var type: String = LibraryInfoViewModel.TAGS_TYPE,
    var parentFilter: Filter? = null
) : BaseFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val args = arguments
        if (args == null) {
            arguments = Bundle().apply {
                putString("type", type)
                parentFilter?.toTypedArray()?.let { putParcelableArray("parentFilter", it) }
            }
        } else {
            type = args.getString("type", LibraryInfoViewModel.TAGS_TYPE)
            parentFilter = (args.getParcelableArray("parentFilter") as? Array<FilterParam<*>>)
                ?.let { Filter(*it) }
        }
        super.onCreate(savedInstanceState)
    }


    override fun getTitle(): String =
        if (type == LibraryInfoViewModel.TAGS_TYPE) getString(R.string.library_title_main) else getString(
            R.string.menu_podcast
        )

    override fun getSubtitle(): String? = parentFilter?.getFullDisplay()

    lateinit var viewModel: LibraryInfoViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = ViewModelProvider(
            this,
            requireActivity().viewModelFactory
        )[type + Random(23).toString(), LibraryInfoViewModel::class.java]
        viewModel.setType(type)
        viewModel.filterNavigation = parentFilter
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireActivity()).apply {
        setContent {
            val state = viewModel.state.collectAsStateWithLifecycle(persistentListOf())
            AppTheme(authenticationInterceptor = viewModel.authenticationInterceptor) {
                LibraryInfoScreen(
                    state.value,
                    {
                        viewModel.executeAction(this@LibraryInfoFragment, it, type)
                    }
                )
            }
        }
    }
}