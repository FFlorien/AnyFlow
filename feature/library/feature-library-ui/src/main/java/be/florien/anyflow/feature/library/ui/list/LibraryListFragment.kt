package be.florien.anyflow.feature.library.ui.list

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.paging.compose.collectAsLazyPagingItems
import be.florien.anyflow.common.resources.theming.AppTheme
import be.florien.anyflow.feature.library.ui.BaseFilteringFragment
import be.florien.anyflow.feature.library.ui.LibraryViewModel
import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.management.filters.domain.model.FilterParam
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okhttp3.OkHttpClient

abstract class LibraryListFragment @SuppressLint("ValidFragment")
constructor(
    var filterType: String,
    private var parentFilterParam: Filter? = null
) : BaseFilteringFragment() {

    companion object {
        private const val FILTER_TYPE = "TYPE"
        private const val PARENT_FILTER = "PARENT_FILTER"
    }

    override val libraryViewModel: LibraryViewModel
        get() = viewModel
    override val navigator: be.florien.anyflow.common.navigation.Navigator
        get() = viewModel.navigator
    lateinit var viewModel: LibraryListViewModel

    abstract fun getViewModel(filterName: String): LibraryListViewModel
    abstract fun onInfoDisplayAsked(item: FilterDisplay)

    init {
        arguments?.let {
            filterType = it.getString(FILTER_TYPE, "Error")
            parentFilterParam =
                Filter(*(it.getParcelableArray(PARENT_FILTER) as Array<FilterParam<*>>))
        }
        if (arguments == null) {
            arguments = Bundle().apply {
                putString(FILTER_TYPE, filterType)
                putParcelableArray(PARENT_FILTER, parentFilterParam?.toTypedArray())
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = getViewModel(filterType)
        viewModel.navigationFilter = parentFilterParam
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {

                val items = viewModel.values.collectAsLazyPagingItems()
                AppTheme {
                    setSingletonImageLoaderFactory { context -> //todo move to the top of content once in SingleActivity
                        ImageLoader.Builder(context)
                            .components {
                                add(
                                    OkHttpNetworkFetcherFactory(
                                        callFactory = {
                                            OkHttpClient.Builder()
                                                .addInterceptor(viewModel.authenticationInterceptor)
                                                .build()
                                        }
                                    )
                                )
                            }
                            .build()
                    }
                    LibraryListScreen(
                        itemCount = items.itemCount,
                        getItem = items::get,
                        onClick = viewModel::toggleFilterSelection,
                        onNavigation = ::onInfoDisplayAsked
                    )
                }
            }
        }
    }
}