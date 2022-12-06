package be.florien.anyflow.feature.player.info.song.info

import android.content.ContentResolver
import android.content.ContentValues
import android.content.SharedPreferences
import android.os.Build
import android.provider.MediaStore
import be.florien.anyflow.R
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.feature.player.info.InfoActions
import be.florien.anyflow.player.FiltersManager
import be.florien.anyflow.player.OrderComposer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class SongInfoActions(
    private val contentResolver: ContentResolver,
    private val filtersManager: FiltersManager,
    private val orderComposer: OrderComposer,
    private val dataRepository: DataRepository,
    private val sharedPreferences: SharedPreferences
) : InfoActions<SongInfo>() {
    companion object {
        const val DUMMY_SONG_ID = -5L
        private const val QUICK_ACTIONS_PREF_NAME = "Quick actions"
    }

    private var lastSongInfo: SongInfo? = null

    fun getAlbumArtUrl(albumId: Long) = dataRepository.getAlbumArtUrl(albumId)

    suspend fun playNext(songId: Long) {
        orderComposer.changeSongPositionForNext(songId)
    }

    suspend fun filterOn(songInfo: SongInfo, fieldType: FieldType) {
        val filter = when (fieldType) {
            FieldType.TITLE -> Filter.SongIs(
                songInfo.id,
                songInfo.title
            )
            FieldType.ARTIST -> Filter.ArtistIs(songInfo.artistId, songInfo.artistName)
            FieldType.ALBUM -> Filter.AlbumIs(
                songInfo.albumId,
                songInfo.albumName
            )
            FieldType.ALBUM_ARTIST -> Filter.AlbumArtistIs(
                songInfo.albumArtistId,
                songInfo.albumArtistName
            )
            FieldType.GENRE -> Filter.GenreIs(
                songInfo.genreIds.first(),
                songInfo.genreNames.first()
            ) // todo handle multiple genre in info view
            else -> throw IllegalArgumentException("This field can't be filtered on")
        }
        filtersManager.clearFilters()
        filtersManager.addFilter(filter)
        filtersManager.commitChanges()
    }

    fun getSearchTerms(songInfo: SongInfo, fieldType: FieldType): String {
        return when (fieldType) {
            FieldType.TITLE -> songInfo.title
            FieldType.ARTIST -> songInfo.artistName
            FieldType.ALBUM -> songInfo.albumName
            FieldType.ALBUM_ARTIST -> songInfo.albumArtistName
            FieldType.GENRE -> songInfo.genreNames.first()
            else -> throw IllegalArgumentException("This field can't be searched on")
        }
    }

    suspend fun download(songInfo: SongInfo) {
        val audioCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val newSongDetails = getNewSongDetails(songInfo)

        val newSongUri = contentResolver.insert(audioCollection, newSongDetails) ?: return

        val result = kotlin.runCatching {
            withContext(Dispatchers.IO) {
                val songUrl = dataRepository.getSongUrl(songInfo.id)
                URL(songUrl).openStream().use { input ->
                    contentResolver.openOutputStream(newSongUri)?.use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
        if (result.isFailure) {
            throw result.exceptionOrNull() ?: return
        }
        dataRepository.updateSongLocalUri(songInfo.id, newSongUri.toString())
        lastSongInfo = null
    }

    private fun getNewSongDetails(songInfo: SongInfo): ContentValues {
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
                put(MediaStore.Audio.Media.GENRE, songInfo.genreNames.first())
                put(MediaStore.Audio.Media.YEAR, songInfo.year)
            }
        }
    }

    override fun getInfoRows(infoSource: SongInfo): List<InfoRow> {
        return listOfNotNull(
            getSongAction(infoSource, FieldType.TITLE, ActionType.EXPANDABLE_TITLE),
            getSongAction(infoSource, FieldType.ARTIST, ActionType.EXPANDABLE_TITLE),
            getSongAction(infoSource, FieldType.TRACK, ActionType.INFO_TITLE),
            getSongAction(infoSource, FieldType.ALBUM, ActionType.EXPANDABLE_TITLE),
            getSongAction(infoSource, FieldType.ALBUM_ARTIST, ActionType.EXPANDABLE_TITLE),
            getSongAction(infoSource, FieldType.GENRE, ActionType.EXPANDABLE_TITLE),
            getSongAction(infoSource, FieldType.DURATION, ActionType.INFO_TITLE),
            getSongAction(infoSource, FieldType.YEAR, ActionType.INFO_TITLE)
        )
    }

    override fun getActionsRows(
        infoSource: SongInfo,
        fieldType: FieldType
    ): List<InfoRow> {
        return when (fieldType) {
            FieldType.TITLE -> listOfNotNull(
                getSongAction(infoSource, fieldType, ActionType.ADD_NEXT),
                getSongAction(infoSource, fieldType, ActionType.ADD_TO_PLAYLIST),
                getSongAction(infoSource, fieldType, ActionType.ADD_TO_FILTER),
                getSongAction(infoSource, fieldType, ActionType.SEARCH),
                if (infoSource.local == null) {
                    getSongAction(infoSource, fieldType, ActionType.DOWNLOAD)
                } else {
                    getSongAction(infoSource, fieldType, ActionType.NONE)
                }
            )
            FieldType.ARTIST -> listOfNotNull(
                getSongAction(infoSource, fieldType, ActionType.ADD_TO_FILTER),
                getSongAction(infoSource, fieldType, ActionType.SEARCH)
            )
            FieldType.ALBUM -> listOfNotNull(
                getSongAction(infoSource, fieldType, ActionType.ADD_TO_FILTER),
                getSongAction(infoSource, fieldType, ActionType.SEARCH)
            )
            FieldType.ALBUM_ARTIST -> listOfNotNull(
                getSongAction(infoSource, fieldType, ActionType.ADD_TO_FILTER),
                getSongAction(infoSource, fieldType, ActionType.SEARCH)
            )
            FieldType.GENRE -> listOfNotNull(
                getSongAction(infoSource, fieldType, ActionType.ADD_TO_FILTER),
                getSongAction(infoSource, fieldType, ActionType.SEARCH)
            )
            else -> listOf()
        }
    }

    private fun getSongAction(
        songInfo: SongInfo,
        field: FieldType,
        action: ActionType
    ): InfoRow? {
        val order = getQuickActions().indexOfFirst { it.actionType == action && it.fieldType == field}.takeIf { it >= 0 }
        return getSongAction(songInfo, field, action, order)
    }

    private fun getSongAction(
        songInfo: SongInfo,
        field: FieldType,
        action: ActionType,
        order: Int?
    ): InfoRow? {
        return when (action) {
            ActionType.EXPANDABLE_TITLE -> when (field) {
                FieldType.TITLE -> InfoRow(
                    R.string.info_title,
                    songInfo.title,
                    null,
                    FieldType.TITLE,
                    ActionType.EXPANDABLE_TITLE,
                    order
                )
                FieldType.ARTIST -> InfoRow(
                    R.string.info_artist,
                    songInfo.artistName,
                    null,
                    FieldType.ARTIST,
                    ActionType.EXPANDABLE_TITLE,
                    order
                )
                FieldType.ALBUM -> InfoRow(
                    R.string.info_album,
                    songInfo.albumName,
                    null,
                    FieldType.ALBUM,
                    ActionType.EXPANDABLE_TITLE,
                    order
                )
                FieldType.ALBUM_ARTIST -> InfoRow(
                    R.string.info_album_artist,
                    songInfo.albumArtistName,
                    null,
                    FieldType.ALBUM_ARTIST,
                    ActionType.EXPANDABLE_TITLE,
                    order
                )
                FieldType.GENRE -> InfoRow(
                    R.string.info_genre,
                    songInfo.genreNames.first(),
                    null,
                    FieldType.GENRE,
                    ActionType.EXPANDABLE_TITLE,
                    order
                )
                else -> null
            }
            ActionType.EXPANDED_TITLE -> when (field) {
                FieldType.TITLE -> InfoRow(
                    R.string.info_title,
                    songInfo.title,
                    null,
                    FieldType.TITLE,
                    ActionType.EXPANDED_TITLE,
                    order
                )
                FieldType.ARTIST -> InfoRow(
                    R.string.info_artist,
                    songInfo.artistName,
                    null,
                    FieldType.ARTIST,
                    ActionType.EXPANDED_TITLE,
                    order
                )
                FieldType.ALBUM -> InfoRow(
                    R.string.info_album,
                    songInfo.albumName,
                    null,
                    FieldType.ALBUM,
                    ActionType.EXPANDED_TITLE,
                    order
                )
                FieldType.ALBUM_ARTIST -> InfoRow(
                    R.string.info_album_artist,
                    songInfo.albumArtistName,
                    null,
                    FieldType.ALBUM_ARTIST,
                    ActionType.EXPANDED_TITLE,
                    order
                )
                FieldType.GENRE -> InfoRow(
                    R.string.info_genre,
                    songInfo.genreNames.first(),
                    null,
                    FieldType.GENRE,
                    ActionType.EXPANDED_TITLE,
                    order
                )
                else -> null
            }
            ActionType.INFO_TITLE -> when (field) {
                FieldType.TRACK -> InfoRow(
                    R.string.info_track,
                    songInfo.track.toString(),
                    null,
                    FieldType.TRACK,
                    ActionType.INFO_TITLE,
                    order
                )
                FieldType.DURATION -> InfoRow(
                    R.string.info_duration,
                    songInfo.timeText,
                    null,
                    FieldType.DURATION,
                    ActionType.INFO_TITLE,
                    order
                )
                FieldType.YEAR -> InfoRow(
                    R.string.info_year,
                    songInfo.year.toString(),
                    null,
                    FieldType.YEAR,
                    ActionType.INFO_TITLE,
                    order
                )
                else -> null
            }
            ActionType.NONE -> if (field == FieldType.TITLE) InfoRow(
                R.string.info_action_downloaded,
                null,
                R.string.info_action_downloaded_description,
                FieldType.TITLE,
                ActionType.NONE,
                order
            ) else null
            ActionType.ADD_TO_FILTER -> when (field) {
                FieldType.TITLE -> InfoRow(
                    R.string.info_action_filter_title,
                    songInfo.title,
                    R.string.info_action_filter_on,
                    FieldType.TITLE,
                    ActionType.ADD_TO_FILTER,
                    order
                )
                FieldType.ARTIST -> InfoRow(
                    R.string.info_action_filter_title,
                    songInfo.artistName,
                    R.string.info_action_filter_on,
                    FieldType.ARTIST,
                    ActionType.ADD_TO_FILTER,
                    order
                )
                FieldType.ALBUM -> InfoRow(
                    R.string.info_action_filter_title,
                    songInfo.albumName,
                    R.string.info_action_filter_on,
                    FieldType.ALBUM,
                    ActionType.ADD_TO_FILTER,
                    order
                )
                FieldType.ALBUM_ARTIST -> InfoRow(
                    R.string.info_action_filter_title,
                    songInfo.albumArtistName,
                    R.string.info_action_filter_on,
                    FieldType.ALBUM_ARTIST,
                    ActionType.ADD_TO_FILTER,
                    order
                )
                FieldType.GENRE -> InfoRow(
                    R.string.info_action_filter_title,
                    songInfo.genreNames.first(),
                    R.string.info_action_filter_on,
                    FieldType.GENRE,
                    ActionType.ADD_TO_FILTER,
                    order
                )
                else -> null
            }
            ActionType.ADD_TO_PLAYLIST -> if (field == FieldType.TITLE) InfoRow(
                R.string.info_action_add_to_playlist,
                null,
                R.string.info_action_add_to_playlist_detail,
                FieldType.TITLE,
                ActionType.ADD_TO_PLAYLIST,
                order
            ) else null
            ActionType.ADD_NEXT -> if (field == FieldType.TITLE) InfoRow(
                R.string.info_action_next_title,
                null,
                R.string.info_action_track_next,
                FieldType.TITLE,
                ActionType.ADD_NEXT,
                order
            ) else null
            ActionType.SEARCH -> when (field) {
                FieldType.TITLE -> InfoRow(
                    R.string.info_action_search_title,
                    songInfo.title,
                    R.string.info_action_search_on,
                    FieldType.TITLE,
                    ActionType.SEARCH,
                    order
                )
                FieldType.ARTIST -> InfoRow(
                    R.string.info_action_search_title,
                    songInfo.artistName,
                    R.string.info_action_search_on,
                    FieldType.ARTIST,
                    ActionType.SEARCH,
                    order
                )
                FieldType.ALBUM -> InfoRow(
                    R.string.info_action_search_title,
                    songInfo.albumName,
                    R.string.info_action_search_on,
                    FieldType.ALBUM,
                    ActionType.SEARCH,
                    order
                )
                FieldType.ALBUM_ARTIST -> InfoRow(
                    R.string.info_action_search_title,
                    songInfo.albumArtistName,
                    R.string.info_action_search_on,
                    FieldType.ALBUM_ARTIST,
                    ActionType.SEARCH,
                    order
                )
                else -> null
            }
            ActionType.DOWNLOAD -> if (field == FieldType.TITLE) InfoRow(
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

    fun getQuickActions(): List<InfoRow> {
        val string = sharedPreferences.getString(QUICK_ACTIONS_PREF_NAME, "") ?: return emptyList()
        val songInfo = SongInfo.dummySongInfo()

        return string.split("#").filter { it.isNotEmpty() }.mapIndexedNotNull { index, it ->
            val fieldTypeString = it.substringBefore('|')
            val actionTypeString = it.substringAfter('|')

            val fieldType = FieldType.valueOf(fieldTypeString)
            val actionType = ActionType.valueOf(actionTypeString)
            getSongAction(songInfo, fieldType, actionType, index)
        }
    }
}