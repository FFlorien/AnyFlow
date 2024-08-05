package be.florien.anyflow.feature.library.podcast.ui.info

import androidx.lifecycle.ViewModelProvider
import be.florien.anyflow.architecture.di.viewModelFactory
import be.florien.anyflow.common.ui.data.info.InfoActions
import be.florien.anyflow.feature.library.podcast.domain.LibraryPodcastInfoActions
import be.florien.anyflow.feature.library.podcast.ui.list.LibraryPodcastListFragment
import be.florien.anyflow.feature.library.ui.R
import be.florien.anyflow.feature.library.ui.info.LibraryInfoFragment
import be.florien.anyflow.management.filters.model.Filter
import kotlin.random.Random

class LibraryPodcastInfoFragment(parentFilter: Filter<*>? = null) : LibraryInfoFragment(parentFilter) {
    override fun getTitle(): String = getString(R.string.menu_podcast)
    override fun getSubtitle(): String? = parentFilter?.getFullDisplay()
    override fun getLibraryInfoViewModel() = ViewModelProvider(
        requireActivity(),
        requireActivity().viewModelFactory
    )[Random(23).toString(), LibraryPodcastInfoViewModel::class.java]

    override fun executeAction(row: InfoActions.InfoRow) {
        val action = row.actionType
        when (action) {
            LibraryPodcastInfoActions.LibraryPodcastActionType.SubFilter -> {
                val value = LibraryPodcastInfoViewModel.PODCAST_EPISODE_ID
                viewModel.navigator.displayFragmentOnMain(
                    requireContext(),
                    LibraryPodcastListFragment(value, viewModel.filterNavigation),
                    LibraryPodcastListFragment::class.java.simpleName
                )
            }

            else -> viewModel.executeAction(row)
        }
    }
}
