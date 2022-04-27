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
import be.florien.anyflow.data.view.Song
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.player.FiltersManager
import be.florien.anyflow.player.OrderComposer
import java.net.URL

class SongInfoActions constructor(
    private val contentResolver: ContentResolver,
    private val ampache: AmpacheConnection,
    private val filtersManager: FiltersManager,
    private val orderComposer: OrderComposer,
    private val dataRepository: DataRepository,
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        const val DUMMY_SONG_ID = -5L
        private const val QUICK_ACTIONS_PREF_NAME = "Quick actions"
    }

    private var lastSongInfo: SongInfo? = null

    private suspend fun initSongInfo(songId: Long) {
        lastSongInfo =
            dataRepository.getSongById(songId) ?: throw IllegalArgumentException("No song for ID")
    }

    private suspend fun getSongInfo(songId: Long): SongInfo {
        if (lastSongInfo == null || lastSongInfo?.id != songId) {
            initSongInfo(songId)
        }
        return lastSongInfo as SongInfo
    }

    suspend fun getSongInfo(song: Song): SongInfo {
        if (song.id == DUMMY_SONG_ID) {
            lastSongInfo =
                SongInfo(
                    id = DUMMY_SONG_ID,
                    title = song.title,
                    artistName = song.artistName,
                    artistId = 0L,
                    albumName = song.albumName,
                    albumId = 0L,
                    albumArtistName = song.albumArtistName,
                    albumArtistId = 0L,
                    track = 0,
                    time = song.time,
                    year = 0,
                    url = song.url,
                    art = song.art,
                    genre = song.genre,
                    fileName = "",
                    local = null
                )
        } else if (lastSongInfo == null || lastSongInfo?.id != song.id) {
            initSongInfo(song.id)
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
            FieldType.ALBUM_ARTIST -> Filter.AlbumArtistIs(
                songInfo.albumArtistId,
                songInfo.albumArtistName,
                null
            )
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
            getSongAction(songInfo, FieldType.TITLE, ActionType.EXPANDABLE_TITLE),
            getSongAction(songInfo, FieldType.ARTIST, ActionType.EXPANDABLE_TITLE),
            getSongAction(songInfo, FieldType.TRACK, ActionType.INFO_TITLE),
            getSongAction(songInfo, FieldType.ALBUM, ActionType.EXPANDABLE_TITLE),
            getSongAction(songInfo, FieldType.ALBUM_ARTIST, ActionType.EXPANDABLE_TITLE),
            getSongAction(songInfo, FieldType.GENRE, ActionType.EXPANDABLE_TITLE),
            getSongAction(songInfo, FieldType.DURATION, ActionType.INFO_TITLE),
            getSongAction(songInfo, FieldType.YEAR, ActionType.INFO_TITLE)
        )
    }

    suspend fun getActionsRows(
        songId: Long,
        fieldType: FieldType,
        order: Int? = null
    ): List<SongRow> {
        val songInfo = getSongInfo(songId)
        return when (fieldType) {
            FieldType.TITLE -> listOfNotNull(
                getSongAction(songInfo, fieldType, ActionType.ADD_NEXT, order),
                getSongAction(songInfo, fieldType, ActionType.ADD_TO_PLAYLIST, order),
                getSongAction(songInfo, fieldType, ActionType.ADD_TO_FILTER, order),
                getSongAction(songInfo, fieldType, ActionType.SEARCH, order),
                if (songInfo.local == null) {
                    getSongAction(songInfo, fieldType, ActionType.DOWNLOAD, order)
                } else {
                    getSongAction(songInfo, fieldType, ActionType.NONE, order)
                }
            )
            FieldType.ARTIST -> listOfNotNull(
                getSongAction(songInfo, fieldType, ActionType.ADD_TO_FILTER, order),
                getSongAction(songInfo, fieldType, ActionType.SEARCH, order)
            )
            FieldType.ALBUM -> listOfNotNull(
                getSongAction(songInfo, fieldType, ActionType.ADD_TO_FILTER, order),
                getSongAction(songInfo, fieldType, ActionType.SEARCH, order)
            )
            FieldType.ALBUM_ARTIST -> listOfNotNull(
                getSongAction(songInfo, fieldType, ActionType.ADD_TO_FILTER, order),
                getSongAction(songInfo, fieldType, ActionType.SEARCH, order)
            )
            FieldType.GENRE -> listOfNotNull(
                getSongAction(songInfo, fieldType, ActionType.ADD_TO_FILTER, order),
                getSongAction(songInfo, fieldType, ActionType.SEARCH, order)
            )
            else -> listOf()
        }
    }

    private fun getSongAction(
        songInfo: SongInfo,
        field: FieldType,
        action: ActionType,
        order: Int? = null
    ): SongRow? {
        return when (action) {
            ActionType.EXPANDABLE_TITLE -> when (field) {
                FieldType.TITLE -> SongRow(
                    R.string.info_title,
                    songInfo.title,
                    null,
                    FieldType.TITLE,
                    ActionType.EXPANDABLE_TITLE,
                    order
                )
                FieldType.ARTIST -> SongRow(
                    R.string.info_artist,
                    songInfo.artistName,
                    null,
                    FieldType.ARTIST,
                    ActionType.EXPANDABLE_TITLE,
                    order
                )
                FieldType.ALBUM -> SongRow(
                    R.string.info_album,
                    songInfo.albumName,
                    null,
                    FieldType.ALBUM,
                    ActionType.EXPANDABLE_TITLE,
                    order
                )
                FieldType.ALBUM_ARTIST -> SongRow(
                    R.string.info_album_artist,
                    songInfo.albumArtistName,
                    null,
                    FieldType.ALBUM_ARTIST,
                    ActionType.EXPANDABLE_TITLE,
                    order
                )
                FieldType.GENRE -> SongRow(
                    R.string.info_genre,
                    songInfo.genre,
                    null,
                    FieldType.GENRE,
                    ActionType.EXPANDABLE_TITLE,
                    order
                )
                else -> null
            }
            ActionType.EXPANDED_TITLE -> when (field) {
                FieldType.TITLE -> SongRow(
                    R.string.info_title,
                    songInfo.title,
                    null,
                    FieldType.TITLE,
                    ActionType.EXPANDED_TITLE,
                    order
                )
                FieldType.ARTIST -> SongRow(
                    R.string.info_artist,
                    songInfo.artistName,
                    null,
                    FieldType.ARTIST,
                    ActionType.EXPANDED_TITLE,
                    order
                )
                FieldType.ALBUM -> SongRow(
                    R.string.info_album,
                    songInfo.albumName,
                    null,
                    FieldType.ALBUM,
                    ActionType.EXPANDED_TITLE,
                    order
                )
                FieldType.ALBUM_ARTIST -> SongRow(
                    R.string.info_album_artist,
                    songInfo.albumArtistName,
                    null,
                    FieldType.ALBUM_ARTIST,
                    ActionType.EXPANDED_TITLE,
                    order
                )
                FieldType.GENRE -> SongRow(
                    R.string.info_genre,
                    songInfo.genre,
                    null,
                    FieldType.GENRE,
                    ActionType.EXPANDED_TITLE,
                    order
                )
                else -> null
            }
            ActionType.INFO_TITLE -> when (field) {
                FieldType.TRACK -> SongRow(
                    R.string.info_track,
                    songInfo.track.toString(),
                    null,
                    FieldType.TRACK,
                    ActionType.INFO_TITLE,
                    order
                )
                FieldType.DURATION -> SongRow(
                    R.string.info_duration,
                    songInfo.timeText,
                    null,
                    FieldType.DURATION,
                    ActionType.INFO_TITLE,
                    order
                )
                FieldType.YEAR -> SongRow(
                    R.string.info_year,
                    songInfo.year.toString(),
                    null,
                    FieldType.YEAR,
                    ActionType.INFO_TITLE,
                    order
                )
                else -> null
            }
            ActionType.NONE -> if (field == FieldType.TITLE) SongRow(
                R.string.info_action_downloaded,
                null,
                R.string.info_action_downloaded_description,
                FieldType.TITLE,
                ActionType.NONE,
                order
            ) else null
            ActionType.ADD_TO_FILTER -> when (field) {
                FieldType.TITLE -> SongRow(
                    R.string.info_action_filter_title,
                    songInfo.title,
                    R.string.info_action_filter_on,
                    FieldType.TITLE,
                    ActionType.ADD_TO_FILTER,
                    order
                )
                FieldType.ARTIST -> SongRow(
                    R.string.info_action_filter_title,
                    songInfo.artistName,
                    R.string.info_action_filter_on,
                    FieldType.ARTIST,
                    ActionType.ADD_TO_FILTER,
                    order
                )
                FieldType.ALBUM -> SongRow(
                    R.string.info_action_filter_title,
                    songInfo.albumName,
                    R.string.info_action_filter_on,
                    FieldType.ALBUM,
                    ActionType.ADD_TO_FILTER,
                    order
                )
                FieldType.ALBUM_ARTIST -> SongRow(
                    R.string.info_action_filter_title,
                    songInfo.albumArtistName,
                    R.string.info_action_filter_on,
                    FieldType.ALBUM_ARTIST,
                    ActionType.ADD_TO_FILTER,
                    order
                )
                FieldType.GENRE -> SongRow(
                    R.string.info_action_filter_title,
                    songInfo.genre,
                    R.string.info_action_filter_on,
                    FieldType.GENRE,
                    ActionType.ADD_TO_FILTER,
                    order
                )
                else -> null
            }
            ActionType.ADD_TO_PLAYLIST -> if (field == FieldType.TITLE) SongRow(
                R.string.info_action_add_to_playlist,
                null,
                R.string.info_action_add_to_playlist_detail,
                FieldType.TITLE,
                ActionType.ADD_TO_PLAYLIST,
                order
            ) else null
            ActionType.ADD_NEXT -> if (field == FieldType.TITLE) SongRow(
                R.string.info_action_next_title,
                null,
                R.string.info_action_track_next,
                FieldType.TITLE,
                ActionType.ADD_NEXT,
                order
            ) else null
            ActionType.SEARCH -> when (field) {
                FieldType.TITLE -> SongRow(
                    R.string.info_action_search_title,
                    songInfo.title,
                    R.string.info_action_search_on,
                    FieldType.TITLE,
                    ActionType.SEARCH,
                    order
                )
                FieldType.ARTIST -> SongRow(
                    R.string.info_action_search_title,
                    songInfo.artistName,
                    R.string.info_action_search_on,
                    FieldType.ARTIST,
                    ActionType.SEARCH,
                    order
                )
                FieldType.ALBUM -> SongRow(
                    R.string.info_action_search_title,
                    songInfo.albumName,
                    R.string.info_action_search_on,
                    FieldType.ALBUM,
                    ActionType.SEARCH,
                    order
                )
                FieldType.ALBUM_ARTIST -> SongRow(
                    R.string.info_action_search_title,
                    songInfo.albumArtistName,
                    R.string.info_action_search_on,
                    FieldType.ALBUM_ARTIST,
                    ActionType.SEARCH,
                    order
                )
                else -> null
            }
            ActionType.DOWNLOAD -> if (field == FieldType.TITLE) SongRow(
                R.string.info_action_download,
                null,
                R.string.info_action_download_description,
                FieldType.TITLE,
                ActionType.DOWNLOAD,
                order
            ) else null
        }
    }

    fun toggleQuickAction(fieldType: FieldType, actionType: ActionType) {
        val quickActions = getQuickActions().toMutableList()
        if (quickActions.removeAll { it.fieldType == fieldType && it.actionType == actionType }) {
            sharedPreferences.edit()
                .putString(
                    QUICK_ACTIONS_PREF_NAME,
                    quickActions.joinToString(separator = "#") { "${it.fieldType}|${it.actionType}" }
                )
                .apply()
        } else {
            val originalString = sharedPreferences.getString(QUICK_ACTIONS_PREF_NAME, "")
            sharedPreferences.edit()
                .putString(QUICK_ACTIONS_PREF_NAME, "$originalString#$fieldType|$actionType")
                .apply()
        }
    }

    fun getQuickActions(): List<SongRow> {
        val string = sharedPreferences.getString(QUICK_ACTIONS_PREF_NAME, "") ?: return emptyList()
        val songInfo = SongInfo(0L, "", "", 0L, "", 0L, "", 0L, 0, 0, 0, "", "", "", "", null)

        return string.split("#").filter { it.isNotEmpty() }.mapIndexedNotNull { index, it ->
            val fieldTypeString = it.substringBefore('|')
            val actionTypeString = it.substringAfter('|')

            val fieldType = FieldType.valueOf(fieldTypeString)
            val actionType = ActionType.valueOf(actionTypeString)
            getSongAction(songInfo, fieldType, actionType, index)
        }
    }

    class SongRow(
        @StringRes val title: Int,
        val text: String?,
        @StringRes val textRes: Int?,
        val fieldType: FieldType,
        val actionType: ActionType,
        val order: Int? = null
    ) {
        constructor(other: SongRow, order: Int? = null) : this(
            other.title,
            other.text,
            other.textRes,
            other.fieldType,
            other.actionType,
            order ?: other.order
        )
    }

    enum class FieldType(
        @DrawableRes
        val iconRes: Int
    ) {
        TITLE(R.drawable.ic_song),
        TRACK(R.drawable.ic_song),
        ARTIST(R.drawable.ic_artist),
        ALBUM(R.drawable.ic_album),
        ALBUM_ARTIST(R.drawable.ic_album_artist),
        GENRE(R.drawable.ic_genre),
        YEAR(R.drawable.ic_album),
        DURATION(R.drawable.ic_song)
    }

    enum class ActionType(
        @DrawableRes
        val iconRes: Int
    ) {
        NONE(0),
        INFO_TITLE(0),
        EXPANDABLE_TITLE(R.drawable.ic_next_occurence),
        EXPANDED_TITLE(R.drawable.ic_previous_occurence),
        ADD_TO_FILTER(R.drawable.ic_filter),
        ADD_TO_PLAYLIST(R.drawable.ic_add_to_playlist),
        ADD_NEXT(R.drawable.ic_play_next),
        SEARCH(R.drawable.ic_search),
        DOWNLOAD(R.drawable.ic_download)
    }
}