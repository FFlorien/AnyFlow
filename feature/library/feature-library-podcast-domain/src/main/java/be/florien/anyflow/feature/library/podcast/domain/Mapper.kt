package be.florien.anyflow.feature.library.podcast.domain

import be.florien.anyflow.common.utils.TimeOperations
import be.florien.anyflow.feature.library.domain.model.FilterItem
import be.florien.anyflow.feature.library.tags.domain.model.IdText
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.management.filters.domain.model.FilterParam
import be.florien.anyflow.management.filters.domain.model.PodcastFilterType
import be.florien.anyflow.management.podcast.model.PodcastDisplay
import be.florien.anyflow.management.podcast.model.PodcastEpisodeDisplay
import be.florien.anyflow.urls.UrlRepository

internal fun PodcastDisplay.toFilterItem(
    parentFilter: Filter?,
    urlRepository: UrlRepository,
    filtersManager: FiltersManager
): FilterItem {
    val filterParam = FilterParam(
        PodcastFilterType.PODCAST_IS,
        id,
        name
    )
    val filterInHierarchy = parentFilter.withChild(filterParam)

    return FilterItem(
        id = id,
        title = name,
        isSelected = filtersManager.isFilterInEdition(filterInHierarchy),
        section = name.first().uppercase(),
        artUrl = urlRepository.getArtUrl("podcast", id),
    )
}

internal fun PodcastEpisodeDisplay.toFilterItem(
    parentFilter: Filter?,
    urlRepository: UrlRepository,
    filtersManager: FiltersManager
): FilterItem {
    val filterParam = FilterParam(
        PodcastFilterType.PODCAST_EPISODE_IS,
        id,
        title
    )
    val filterInHierarchy = parentFilter.withChild(filterParam)
    return (FilterItem(
        id = id,
        title = title,
        isSelected = filtersManager.isFilterInEdition(filterInHierarchy),
        section = TimeOperations.toDisplayMonthDate(publicationDate),
        artUrl = urlRepository.getArtUrl("podcast", podcastId),
        duration = TimeOperations.toMediaDuration(time),
        subtitle = podcast
    ))
}

internal fun PodcastDisplay.toIdText() = IdText(id, name)

internal fun PodcastEpisodeDisplay.toIdText() = IdText(id, title)

private fun Filter?.withChild(filterParam: FilterParam<*>): Filter {
    if (this == null) {
        return Filter(filterParam)
    }
    add(filterParam)
    return this
}