package be.florien.anyflow.feature.player.songlist

import android.content.ContentResolver
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import be.florien.anyflow.R
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.server.AmpacheConnection
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.player.FiltersManager
import be.florien.anyflow.player.OrderComposer
import java.net.URL

class SongInfoOptions constructor(
    private val songId: Long = 0L,
    private val contentResolver: ContentResolver,
    private val ampache: AmpacheConnection,
    private val filtersManager: FiltersManager,
    private val orderComposer: OrderComposer,
    private val dataRepository: DataRepository

) {
    lateinit var songInfo: SongInfo

    suspend fun initSongInfo() {
        songInfo = dataRepository.getSongById(songId) ?: throw IllegalArgumentException("No song for ID")
    }

    suspend fun playNext() {
        orderComposer.changeSongPositionForNext(songId)
    }

    suspend fun filterOn(fieldType: FieldType) {
        val filter = when (fieldType) {
            FieldType.TITLE -> Filter.SongIs(songInfo.id, songInfo.title, songInfo.art)
            FieldType.ARTIST -> Filter.ArtistIs(songInfo.artistId, songInfo.artistName, null)
            FieldType.ALBUM -> Filter.AlbumIs(songInfo.albumId, songInfo.albumName, songInfo.art)
            FieldType.ALBUM_ARTIST -> Filter.AlbumArtistIs(songInfo.albumArtistId, songInfo.albumArtistName, null)
            FieldType.GENRE -> Filter.GenreIs(songInfo.genre)
            else -> throw IllegalArgumentException("This field can't be filtered on")
        }
        filtersManager.clearFilters()
        filtersManager.addFilter(filter)
        filtersManager.commitChanges()
    }

    fun getSearchTerms(fieldType: FieldType): String = when (fieldType) {
        FieldType.TITLE ->  songInfo.title
        FieldType.ARTIST ->  songInfo.artistName
        FieldType.ALBUM ->  songInfo.albumName
        FieldType.ALBUM_ARTIST ->  songInfo.albumArtistName
        FieldType.GENRE ->  songInfo.genre
        else -> throw IllegalArgumentException("This field can't be searched on")
    }

    suspend fun download() {
        val audioCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val newSongDetails = getNewSongDetails()

        val newSongUri = contentResolver.insert(audioCollection, newSongDetails) ?: return

        kotlin.runCatching {
            val songUrl = ampache.getSongUrl(songId)
            URL(songUrl).openStream().use { input ->
                contentResolver.openOutputStream(newSongUri)?.use { output ->
                    input.copyTo(output)
                }
            }
        }
        dataRepository.updateSongLocalUri(songId, newSongUri.toString())
        initSongInfo()
    }

    private fun getNewSongDetails(): ContentValues = ContentValues().apply {
        put(MediaStore.Audio.Media.TITLE, songInfo.title)
        put(MediaStore.Audio.Media.ARTIST, songInfo.artistName)
        put(MediaStore.Audio.Media.ARTIST_ID, songInfo.artistId)
        put(MediaStore.Audio.Media.ALBUM, songInfo.albumName)
        put(MediaStore.Audio.Media.ALBUM_ID, songInfo.albumId)
        put(MediaStore.Audio.Media.TRACK, songInfo.track)
        put(MediaStore.Audio.Media.DURATION, songInfo.time)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            put(MediaStore.Audio.Media.ALBUM_ARTIST, songInfo.albumArtistName)
            put(MediaStore.Audio.Media.GENRE, songInfo.genre)
            put(MediaStore.Audio.Media.YEAR, songInfo.year)
        }
    }

    fun getInfoRows() : List<SongRow> = listOf(
        SongRow(R.string.info_title, songInfo.title, null, R.drawable.ic_next_occurence, FieldType.TITLE, ActionType.EXPAND_TITLE),
        SongRow(R.string.info_artist, songInfo.artistName, null, R.drawable.ic_next_occurence, FieldType.ARTIST, ActionType.EXPAND_TITLE),
        SongRow(R.string.info_track, songInfo.track.toString(), null, 0, FieldType.TRACK, ActionType.INFO_TITLE),
        SongRow(R.string.info_album, songInfo.albumName, null, R.drawable.ic_next_occurence, FieldType.ALBUM, ActionType.EXPAND_TITLE),
        SongRow(R.string.info_album_artist, songInfo.albumArtistName, null, R.drawable.ic_next_occurence, FieldType.ALBUM_ARTIST, ActionType.EXPAND_TITLE),
        SongRow(R.string.info_genre, songInfo.genre, null, R.drawable.ic_next_occurence, FieldType.GENRE, ActionType.EXPAND_TITLE),
        SongRow(R.string.info_duration, songInfo.timeText, null, 0, FieldType.DURATION, ActionType.INFO_TITLE),
        SongRow(R.string.info_year, songInfo.year.toString(), null, 0, FieldType.YEAR, ActionType.INFO_TITLE)
    )

    fun getOptionsRows(fieldType: FieldType): List<SongRow> = when (fieldType) {
        FieldType.TITLE -> listOf(
            SongRow(R.string.info_option_next_title, null, R.string.info_option_track_next, R.drawable.ic_add, fieldType, ActionType.ADD_NEXT),
            SongRow(R.string.info_option_add_to_playlist, null, R.string.info_option_add_to_playlist_detail, R.drawable.ic_add_to_playlist, fieldType, ActionType.ADD_TO_PLAYLIST),
            SongRow(R.string.info_option_filter_title, songInfo.title, R.string.info_option_filter_on, R.drawable.ic_filter, fieldType, ActionType.ADD_TO_FILTER),
            SongRow(R.string.info_option_search_title, songInfo.title, R.string.info_option_search_on, R.drawable.ic_search, fieldType, ActionType.SEARCH),
            if (songInfo.local == null) {
                SongRow(R.string.info_option_download, null, R.string.info_option_download_description, R.drawable.ic_download, fieldType, ActionType.DOWNLOAD)
            } else {
                SongRow(R.string.info_option_downloaded, null, R.string.info_option_downloaded_description, R.drawable.ic_downloaded, fieldType, ActionType.NONE)
            }
        )
        FieldType.ARTIST -> listOf(
            SongRow(R.string.info_option_filter_title, songInfo.artistName, R.string.info_option_filter_on, R.drawable.ic_filter, fieldType, ActionType.ADD_TO_FILTER),
            SongRow(R.string.info_option_search_title, songInfo.artistName, R.string.info_option_search_on, R.drawable.ic_search, fieldType, ActionType.SEARCH)
        )
        FieldType.ALBUM -> listOf(
            SongRow(R.string.info_option_filter_title, songInfo.albumName, R.string.info_option_filter_on, R.drawable.ic_filter, fieldType, ActionType.ADD_TO_FILTER),
            SongRow(R.string.info_option_search_title, songInfo.albumName, R.string.info_option_search_on, R.drawable.ic_search, fieldType, ActionType.SEARCH)
        )
        FieldType.ALBUM_ARTIST -> listOf(
            SongRow(R.string.info_option_filter_title, songInfo.albumArtistName, R.string.info_option_filter_on, R.drawable.ic_filter, fieldType, ActionType.ADD_TO_FILTER),
            SongRow(R.string.info_option_search_title, songInfo.albumArtistName, R.string.info_option_search_on, R.drawable.ic_search, fieldType, ActionType.SEARCH)
        )
        FieldType.GENRE -> listOf(
            SongRow(R.string.info_option_filter_title, songInfo.genre, R.string.info_option_filter_on, R.drawable.ic_filter, fieldType, ActionType.ADD_TO_FILTER),
            SongRow(R.string.info_option_search_title, songInfo.genre, R.string.info_option_search_on, R.drawable.ic_search, fieldType, ActionType.SEARCH)
        )
        else -> listOf()
    }

    class SongRow(@StringRes val title: Int, val text: String?, @StringRes val textRes: Int?, @DrawableRes val icon: Int, val fieldType: FieldType, val actionType: ActionType)

    enum class FieldType {
        TITLE, TRACK, ARTIST, ALBUM, ALBUM_ARTIST, GENRE, YEAR, DURATION
    }

    enum class ActionType {
        NONE, INFO_TITLE, EXPAND_TITLE, ADD_TO_FILTER, ADD_TO_PLAYLIST, ADD_NEXT, SEARCH, DOWNLOAD
    }
}