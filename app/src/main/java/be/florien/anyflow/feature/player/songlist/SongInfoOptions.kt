package be.florien.anyflow.feature.player.songlist

import android.content.ContentResolver
import android.content.ContentValues
import android.content.SharedPreferences
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
    private val contentResolver: ContentResolver,
    private val ampache: AmpacheConnection,
    private val filtersManager: FiltersManager,
    private val orderComposer: OrderComposer,
    private val dataRepository: DataRepository,
    private val sharedPreferences: SharedPreferences
) {
    private var lastSongInfo: SongInfo? = null

    private suspend fun initSongInfo(songId: Long) {
        lastSongInfo = dataRepository.getSongById(songId) ?: throw IllegalArgumentException("No song for ID")
    }

    suspend fun getSongInfo(songId: Long): SongInfo {
        if (lastSongInfo == null || lastSongInfo?.id != songId) {
            initSongInfo(songId)
        }
        return lastSongInfo as SongInfo
    }

    suspend fun playNext(songId: Long) {
        orderComposer.changeSongPositionForNext(songId)
    }

    suspend fun filterOn(songId: Long, fieldType: FieldType) {
        val songInfo = getSongInfo(songId)
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

    suspend fun getSearchTerms(songId: Long, fieldType: FieldType): String {
        val songInfo = getSongInfo(songId)
        return when (fieldType) {
            FieldType.TITLE -> songInfo.title
            FieldType.ARTIST -> songInfo.artistName
            FieldType.ALBUM -> songInfo.albumName
            FieldType.ALBUM_ARTIST -> songInfo.albumArtistName
            FieldType.GENRE -> songInfo.genre
            else -> throw IllegalArgumentException("This field can't be searched on")
        }
    }

    suspend fun download(songId: Long) {
        val audioCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val newSongDetails = getNewSongDetails(songId)

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
        lastSongInfo = null
        initSongInfo(songId)
    }

    private suspend fun getNewSongDetails(songId: Long): ContentValues {
        val songInfo = getSongInfo(songId)
        return ContentValues().apply {
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
    }

    suspend fun getInfoRows(songId: Long): List<SongRow> {
        val songInfo = getSongInfo(songId)
        return listOfNotNull(
            getSongOption(songInfo, FieldType.TITLE, ActionType.EXPAND_TITLE),
            getSongOption(songInfo, FieldType.ARTIST, ActionType.EXPAND_TITLE),
            getSongOption(songInfo, FieldType.TRACK, ActionType.INFO_TITLE),
            getSongOption(songInfo, FieldType.ALBUM, ActionType.EXPAND_TITLE),
            getSongOption(songInfo, FieldType.ALBUM_ARTIST, ActionType.EXPAND_TITLE),
            getSongOption(songInfo, FieldType.GENRE, ActionType.EXPAND_TITLE),
            getSongOption(songInfo, FieldType.DURATION, ActionType.INFO_TITLE),
            getSongOption(songInfo, FieldType.YEAR, ActionType.INFO_TITLE)
        )
    }

    suspend fun getOptionsRows(songId: Long, fieldType: FieldType): List<SongRow> {
        val songInfo = getSongInfo(songId)
        return when (fieldType) {
            FieldType.TITLE -> listOfNotNull(
                getSongOption(songInfo, fieldType, ActionType.ADD_NEXT),
                getSongOption(songInfo, fieldType, ActionType.ADD_TO_PLAYLIST),
                getSongOption(songInfo, fieldType, ActionType.ADD_TO_FILTER),
                getSongOption(songInfo, fieldType, ActionType.SEARCH),
                if (songInfo.local == null) {
                    getSongOption(songInfo, fieldType, ActionType.DOWNLOAD)
                } else {
                    getSongOption(songInfo, fieldType, ActionType.NONE)
                }
            )
            FieldType.ARTIST -> listOfNotNull(
                getSongOption(songInfo, fieldType, ActionType.ADD_TO_FILTER),
                getSongOption(songInfo, fieldType, ActionType.SEARCH)
            )
            FieldType.ALBUM -> listOfNotNull(
                getSongOption(songInfo, fieldType, ActionType.ADD_TO_FILTER),
                getSongOption(songInfo, fieldType, ActionType.SEARCH)
            )
            FieldType.ALBUM_ARTIST -> listOfNotNull(
                getSongOption(songInfo, fieldType, ActionType.ADD_TO_FILTER),
                getSongOption(songInfo, fieldType, ActionType.SEARCH)
            )
            FieldType.GENRE -> listOfNotNull(
                getSongOption(songInfo, fieldType, ActionType.ADD_TO_FILTER),
                getSongOption(songInfo, fieldType, ActionType.SEARCH)
            )
            else -> listOf()
        }
    }

    private fun getSongOption(songInfo: SongInfo, field: FieldType, action: ActionType): SongRow? {
        return when (action) {
            ActionType.EXPAND_TITLE -> when (field) {
                FieldType.TITLE -> SongRow(R.string.info_title, songInfo.title, null, R.drawable.ic_next_occurence, FieldType.TITLE, ActionType.EXPAND_TITLE)
                FieldType.ARTIST -> SongRow(R.string.info_artist, songInfo.artistName, null, R.drawable.ic_next_occurence, FieldType.ARTIST, ActionType.EXPAND_TITLE)
                FieldType.ALBUM -> SongRow(R.string.info_album, songInfo.albumName, null, R.drawable.ic_next_occurence, FieldType.ALBUM, ActionType.EXPAND_TITLE)
                FieldType.ALBUM_ARTIST -> SongRow(R.string.info_album_artist, songInfo.albumArtistName, null, R.drawable.ic_next_occurence, FieldType.ALBUM_ARTIST, ActionType.EXPAND_TITLE)
                FieldType.GENRE -> SongRow(R.string.info_genre, songInfo.genre, null, R.drawable.ic_next_occurence, FieldType.GENRE, ActionType.EXPAND_TITLE)
                else -> null
            }
            ActionType.INFO_TITLE -> when (field) {
                FieldType.TRACK -> SongRow(R.string.info_track, songInfo.track.toString(), null, 0, FieldType.TRACK, ActionType.INFO_TITLE)
                FieldType.DURATION -> SongRow(R.string.info_duration, songInfo.timeText, null, 0, FieldType.DURATION, ActionType.INFO_TITLE)
                FieldType.YEAR -> SongRow(R.string.info_year, songInfo.year.toString(), null, 0, FieldType.YEAR, ActionType.INFO_TITLE)
                else -> null
            }
            ActionType.NONE -> if (field == FieldType.TITLE) SongRow(R.string.info_option_downloaded, null, R.string.info_option_downloaded_description, R.drawable.ic_downloaded, FieldType.TITLE, ActionType.NONE) else null
            ActionType.ADD_TO_FILTER -> when (field) {
                FieldType.TITLE -> SongRow(R.string.info_option_filter_title, songInfo.title, R.string.info_option_filter_on, R.drawable.ic_filter, FieldType.TITLE, ActionType.ADD_TO_FILTER)
                FieldType.ARTIST -> SongRow(R.string.info_option_filter_title, songInfo.artistName, R.string.info_option_filter_on, R.drawable.ic_filter, FieldType.ARTIST, ActionType.ADD_TO_FILTER)
                FieldType.ALBUM -> SongRow(R.string.info_option_filter_title, songInfo.albumName, R.string.info_option_filter_on, R.drawable.ic_filter, FieldType.ALBUM, ActionType.ADD_TO_FILTER)
                FieldType.ALBUM_ARTIST -> SongRow(R.string.info_option_filter_title, songInfo.albumArtistName, R.string.info_option_filter_on, R.drawable.ic_filter, FieldType.ALBUM_ARTIST, ActionType.ADD_TO_FILTER)
                FieldType.GENRE -> SongRow(R.string.info_option_filter_title, songInfo.genre, R.string.info_option_filter_on, R.drawable.ic_filter, FieldType.GENRE, ActionType.ADD_TO_FILTER)
                else -> null
            }
            ActionType.ADD_TO_PLAYLIST -> if (field == FieldType.TITLE) SongRow(
                R.string.info_option_add_to_playlist,
                null,
                R.string.info_option_add_to_playlist_detail,
                R.drawable.ic_add_to_playlist,
                FieldType.TITLE,
                ActionType.ADD_TO_PLAYLIST
            ) else null
            ActionType.ADD_NEXT -> if (field == FieldType.TITLE) SongRow(R.string.info_option_next_title, null, R.string.info_option_track_next, R.drawable.ic_play_next, FieldType.TITLE, ActionType.ADD_NEXT) else null
            ActionType.SEARCH -> when (field) {
                FieldType.TITLE -> SongRow(
                    R.string.info_option_search_title,
                    songInfo.title,
                    R.string.info_option_search_on,
                    R.drawable.ic_search,
                    FieldType.TITLE,
                    ActionType.SEARCH
                )
                FieldType.ARTIST -> SongRow(
                    R.string.info_option_search_title,
                    songInfo.artistName,
                    R.string.info_option_search_on,
                    R.drawable.ic_search,
                    FieldType.ARTIST,
                    ActionType.SEARCH
                )
                FieldType.ALBUM -> SongRow(
                    R.string.info_option_search_title,
                    songInfo.albumName,
                    R.string.info_option_search_on,
                    R.drawable.ic_search,
                    FieldType.ALBUM,
                    ActionType.SEARCH
                )
                FieldType.ALBUM_ARTIST -> SongRow(
                    R.string.info_option_search_title,
                    songInfo.albumArtistName,
                    R.string.info_option_search_on,
                    R.drawable.ic_search,
                    FieldType.ALBUM_ARTIST,
                    ActionType.SEARCH
                )
                else -> null
            }
            ActionType.DOWNLOAD -> if (field == FieldType.TITLE) SongRow(R.string.info_option_download, null, R.string.info_option_download_description, R.drawable.ic_download, FieldType.TITLE, ActionType.DOWNLOAD) else null
        }
    }

    fun getQuickOptions(): List<SongRow> {
        // val stringSet = sharedPreferences.getStringSet("quickOptions", setOf()) ?: return emptyList()
        val stringSet = listOf("TITLE|ADD_TO_FILTER", "TITLE|SEARCH", "TITLE|ADD_TO_PLAYLIST")
        val songInfo = SongInfo(0L, "", "", 0L, "", 0L, "", 0L, 0, 0, 0, "", "", "", "", null)

        return stringSet.mapNotNull {
            val fieldTypeString = it.substringBefore('|')
            val actionTypeString = it.substringAfter('|')

            val fieldType = FieldType.valueOf(fieldTypeString)
            val actionType = ActionType.valueOf(actionTypeString)
            getSongOption(songInfo, fieldType, actionType)
        }
    }

    class SongRow(@StringRes val title: Int, val text: String?, @StringRes val textRes: Int?, @DrawableRes val icon: Int, val fieldType: FieldType, val actionType: ActionType)

    enum class FieldType {
        TITLE, TRACK, ARTIST, ALBUM, ALBUM_ARTIST, GENRE, YEAR, DURATION
    }

    enum class ActionType {
        NONE, INFO_TITLE, EXPAND_TITLE, ADD_TO_FILTER, ADD_TO_PLAYLIST, ADD_NEXT, SEARCH, DOWNLOAD
    }
}