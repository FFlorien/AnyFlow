package be.florien.anyflow.feature.library.tags.domain

import be.florien.anyflow.common.ui.domain.TextConfig
import be.florien.anyflow.common.utils.TimeOperations
import be.florien.anyflow.feature.library.domain.LibraryInfoRepository
import be.florien.anyflow.feature.library.domain.model.IdText
import be.florien.anyflow.feature.library.domain.model.LibraryRowType
import be.florien.anyflow.feature.library.domain.model.LibraryFieldType
import be.florien.anyflow.feature.library.domain.model.LibraryInfoRow
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
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class LibraryInfoTagsRepository @Inject constructor(
    private val urlRepository: UrlRepository,
    private val tagsRepository: TagsRepository,
    private val playlistRepository: PlaylistRepository
) : LibraryInfoRepository {
    override fun getArtUrl(artType: String?, id: Long): String? =
        if (artType == null) {
            null
        } else {
            urlRepository.getArtUrl(artType, id)
        }

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
        val filteredInfo =
            withContext(Dispatchers.IO) { tagsRepository.getFilteredInfo(filter) }
        return mutableListOf(
            LibraryInfoRow(
                LibraryFieldType.Tags.Duration,
                LibraryRowType.InfoTitle,
                TextConfig(
                    TimeOperations.toMediaDuration(
                        filteredInfo.duration.toDuration(DurationUnit.SECONDS)
                    )
                )

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
                TextConfig(text = filteredInfo.genres.toString())
            ),
            LibraryInfoRow(
                LibraryFieldType.Tags.AlbumArtist,
                getAction(filteredInfo.albumArtists),
                TextConfig(text = filteredInfo.albumArtists.toString())
            ),
            LibraryInfoRow(
                LibraryFieldType.Tags.Album,
                getAction(filteredInfo.albums),
                TextConfig(text = filteredInfo.albums.toString())
            ),
            LibraryInfoRow(
                LibraryFieldType.Tags.Artist,
                getAction(filteredInfo.artists),
                TextConfig(text = filteredInfo.artists.toString())
            ),
            LibraryInfoRow(
                LibraryFieldType.Tags.Song,
                getAction(filteredInfo.songs),
                TextConfig(text = filteredInfo.songs.toString())
            ),
            LibraryInfoRow(
                LibraryFieldType.Tags.Downloaded,
                LibraryRowType.SubFilter,
                TextConfig(text = filteredInfo.downloaded.toString())
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
                TextConfig(text = filteredInfo.playlists.toString())
            )
        )
    }

    override suspend fun getActionList(fieldType: LibraryFieldType): List<LibraryInfoRow> {
        if (fieldType !is LibraryFieldType.Tags) {
            return emptyList()
        }

        return when (fieldType) {
            LibraryFieldType.Tags.Duration -> emptyList()
            LibraryFieldType.Tags.Genre,
            LibraryFieldType.Tags.AlbumArtist,
            LibraryFieldType.Tags.Album,
            LibraryFieldType.Tags.Artist,
            LibraryFieldType.Tags.Song,
            LibraryFieldType.Tags.Playlist,
            LibraryFieldType.Tags.Downloaded -> listOf(
                LibraryInfoRow(
                    fieldType = fieldType,
                    actionType = LibraryRowType.AddToPlaylist,
                    infoText = TextConfig(textRes = R.string.info_action_select_playlist_detail)
                ),
                LibraryInfoRow(
                    fieldType = fieldType,
                    actionType = LibraryRowType.AddToFilter,
                    infoText = TextConfig(textRes = R.string.info_action_filter_title)
                ),
                LibraryInfoRow(
                    fieldType = fieldType,
                    actionType = LibraryRowType.AddNext,
                    infoText = TextConfig(textRes = R.string.info_action_next_title)
                ),
                LibraryInfoRow(
                    fieldType = fieldType,
                    actionType = LibraryRowType.Search,
                    infoText = TextConfig(textRes = R.string.info_action_search_title)
                ),
                LibraryInfoRow(
                    fieldType = fieldType,
                    actionType = LibraryRowType.Download,
                    infoText = TextConfig(textRes = R.string.info_action_download)
                )
            )
        }
    }


    private fun getAction(count: Int): LibraryRowType {
        return if (count > 1) LibraryRowType.SubFilter else LibraryRowType.ExpandableTitle
    }
}