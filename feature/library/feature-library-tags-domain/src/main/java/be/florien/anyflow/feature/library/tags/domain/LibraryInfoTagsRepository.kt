package be.florien.anyflow.feature.library.tags.domain

import be.florien.anyflow.feature.library.domain.LibraryInfoRepository
import be.florien.anyflow.feature.library.domain.model.IdText
import be.florien.anyflow.feature.library.domain.model.LibraryFieldType
import be.florien.anyflow.feature.library.domain.model.LibraryInfoRow
import be.florien.anyflow.feature.library.domain.model.LibraryRowType
import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.management.filters.domain.model.FilterType
import be.florien.anyflow.management.filters.domain.model.PodcastFilterType
import be.florien.anyflow.management.filters.domain.model.TagFilterType
import be.florien.anyflow.management.playlist.PlaylistRepository
import be.florien.anyflow.management.playlist.model.PlaylistWithCount
import be.florien.anyflow.tags.TagsRepository
import be.florien.anyflow.tags.model.Album
import be.florien.anyflow.tags.model.Artist
import be.florien.anyflow.tags.model.Genre
import be.florien.anyflow.tags.model.SongDisplayDomain
import be.florien.anyflow.urls.UrlRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LibraryInfoTagsRepository @Inject constructor(
    private val urlRepository: UrlRepository,
    private val tagsRepository: TagsRepository,
    private val playlistRepository: PlaylistRepository
) : LibraryInfoRepository {
    override fun getArtUrl(artType: String?, id: Long): String? =
        artType?.let { urlRepository.getArtUrl(it, id) }

    override suspend fun getFilteredInfo(
        filterType: FilterType,
        filter: Filter?
    ): IdText? = when (filterType) {
        TagFilterType.SONG_IS ->
            tagsRepository
                .getSongFiltered(filter, "")
                .map(SongDisplayDomain::toIdText)

        TagFilterType.ARTIST_IS ->
            tagsRepository
                .getArtistFiltered(filter, "")
                .map(Artist::toIdText)

        TagFilterType.ALBUM_ARTIST_IS ->
            tagsRepository
                .getAlbumArtistFiltered(filter, "")
                .map(Artist::toIdText)

        TagFilterType.ALBUM_IS ->
            tagsRepository
                .getAlbumFiltered(filter, "")
                .map(Album::toIdText)

        TagFilterType.GENRE_IS ->
            tagsRepository
                .getGenreFiltered(filter, "")
                .map(Genre::toIdText)

        TagFilterType.PLAYLIST_IS ->
            playlistRepository
                .getPlaylistFiltered(filter, "")
                .map(PlaylistWithCount::toIdText)

        TagFilterType.DOWNLOADED_STATUS_IS,
        TagFilterType.DISK_IS,
        PodcastFilterType.PODCAST_EPISODE_IS,
        PodcastFilterType.PODCAST_IS,
        PodcastFilterType.STATE_IS -> listOf(null)
    }.firstOrNull()

    override suspend fun getInfoRowList(filter: Filter?): MutableList<LibraryInfoRow> {
        val filteredInfo = withContext(Dispatchers.IO) {
            tagsRepository.getFilteredInfo(filter)
        }
        return mutableListOf(
            LibraryInfoRow(
                LibraryFieldType.Tags.Duration,
                LibraryRowType.MultiRow.InfoTitle,
                filteredInfo.duration
            ),
            LibraryInfoRow(
                LibraryFieldType.Tags.Genre,
                getAction(filteredInfo.genres.minus(filter?.let { source ->
                    val genreFilters = mutableSetOf<Long>()
                    source.forEach { filter ->
                        if (filter.type == TagFilterType.GENRE_IS) {
                            genreFilters.add(filter.argument as Long)
                        }
                    }
                    genreFilters.size
                } ?: 0)), // todo "Electro and 3 other genres" instead
                filteredInfo.genres
            ),
            LibraryInfoRow(
                LibraryFieldType.Tags.AlbumArtist,
                getAction(filteredInfo.albumArtists),
                filteredInfo.albumArtists
            ),
            LibraryInfoRow(
                LibraryFieldType.Tags.Album,
                getAction(filteredInfo.albums),
                filteredInfo.albums
            ),
            LibraryInfoRow(
                LibraryFieldType.Tags.Artist,
                getAction(filteredInfo.artists),
                filteredInfo.artists
            ),
            LibraryInfoRow(
                LibraryFieldType.Tags.Song,
                getAction(filteredInfo.songs),
                filteredInfo.songs
            ),
            LibraryInfoRow(
                LibraryFieldType.Tags.Downloaded,
                LibraryRowType.MultiRow.SubFilter,
                filteredInfo.downloaded
            ),
            LibraryInfoRow(
                LibraryFieldType.Tags.Playlist,
                getAction(filteredInfo.playlists.minus(filter?.let { source ->
                    val playlistFilters = mutableSetOf<Long>()
                    source.forEach { filter ->
                        if (filter.type == TagFilterType.PLAYLIST_IS) {
                            playlistFilters.add(filter.argument as Long)
                        }
                    }
                    playlistFilters.size
                } ?: 0)), // todo "Motivation and 3 other playlists" instead
                filteredInfo.playlists
            )
        )
    }

    override suspend fun getActionList(fieldType: LibraryFieldType) = when (fieldType) {
        !is LibraryFieldType.Tags,
        LibraryFieldType.Tags.Duration -> emptyList()

        is LibraryFieldType.Tags -> LibraryRowType.Action.entries.map {
            LibraryInfoRow(
                fieldType = fieldType,
                rowType = it,
                count = 1
            )
        }
    }

    private fun getAction(count: Int): LibraryRowType = if (count > 1) {
        LibraryRowType.MultiRow.SubFilter
    } else {
        LibraryRowType.SingleRow.ExpandableTitle
    }
}