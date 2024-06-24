package be.florien.anyflow.feature.player.ui.info.library

import android.content.Context
import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import be.florien.anyflow.R
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.PodcastRepository
import be.florien.anyflow.data.UrlRepository
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.feature.player.ui.info.InfoActions
import be.florien.anyflow.feature.playlist.PlaylistRepository
import be.florien.anyflow.feature.sync.SyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LibraryInfoActions @Inject constructor(
    private val dataRepository: DataRepository,
    private val playlistRepository: PlaylistRepository,
    private val podcastRepository: PodcastRepository,
    private val urlRepository: UrlRepository,
    context: Context
) : InfoActions<Filter<*>?>() {

    private val resources: Resources = context.resources

    override suspend fun getInfoRows(infoSource: Filter<*>?): List<InfoRow> {
        fun Int?.getDurationString(resource: Int) =
            this?.let { resources.getQuantityString(resource, it, it) } ?: ""

        val filteredInfo =
            withContext(Dispatchers.IO) { dataRepository.getFilteredInfo(infoSource) }
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
                LibraryFieldType.Duration,
                LibraryActionType.InfoTitle,
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
                2 //todo number of downloaded/not downloaded
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
            ),
            getInfoRow(
                R.string.filter_info_podcast_episodes,
                infoSource,
                Filter.FilterType.PODCAST_EPISODE_IS,
                100 //todo number of podcast episodes + move to another screen
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
    ): InfoRow {
        val displayData: DisplayData? = if (count <= 1) {
            val filterIfTypePresent = filter?.getFilterIfTypePresent(filterType)
            val filterData: DisplayData? =
                filterIfTypePresent?.let { DisplayData(it.displayText, it.argument as Long) }
            filterData ?: when (filterType) {
                Filter.FilterType.SONG_IS ->
                    dataRepository.getSongsSearchedList(listOfNotNull(filter), "") {
                        DisplayData(it.title, it.id)
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
                    playlistRepository.getPlaylistsSearchedList(listOfNotNull(filter), "") {
                        DisplayData(it.name, it.id)
                    }

                Filter.FilterType.DOWNLOADED_STATUS_IS,
                Filter.FilterType.DISK_IS -> listOf(null)

                Filter.FilterType.PODCAST_EPISODE_IS ->
                    podcastRepository.getAllPodcastsEpisodesList {
                        DisplayData(it.title, it.id)
                    }
            }.firstOrNull()
        } else null

        val url = if (count <= 1 && displayData != null) {
            when (filterType.artType) {
                SyncRepository.ART_TYPE_PLAYLIST -> urlRepository.getPlaylistArtUrl(displayData.argument)
                SyncRepository.ART_TYPE_ARTIST -> urlRepository.getArtistArtUrl(displayData.argument)
                SyncRepository.ART_TYPE_SONG -> urlRepository.getSongArtUrl(displayData.argument)
                SyncRepository.ART_TYPE_ALBUM -> urlRepository.getAlbumArtUrl(displayData.argument)
                SyncRepository.ART_TYPE_PODCAST -> urlRepository.getPodcastArtUrl(displayData.argument)
                else -> null
            }
        } else {
            null
        }

        return LibraryInfoRow(
            title,
            getDisplayText(displayData, count),
            null,
            getField(filterType),
            getAction(count),
            url
        )
    }

    private fun getDisplayText(
        filter: DisplayData?,
        count: Int
    ): String {
        return if (count <= 1 && filter != null) {
            filter.text
        } else {
            count.toString()
        }
    }

    private fun getField(
        filterType: Filter.FilterType
    ): LibraryFieldType {
        return when (filterType) {
            Filter.FilterType.SONG_IS -> LibraryFieldType.Song
            Filter.FilterType.ARTIST_IS -> LibraryFieldType.Artist
            Filter.FilterType.ALBUM_ARTIST_IS -> LibraryFieldType.AlbumArtist
            Filter.FilterType.ALBUM_IS -> LibraryFieldType.Album
            Filter.FilterType.PLAYLIST_IS -> LibraryFieldType.Playlist
            Filter.FilterType.DOWNLOADED_STATUS_IS -> LibraryFieldType.Downloaded
            Filter.FilterType.PODCAST_EPISODE_IS -> LibraryFieldType.PodcastEpisode
            else -> LibraryFieldType.Genre
        }
    }

    private fun getAction(count: Int): ActionType {
        return if (count > 1) LibraryActionType.SubFilter else LibraryActionType.InfoTitle
    }

    class DisplayData(val text: String, val argument: Long)

    enum class LibraryFieldType(
        @DrawableRes override val iconRes: Int
    ) : FieldType {
        Duration(R.drawable.ic_duration),
        Genre(R.drawable.ic_genre),
        AlbumArtist(R.drawable.ic_album_artist),
        Album(R.drawable.ic_album),
        Artist(R.drawable.ic_artist),
        Song(R.drawable.ic_song),
        Playlist(R.drawable.ic_playlist),
        Downloaded(R.drawable.ic_downloaded),
        PodcastEpisode(R.drawable.ic_podcast_episode);
    }

    enum class LibraryActionType(
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