package be.florien.anyflow.tags.local

import be.florien.anyflow.management.filters.domain.model.FilterParam
import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.management.filters.domain.model.PodcastFilterType
import be.florien.anyflow.management.filters.domain.model.TagFilterType
import be.florien.anyflow.tags.local.query.QueryFilter


// region view to utilities

fun Iterable<Filter>.toQueryFilters() = map { it.toQueryFilter() }

fun Iterable<FilterParam<*>>.toQueryFilter(): QueryFilter {
    val topQueryFilter: QueryFilter = first().toQueryFilter(0)
    var previousQueryFilter: QueryFilter? = topQueryFilter
    drop(1).forEachIndexed { index, filter ->
        val queryFilter = filter.toQueryFilter(index + 1)
        previousQueryFilter?.child = queryFilter
        previousQueryFilter = queryFilter
    }

    return topQueryFilter
}

fun FilterParam<*>.toQueryFilter(level: Int = 0): QueryFilter {
    val argument = argument
    return QueryFilter(
        type = when (type) {
            TagFilterType.GENRE_IS -> QueryFilter.FilterType.GENRE_IS
            TagFilterType.SONG_IS -> QueryFilter.FilterType.SONG_IS
            TagFilterType.ARTIST_IS -> QueryFilter.FilterType.ARTIST_IS
            TagFilterType.ALBUM_ARTIST_IS -> QueryFilter.FilterType.ALBUM_ARTIST_IS
            TagFilterType.ALBUM_IS -> QueryFilter.FilterType.ALBUM_IS
            TagFilterType.DISK_IS -> QueryFilter.FilterType.DISK_IS
            TagFilterType.PLAYLIST_IS -> QueryFilter.FilterType.PLAYLIST_IS
            TagFilterType.DOWNLOADED_STATUS_IS -> QueryFilter.FilterType.DOWNLOADED_STATUS_IS
            PodcastFilterType.PODCAST_IS -> QueryFilter.FilterType.PODCAST_IS
            PodcastFilterType.PODCAST_EPISODE_IS -> QueryFilter.FilterType.PODCAST_EPISODE_IS
            PodcastFilterType.STATE_IS -> QueryFilter.FilterType.STATE_IS
        },
        argument = when (argument) {
            is Boolean -> if (argument) "NOT NULL" else "NULL"
            else -> argument.toString()
        },
        level = level
    )
}