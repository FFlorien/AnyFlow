package be.florien.anyflow.feature.library.podcast.ui.list

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModelProvider
import be.florien.anyflow.architecture.di.ActivityScope
import be.florien.anyflow.architecture.di.ServerScope
import be.florien.anyflow.architecture.di.viewModelFactory
import be.florien.anyflow.common.ui.list.DetailViewHolderListener
import be.florien.anyflow.feature.library.domain.model.FilterItem
import be.florien.anyflow.feature.library.podcast.ui.info.LibraryPodcastInfoFragment
import be.florien.anyflow.feature.library.podcast.ui.info.LibraryPodcastInfoViewModel
import be.florien.anyflow.feature.library.podcast.ui.list.viewmodels.LibraryPodcastEpisodeListViewModel
import be.florien.anyflow.feature.library.ui.R
import be.florien.anyflow.feature.library.ui.list.LibraryListFragment
import be.florien.anyflow.management.filters.model.Filter

@ActivityScope
@ServerScope
class LibraryPodcastListFragment @SuppressLint("ValidFragment") constructor(
    filterType: String = LibraryPodcastInfoViewModel.PODCAST_EPISODE_ID,
    parentFilter: Filter<*>? = null
) : LibraryListFragment(filterType, parentFilter),
    DetailViewHolderListener<FilterItem> {

    override fun getViewModel(filterName: String) =
        ViewModelProvider(this, requireActivity().viewModelFactory)[
            LibraryPodcastEpisodeListViewModel::class.java]

    override fun getTitle(): String = getString(R.string.menu_podcast)

    override fun getSubtitle(): String? = when (filterType) {
        LibraryPodcastInfoViewModel.PODCAST_EPISODE_ID -> getString(R.string.library_type_podcast_episode)
        else -> null
    }

    override fun onInfoDisplayAsked(item: FilterItem) {
        val filter = viewModel.getFilter(item)
        navigator.displayFragmentOnMain(
            requireContext(),
            LibraryPodcastInfoFragment(filter),
            "PODCAST",
            LibraryPodcastInfoFragment::class.java.simpleName
        )
    }
}
