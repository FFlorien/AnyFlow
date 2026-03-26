package be.florien.anyflow.feature.library.podcast.ui.list

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModelProvider
import be.florien.anyflow.common.di.ActivityScope
import be.florien.anyflow.common.di.ServerScope
import be.florien.anyflow.common.di.viewModelFactory
import be.florien.anyflow.feature.library.podcast.ui.info.LibraryPodcastInfoFragment
import be.florien.anyflow.feature.library.podcast.ui.info.LibraryPodcastInfoViewModel
import be.florien.anyflow.feature.library.podcast.ui.list.viewmodels.LibraryPodcastEpisodeListViewModel
import be.florien.anyflow.feature.library.podcast.ui.list.viewmodels.LibraryPodcastListViewModel
import be.florien.anyflow.feature.library.ui.R
import be.florien.anyflow.feature.library.ui.list.FilterDisplay
import be.florien.anyflow.feature.library.ui.list.LibraryListFragment
import be.florien.anyflow.feature.library.ui.toItem
import be.florien.anyflow.management.filters.domain.model.Filter

@ActivityScope
@ServerScope
class LibraryPodcastListFragment @SuppressLint("ValidFragment") constructor(
    filterType: String = LibraryPodcastInfoViewModel.PODCAST_EPISODE_ID,
    parentFilter: Filter? = null
) : LibraryListFragment(filterType, parentFilter) {

    override fun getViewModel(filterName: String) =
        ViewModelProvider(this, requireActivity().viewModelFactory)[
            when(filterName){
                LibraryPodcastInfoViewModel.PODCAST_ID -> LibraryPodcastListViewModel::class.java
                LibraryPodcastInfoViewModel.PODCAST_EPISODE_ID ->LibraryPodcastEpisodeListViewModel::class.java
                else -> LibraryPodcastListViewModel::class.java
            }]

    override fun getTitle(): String = getString(R.string.menu_podcast)

    override fun getSubtitle(): String? = when (filterType) {
        LibraryPodcastInfoViewModel.PODCAST_ID -> getString(R.string.library_type_podcast)
        LibraryPodcastInfoViewModel.PODCAST_EPISODE_ID -> getString(R.string.library_type_podcast_episode)
        else -> null
    }

    override fun onInfoDisplayAsked(item: FilterDisplay) {
        val filter = viewModel.getFilter(item.toItem())
        navigator.displayFragmentOnMain(
            requireContext(),
            LibraryPodcastInfoFragment(filter),
            "PODCAST",
            LibraryPodcastInfoFragment::class.java.simpleName
        )
    }
}
