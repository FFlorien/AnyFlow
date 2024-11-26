package be.florien.anyflow.feature.library.tags.domain

import be.florien.anyflow.common.ui.data.TextConfig
import be.florien.anyflow.common.ui.data.TextConfigStyle
import be.florien.anyflow.feature.library.domain.model.FilterItem
import be.florien.anyflow.feature.library.tags.domain.model.IdText
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.model.Filter
import be.florien.anyflow.management.playlist.model.Playlist
import be.florien.anyflow.management.playlist.model.PlaylistWithCount
import be.florien.anyflow.tags.UrlRepository
import be.florien.anyflow.tags.model.Album
import be.florien.anyflow.tags.model.Artist
import be.florien.anyflow.tags.model.DownloadedCount
import be.florien.anyflow.tags.model.Genre
import be.florien.anyflow.tags.model.SongDisplayDomain


internal fun SongDisplayDomain.toFilterItem(
    parentFilter: Filter<*>?,
    urlRepository: UrlRepository,
    filtersManager: FiltersManager
): FilterItem {
    val artUrl = urlRepository.getAlbumArtUrl(albumId)
    val filter = Filter(Filter.FilterType.SONG_IS, id, title)
    val filterWithHierarchy = parentFilter.withChild(filter)
    return FilterItem(
        id,
        TextConfig(title),
        filtersManager.isFilterInEdition(filterWithHierarchy),
        artUrl,
        subtitle = TextConfig(R.string.library_by, nextTextConfig = TextConfig(artistName, TextConfigStyle.BOLD)),
        subsubtitle = TextConfig(R.string.library_from, nextTextConfig = TextConfig(albumName, TextConfigStyle.ITALIC))
    )
}

internal fun Artist.toFilterItem(
    parentFilter: Filter<*>?,
    urlRepository: UrlRepository,
    filtersManager: FiltersManager
): FilterItem {
    val artUrl = urlRepository.getArtistArtUrl(id)
    val filter =
        Filter(Filter.FilterType.ALBUM_ARTIST_IS, id, name)
    val filterWithHierarchy = parentFilter.withChild(filter)

    return FilterItem(
        id,
        TextConfig(name),
        filtersManager.isFilterInEdition(filterWithHierarchy),
        artUrl
    )
}

internal fun Album.toFilterItem(
    parentFilter: Filter<*>?,
    urlRepository: UrlRepository,
    filtersManager: FiltersManager
): FilterItem {
    val artUrl = urlRepository.getAlbumArtUrl(id)
    val filter =
        Filter(
            Filter.FilterType.ALBUM_IS,
            id,
            name,
            emptyList()
        )
    val filterInHierarchy = parentFilter.withChild(filter)
    return FilterItem(
        id,
        TextConfig(name),
        filtersManager.isFilterInEdition(filterInHierarchy),
        artUrl,
        subtitle = TextConfig(R.string.library_by, nextTextConfig = TextConfig(albumArtistName, TextConfigStyle.BOLD))
    )
}

internal fun Genre.toFilterItem(
    parentFilter: Filter<*>?,
    filtersManager: FiltersManager
): FilterItem {
    val filter = Filter(Filter.FilterType.GENRE_IS, id, name)
    val filterInHierarchy = parentFilter.withChild(filter)
    return FilterItem(
        id,
        TextConfig(name),
        filtersManager.isFilterInEdition(filterInHierarchy)
    )
}

internal fun Playlist.toFilterItem(
    parentFilter: Filter<*>?,
    urlRepository: UrlRepository,
    filtersManager: FiltersManager
): FilterItem {
    val artUrl = urlRepository.getPlaylistArtUrl(id)
    val filter = Filter(Filter.FilterType.PLAYLIST_IS, id, name)
    val filterInHierarchy = parentFilter.withChild(filter)
    return FilterItem(
        id,
        TextConfig(name),
        filtersManager.isFilterInEdition(filterInHierarchy),
        artUrl
    )
}

internal fun PlaylistWithCount.toFilterItem(
    parentFilter: Filter<*>?,
    urlRepository: UrlRepository,
    filtersManager: FiltersManager
): FilterItem {
    val artUrl = urlRepository.getPlaylistArtUrl(id)
    val filter = Filter(Filter.FilterType.PLAYLIST_IS, id, name)
    val filterInHierarchy = parentFilter.withChild(filter)
    return FilterItem(
        id,
        TextConfig(name),
        filtersManager.isFilterInEdition(filterInHierarchy),
        artUrl
    )
}

internal fun DownloadedCount.toFilterItem(
    parentFilter: Filter<*>?,
    filtersManager: FiltersManager,
    isDownloadedName: String,
    isNotDownloadedName: String,
): FilterItem {
    val name = if (isDownloaded) {
        isDownloadedName
    } else {
        isNotDownloadedName
    } + count
    val filter = Filter(Filter.FilterType.DOWNLOADED_STATUS_IS, isDownloaded, name)
    val filterInHierarchy = parentFilter.withChild(filter)
    val id = if (isDownloaded) 1L else 0L
    return FilterItem(
        id,
        TextConfig(name),
        filtersManager.isFilterInEdition(filterInHierarchy)
    )
}

internal fun SongDisplayDomain.toIdText() = IdText(id, title)

internal fun Artist.toIdText() = IdText(id, name)

internal fun Album.toIdText() = IdText(id, name)

internal fun Genre.toIdText() = IdText(id, name)

internal fun PlaylistWithCount.toIdText() = IdText(id, name)

private fun Filter<*>?.withChild(filter: Filter<*>): Filter<*> {
    if (this == null) {
        return filter
    }
    val copy = deepCopy()
    copy.addToDeepestChild(filter)
    return copy
}