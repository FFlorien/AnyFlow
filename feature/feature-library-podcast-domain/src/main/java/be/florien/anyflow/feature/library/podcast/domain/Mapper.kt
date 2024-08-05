package be.florien.anyflow.feature.library.podcast.domain

import be.florien.anyflow.feature.library.podcast.domain.LibraryPodcastInfoActions.DisplayData
import be.florien.anyflow.feature.library.domain.model.FilterItem
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.model.Filter
import be.florien.anyflow.management.podcast.model.PodcastEpisodeDisplay
import be.florien.anyflow.tags.UrlRepository

internal fun PodcastEpisodeDisplay.toFilterItem(
    parentFilter: Filter<*>?,
    urlRepository: UrlRepository,
    filtersManager: FiltersManager
): FilterItem {
    val filter = Filter(
        Filter.FilterType.PODCAST_EPISODE_IS,
        id, title
    )
    val filterInHierarchy = parentFilter.withChild(filter)
    return (FilterItem(
        id,
        "$title\nby $author", //todo wut ? i18n ?  + from ${podcastEpisode.albumName}
        filtersManager.isFilterInEdition(filterInHierarchy),
        urlRepository.getArtUrl("podcast", albumId),
    ))
}

internal fun PodcastEpisodeDisplay.toDisplayData() = DisplayData(title, id)

private fun Filter<*>?.withChild(filter: Filter<*>): Filter<*> {
    if (this == null) {
        return filter
    }
    val copy = deepCopy()
    copy.addToDeepestChild(filter)
    return copy
}