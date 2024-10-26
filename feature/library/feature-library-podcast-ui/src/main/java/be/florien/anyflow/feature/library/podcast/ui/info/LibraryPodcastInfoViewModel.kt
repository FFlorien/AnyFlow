package be.florien.anyflow.feature.library.podcast.ui.info

import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.feature.library.podcast.domain.LibraryInfoRow
import be.florien.anyflow.feature.library.podcast.domain.LibraryPodcastActionType
import be.florien.anyflow.feature.library.podcast.domain.LibraryPodcastFieldType
import be.florien.anyflow.feature.library.podcast.domain.LibraryPodcastRepository
import be.florien.anyflow.feature.library.ui.LibraryViewModel
import be.florien.anyflow.feature.library.ui.info.LibraryInfoViewModel
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.model.Filter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LibraryPodcastInfoViewModel @Inject constructor(
    private val libraryPodcastRepository: LibraryPodcastRepository,
    filtersManager: FiltersManager,
    navigator: Navigator
) : LibraryInfoViewModel<LibraryInfoRow>(filtersManager, navigator), LibraryViewModel {
    override fun getArtUrl(artType: String, id: Long) = libraryPodcastRepository.getArtUrl(artType, id)

    override suspend fun getInfoRowList(): MutableList<LibraryInfoRow> {
        val count =
            withContext(Dispatchers.IO) { libraryPodcastRepository.getFilteredInfo(filterNavigation) }
        return mutableListOf(
            LibraryInfoRow(
                LibraryPodcastFieldType.PodcastEpisode,
                if (count.podcastEpisodes > 1) LibraryPodcastActionType.SubFilter else LibraryPodcastActionType.InfoTitle,
                count.podcastEpisodes
            )
        )
    }

    override suspend fun getFilteredInfo(
        filterType: Filter.FilterType,
        filter: Filter<*>?
    ) = when (filterType) { //todo separate podcast & tags
        Filter.FilterType.PODCAST_EPISODE_IS -> libraryPodcastRepository.getPodcastEpisodeList(filter)
        else -> listOf(null)

    }.firstOrNull()

    companion object {
        const val PODCAST_EPISODE_ID = "PodcastEpisode"
    }
}