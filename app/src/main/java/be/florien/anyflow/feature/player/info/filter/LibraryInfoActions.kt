package be.florien.anyflow.feature.player.info.filter

import android.content.Context
import android.content.res.Resources
import androidx.annotation.StringRes
import be.florien.anyflow.R
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.feature.player.info.InfoActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LibraryInfoActions @Inject constructor(
    private val dataRepository: DataRepository,
    context: Context
) : InfoActions<Filter<*>?>() {

    private val resources: Resources = context.resources

    override suspend fun getInfoRows(infoSource: Filter<*>?): List<InfoRow> {
        fun Int?.getDurationString(resource: Int) =
            this?.let { resources.getQuantityString(resource, it, it) } ?: ""

        val filteredInfo =
            withContext(Dispatchers.IO) { dataRepository.getFilteredInfo(infoSource) }
        return listOfNotNull(
            InfoRow(
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
                LibraryFieldType.Duration(),
                ActionType.InfoTitle()
            ),
            getInfoRow(
                R.string.filter_info_genre,
                infoSource,
                Filter.FilterType.GENRE_IS,
                filteredInfo.genres
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
                R.string.filter_info_playlist,
                infoSource,
                Filter.FilterType.PLAYLIST_IS,
                filteredInfo.playlists
            )
        )
    }

    override suspend fun getActionsRows(
        infoSource: Filter<*>?,
        fieldType: FieldType
    ): List<InfoRow> = emptyList()

    private suspend fun getInfoRow(
        @StringRes title: Int,
        filter: Filter<*>?,
        filterType: Filter.FilterType,
        count: Int
    ): InfoRow {
        val displayData: DisplayData? = if (count == 1) {
            val filterIfTypePresent = filter?.getFilterIfTypePresent(filterType)
            val filterData: DisplayData? =
                filterIfTypePresent?.let { DisplayData(it.displayText, it.argument as Long) }
            filterData
                ?: when (filterType) {
                    Filter.FilterType.SONG_IS ->
                        dataRepository.getSongsSearchedList(listOfNotNull(filter), "") {
                            DisplayData(it.song.title, it.song.id)
                        }
                    Filter.FilterType.ARTIST_IS ->
                        dataRepository.getArtistsSearchedList(listOfNotNull(filter), "") {
                            DisplayData(it.name, it.id)
                        }
                    Filter.FilterType.ALBUM_ARTIST_IS ->
                        dataRepository.getAlbumArtistsSearchedList(listOfNotNull(filter), "") {
                            DisplayData(it.name, it.id)
                        }
                    Filter.FilterType.ALBUM_IS ->
                        dataRepository.getAlbumsSearchedList(listOfNotNull(filter), "") {
                            DisplayData(it.albumName, it.albumId)
                        }
                    Filter.FilterType.GENRE_IS ->
                        dataRepository.getGenresSearchedList(listOfNotNull(filter), "") {
                            DisplayData(it.name, it.id)
                        }
                    Filter.FilterType.PLAYLIST_IS ->
                        dataRepository.getPlaylistsSearchedList(listOfNotNull(filter), "") {
                            DisplayData(it.name, it.id)
                        }
                    Filter.FilterType.DOWNLOADED_STATUS_IS -> listOf(null)
                }.firstOrNull()
        } else null

        return InfoRow(
            title,
            getDisplayText(displayData, count),
            null,
            getField(displayData, filterType, count),
            getAction(count)
        )
    }

    private fun getDisplayText(
        filter: DisplayData?,
        count: Int
    ): String {
        return if (count == 1 && filter != null) {
            filter.text
        } else {
            count.toString()
        }
    }

    private fun getField(
        data: DisplayData?,
        filterType: Filter.FilterType,
        count: Int
    ): LibraryFieldType {
        val url = if (count == 1 && data != null) {
            when (filterType) {
                Filter.FilterType.PLAYLIST_IS -> dataRepository.getPlaylistArtUrl(data.argument)
                Filter.FilterType.ALBUM_ARTIST_IS,
                Filter.FilterType.ARTIST_IS -> dataRepository.getArtistArtUrl(data.argument)
                Filter.FilterType.SONG_IS -> dataRepository.getSongUrl(data.argument)
                Filter.FilterType.ALBUM_IS -> dataRepository.getAlbumArtUrl(data.argument)
                else -> null
            }
        } else {
            null
        }
        return when (filterType) {
            Filter.FilterType.SONG_IS -> LibraryFieldType.Song(url)
            Filter.FilterType.ARTIST_IS -> LibraryFieldType.Artist(url)
            Filter.FilterType.ALBUM_ARTIST_IS -> LibraryFieldType.AlbumArtist(url)
            Filter.FilterType.ALBUM_IS -> LibraryFieldType.Album(url)
            Filter.FilterType.PLAYLIST_IS -> LibraryFieldType.Playlist(url)
            else -> LibraryFieldType.Genre()
        }
    }

    private fun getAction(count: Int): ActionType {
        return if (count > 1) LibraryActionType.SubFilter() else ActionType.InfoTitle()
    }

    class DisplayData(val text: String, val argument: Long)
}