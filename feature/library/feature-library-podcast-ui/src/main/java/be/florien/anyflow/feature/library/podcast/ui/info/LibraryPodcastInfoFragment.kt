package be.florien.anyflow.feature.library.podcast.ui.info

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.florien.anyflow.common.base.BaseFragment
import be.florien.anyflow.common.di.viewModelFactory
import be.florien.anyflow.common.resources.theming.AppTheme
import be.florien.anyflow.feature.library.podcast.ui.list.LibraryPodcastListFragment
import be.florien.anyflow.feature.library.ui.R
import be.florien.anyflow.feature.library.ui.info.LibraryActionType
import be.florien.anyflow.feature.library.ui.info.LibraryFieldType
import be.florien.anyflow.feature.library.ui.info.LibraryInfoRow
import be.florien.anyflow.feature.library.ui.info.LibraryInfoScreen
import be.florien.anyflow.management.filters.domain.model.Filter
import kotlinx.collections.immutable.persistentListOf
import kotlin.random.Random

class LibraryPodcastInfoFragment(val parentFilter: Filter? = null) : BaseFragment() {
    override fun getTitle(): String = getString(R.string.menu_podcast)
    override fun getSubtitle(): String? = parentFilter?.getFullDisplay()

    private lateinit var viewModel: LibraryPodcastInfoViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = ViewModelProvider(
            this,
            requireActivity().viewModelFactory
        )[Random(23).toString(), LibraryPodcastInfoViewModel::class.java]
        viewModel.filterNavigation = parentFilter
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return ComposeView(requireActivity()).apply {
            setContent {
                val state = viewModel.state.collectAsStateWithLifecycle(persistentListOf())
                AppTheme {
                    LibraryInfoScreen(
                        state.value,
                        {
                            executeAction(viewModel.getInfoRow(it))
                        }
                    )
                }
            }
        }
    }

    fun executeAction(row: LibraryInfoRow) {
        val action = row.actionType
        if (row.fieldType !is  LibraryFieldType.Podcast) {
            return
        }
        when (action) {
            LibraryActionType.SubFilter -> {
                val value = when (row.fieldType as LibraryFieldType.Podcast) {
                    LibraryFieldType.Podcast.Podcast -> LibraryPodcastInfoViewModel.PODCAST_ID
                    LibraryFieldType.Podcast.PodcastEpisode -> LibraryPodcastInfoViewModel.PODCAST_EPISODE_ID
                }

                viewModel.navigator.displayFragmentOnMain(
                    requireContext(),
                    LibraryPodcastListFragment(value, viewModel.filterNavigation),
                    "PODCAST",
                    LibraryPodcastListFragment::class.java.simpleName
                )
            }

            else -> viewModel.executeAction(row)
        }
    }
}
