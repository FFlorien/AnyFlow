package be.florien.anyflow.feature.library.podcast.ui.info

import be.florien.anyflow.common.ui.data.info.InfoActions
import be.florien.anyflow.common.ui.navigation.Navigator
import be.florien.anyflow.feature.library.podcast.domain.LibraryPodcastInfoActions
import be.florien.anyflow.feature.library.podcast.domain.LibraryPodcastRepository
import be.florien.anyflow.feature.library.ui.LibraryViewModel
import be.florien.anyflow.feature.library.ui.info.LibraryInfoViewModel
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.model.Filter
import javax.inject.Inject

class LibraryPodcastInfoViewModel @Inject constructor(
    libraryTagsRepository: LibraryPodcastRepository,
    filtersManager: FiltersManager,
    navigator: Navigator
) : LibraryInfoViewModel(filtersManager, navigator), LibraryViewModel {

    override val infoActions: InfoActions<Filter<*>?> = LibraryPodcastInfoActions(libraryTagsRepository)

    companion object {
        const val PODCAST_EPISODE_ID = "PodcastEpisode"
    }
}