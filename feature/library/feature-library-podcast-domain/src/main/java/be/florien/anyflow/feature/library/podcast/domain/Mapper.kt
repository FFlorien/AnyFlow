package be.florien.anyflow.feature.library.podcast.domain

import be.florien.anyflow.common.ui.data.TextConfig
import be.florien.anyflow.common.ui.data.TextConfigStyle
import be.florien.anyflow.common.utils.TimeOperations
import be.florien.anyflow.feature.library.domain.model.FilterItem
import be.florien.anyflow.feature.library.tags.domain.model.IdText
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
        TextConfig(title),
        filtersManager.isFilterInEdition(filterInHierarchy),
        urlRepository.getArtUrl("podcast", podcastId),
        TimeOperations.toMediaDuration(time),
        TextConfig(R.string.library_by, nextTextConfig = TextConfig(podcast, TextConfigStyle.BOLD))
    ))
}

internal fun PodcastEpisodeDisplay.toIdText() = IdText(id, title)

private fun Filter<*>?.withChild(filter: Filter<*>): Filter<*> {
    if (this == null) {
        return filter
    }
    val copy = deepCopy()
    copy.addToDeepestChild(filter)
    return copy
}