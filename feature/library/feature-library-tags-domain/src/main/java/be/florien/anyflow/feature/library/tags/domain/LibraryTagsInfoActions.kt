package be.florien.anyflow.feature.library.tags.domain

import android.content.Context
import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import be.florien.anyflow.common.ui.data.info.InfoActions
import be.florien.anyflow.management.filters.model.Filter
import be.florien.anyflow.resources.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LibraryTagsInfoActions @Inject constructor( //todo highly abstractable
    private val libraryTagsRepository: LibraryTagsRepository,
    context: Context
) : InfoActions<Filter<*>?>() {

    private val resources: Resources = context.resources

    override suspend fun getInfoRows(infoSource: Filter<*>?): List<InfoRow> {
        fun Int?.getDurationString(resource: Int) =
            this?.let { resources.getQuantityString(resource, it, it) } ?: ""

        val filteredInfo =
            withContext(Dispatchers.IO) { libraryTagsRepository.getFilteredInfo(infoSource) }
        return listOfNotNull(
            LibraryInfoRow(
                R.string.filter_info_duration,
                filteredInfo.duration.toComponents { days, hours, minutes, seconds, _ ->
                    val d = days.toInt().takeIf { it > 0 }
                    val h = hours.takeIf { it > 0 || days > 0 }
                    val m = minutes.takeIf { it > 0 || days > 0 || hours > 0 }
                    val s = seconds.takeIf { it > 0 || days > 0 || hours > 0 || minutes > 0 }
                    d.getDurationString(R.plurals.days_component) +
                            h.getDurationString(R.plurals.hours_component) +
                            m.getDurationString(R.plurals.minutes_component) +
                            s.getDurationString(R.plurals.seconds_component)
                },
                null,
                LibraryTagsFieldType.Duration,
                LibraryPodcastActionType.InfoTitle,
                null
            ),
            getInfoRow(
                R.string.filter_info_genre,
                infoSource,
                Filter.FilterType.GENRE_IS,
                filteredInfo.genres.minus(infoSource?.let { source ->
                    val genreFilters = mutableSetOf<Long>()
                    source.traversal { filter ->
                        if (filter.type == Filter.FilterType.GENRE_IS) {
                            genreFilters.add(filter.argument as Long)
                        }
                    }
                    genreFilters.size
                } ?: 0) // todo "Electro and 3 other genres" instead
            ),
            getInfoRow(
                R.string.filter_info_album_artist,
                infoSource,
                Filter.FilterType.ALBUM_ARTIST_IS,
                filteredInfo.albumArtists
            ),
            getInfoRow(
                R.string.filter_info_album,
                infoSource,
                Filter.FilterType.ALBUM_IS,
                filteredInfo.albums
            ),
            getInfoRow(
                R.string.filter_info_artist,
                infoSource,
                Filter.FilterType.ARTIST_IS,
                filteredInfo.artists
            ),
            getInfoRow(
                R.string.filter_info_song,
                infoSource,
                Filter.FilterType.SONG_IS,
                filteredInfo.songs
            ),
            getInfoRow(
                R.string.filter_info_downloaded,
                infoSource,
                Filter.FilterType.DOWNLOADED_STATUS_IS,
                filteredInfo.downloaded,
                "${filteredInfo.downloaded}/${filteredInfo.songs}"
            ),
            getInfoRow(
                R.string.filter_info_playlist,
                infoSource,
                Filter.FilterType.PLAYLIST_IS,
                filteredInfo.playlists.minus(infoSource?.let { source ->
                    val playlistFilters = mutableSetOf<Long>()
                    source.traversal { filter ->
                        if (filter.type == Filter.FilterType.PLAYLIST_IS) {
                            playlistFilters.add(filter.argument as Long)
                        }
                    }
                    playlistFilters.size
                } ?: 0) // todo "Motivation and 3 other playlists" instead
            )
        )
    }

    override suspend fun getActionsRows(
        infoSource: Filter<*>?,
        row: InfoRow
    ): List<InfoRow> = emptyList()

    private suspend fun getInfoRow(
        @StringRes title: Int,
        filter: Filter<*>?,
        filterType: Filter.FilterType,
        count: Int
    ): InfoRow =
        getInfoRow(
            title,
            filter,
            filterType,
            count,
            count.toString()
        )

    private suspend fun getInfoRow(
        @StringRes title: Int,
        filter: Filter<*>?,
        filterType: Filter.FilterType,
        count: Int,
        subTitle: String
    ): InfoRow {
        val displayData: String? = if (count <= 1) {
            val filterIfTypePresent = filter?.getFilterIfTypePresent(filterType)
            val filterData: String? = filterIfTypePresent?.displayText
            filterData ?: when (filterType) { //todo separate podcast & tags
                Filter.FilterType.SONG_IS -> libraryTagsRepository.getSongFiltered(filter)
                Filter.FilterType.ARTIST_IS -> libraryTagsRepository.getArtistFiltered(filter)
                Filter.FilterType.ALBUM_ARTIST_IS -> libraryTagsRepository.getAlbumArtistFiltered(filter)
                Filter.FilterType.ALBUM_IS -> libraryTagsRepository.getAlbumFiltered(filter)
                Filter.FilterType.GENRE_IS -> libraryTagsRepository.getGenreFiltered(filter)
                Filter.FilterType.PLAYLIST_IS -> libraryTagsRepository.getPlaylistFiltered(filter)
                Filter.FilterType.DOWNLOADED_STATUS_IS,
                Filter.FilterType.DISK_IS -> listOf(null)

                Filter.FilterType.PODCAST_EPISODE_IS -> listOf(null)
            }.firstOrNull()
        } else null

        val url =
            if (count <= 1 && filter != null && filter.argument is Long) {
                libraryTagsRepository.getArtUrl(filter.type.artType, filter.argument as Long)
            } else {
                null
            }

        return LibraryInfoRow(
            title,
            getDisplayText(displayData, count, subTitle),
            null,
            getField(filterType),
            getAction(count),
            url
        )
    }

    private fun getDisplayText(
        text: String?,
        count: Int,
        subTitle: String
    ): String {
        return if (count <= 1 && text != null) {
            text
        } else {
            subTitle
        }
    }

    private fun getField(
        filterType: Filter.FilterType
    ): LibraryTagsFieldType {
        return when (filterType) {
            Filter.FilterType.SONG_IS -> LibraryTagsFieldType.Song
            Filter.FilterType.ARTIST_IS -> LibraryTagsFieldType.Artist
            Filter.FilterType.ALBUM_ARTIST_IS -> LibraryTagsFieldType.AlbumArtist
            Filter.FilterType.ALBUM_IS -> LibraryTagsFieldType.Album
            Filter.FilterType.PLAYLIST_IS -> LibraryTagsFieldType.Playlist
            Filter.FilterType.DOWNLOADED_STATUS_IS -> LibraryTagsFieldType.Downloaded
            else -> LibraryTagsFieldType.Genre
        }
    }

    private fun getAction(count: Int): ActionType {
        return if (count > 1) LibraryPodcastActionType.SubFilter else LibraryPodcastActionType.InfoTitle
    }

    enum class LibraryTagsFieldType(
        @DrawableRes override val iconRes: Int
    ) : FieldType {
        Duration(R.drawable.ic_duration),
        Genre(R.drawable.ic_genre),
        AlbumArtist(R.drawable.ic_album_artist),
        Album(R.drawable.ic_album),
        Artist(R.drawable.ic_artist),
        Song(R.drawable.ic_song),
        Playlist(R.drawable.ic_playlist),
        Downloaded(R.drawable.ic_downloaded);
    }

    enum class LibraryPodcastActionType(
        @DrawableRes override val iconRes: Int,
        override val category: ActionTypeCategory
    ) : ActionType {
        SubFilter(R.drawable.ic_go, ActionTypeCategory.Navigation),
        InfoTitle(0, ActionTypeCategory.None);
    }

    data class LibraryInfoRow(
        @StringRes override val title: Int,
        override val text: String?,
        @StringRes override val textRes: Int?,
        override val fieldType: FieldType,
        override val actionType: ActionType,
        override val imageUrl: String?
    ) : InfoRow(title, text, textRes, fieldType, actionType, imageUrl)
}