package be.florien.anyflow.feature.player.info.song

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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.InputStream
import java.io.OutputStream
import java.net.URL

class SongInfoActions(
    private val contentResolver: ContentResolver,
    private val filtersManager: FiltersManager,
    private val orderComposer: OrderComposer,
    private val dataRepository: DataRepository,
    private val sharedPreferences: SharedPreferences
) : InfoActions<SongInfo>() {

    /**
     * Overridden methods
     */

    override suspend fun getInfoRows(infoSource: SongInfo): List<InfoRow> {
        return listOfNotNull(
            getSongAction(infoSource, SongFieldType.Title(), ActionType.ExpandableTitle()),
            getSongAction(infoSource, SongFieldType.Artist(), ActionType.ExpandableTitle()),
            getSongAction(infoSource, SongFieldType.Track(), ActionType.InfoTitle()),
            getSongAction(infoSource, SongFieldType.Album(), ActionType.ExpandableTitle()),
            getSongAction(infoSource, SongFieldType.AlbumArtist(), ActionType.ExpandableTitle()),
            getSongAction(infoSource, SongFieldType.Genre(), ActionType.ExpandableTitle()),
            getSongAction(infoSource, SongFieldType.Duration(), ActionType.InfoTitle()),
            getSongAction(infoSource, SongFieldType.Year(), ActionType.InfoTitle())
        )
    }

    override suspend fun getActionsRows(
        infoSource: SongInfo,
        fieldType: FieldType
    ): List<InfoRow> {
        return when (fieldType) {
            is SongFieldType.Title -> listOfNotNull(
                getSongAction(infoSource, fieldType, SongActionType.AddNext()),
                getSongAction(infoSource, fieldType, SongActionType.AddToPlaylist()),
                getSongAction(infoSource, fieldType, SongActionType.AddToFilter()),
                getSongAction(infoSource, fieldType, SongActionType.Search()),
                if (infoSource.local == null) {
                    getSongAction(infoSource, fieldType, SongActionType.Download())
                } else {
                    getSongAction(infoSource, fieldType, ActionType.None())
                }
            )
            is SongFieldType.Artist -> listOfNotNull(
                getSongAction(infoSource, fieldType, SongActionType.AddToFilter()),
                getSongAction(infoSource, fieldType, SongActionType.Search())
            )
            is SongFieldType.Album -> listOfNotNull(
                getSongAction(infoSource, fieldType, SongActionType.AddToFilter()),
                getSongAction(infoSource, fieldType, SongActionType.Search())
            )
            is SongFieldType.AlbumArtist -> listOfNotNull(
                getSongAction(infoSource, fieldType, SongActionType.AddToFilter()),
                getSongAction(infoSource, fieldType, SongActionType.Search())
            )
            is SongFieldType.Genre -> listOfNotNull(
                getSongAction(infoSource, fieldType, SongActionType.AddToFilter()),
                getSongAction(infoSource, fieldType, SongActionType.Search())
            )
            else -> listOf()
        }
    }

    /**
     * Public methods
     */

    fun getAlbumArtUrl(albumId: Long) = dataRepository.getAlbumArtUrl(albumId)

    /**
     * Action methods
     */

    suspend fun playNext(songId: Long) {
        orderComposer.changeSongPositionForNext(songId)
    }

    suspend fun filterOn(songInfo: SongInfo, fieldType: SongFieldType) {
        val filter = when (fieldType) {
            is SongFieldType.Title -> Filter(Filter.FilterType.SONG_IS, songInfo.id, songInfo.title)
            is SongFieldType.Artist -> Filter(
                Filter.FilterType.ARTIST_IS,
                songInfo.artistId,
                songInfo.artistName
            )
            is SongFieldType.Album -> Filter(
                Filter.FilterType.ALBUM_IS,
                songInfo.albumId,
                songInfo.albumName
            )
            is SongFieldType.AlbumArtist -> Filter(
                Filter.FilterType.ALBUM_ARTIST_IS,
                songInfo.albumArtistId,
                songInfo.albumArtistName
            )
            is SongFieldType.Genre -> Filter(
                Filter.FilterType.GENRE_IS,
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
            is SongFieldType.Title -> songInfo.title
            is SongFieldType.Artist -> songInfo.artistName
            is SongFieldType.Album -> songInfo.albumName
            is SongFieldType.AlbumArtist -> songInfo.albumArtistName
            is SongFieldType.Genre -> songInfo.genreNames.first()
            else -> throw IllegalArgumentException("This field can't be searched on")
        }
    }

    suspend fun download(songInfo: SongInfo): Flow<Int> { //todo download manager which will keep flow in a map, and download wether it is observed or not
        val audioCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val newSongDetails = getNewSongDetails(songInfo)

        val newSongUri =
            contentResolver.insert(audioCollection, newSongDetails) ?: return emptyFlow()

        return flow {
            val songUrl = dataRepository.getSongUrl(songInfo.id)
            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null
            try {
                inputStream = URL(songUrl).openStream()
                outputStream = contentResolver.openOutputStream(newSongUri)
                if (outputStream != null) {
                    var bytesCopied = 0
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var bytes = inputStream.read(buffer)
                    while (bytes >= 0) {
                        outputStream.write(buffer, 0, bytes)
                        bytesCopied += bytes
                        emit(bytesCopied)
                        bytes = inputStream.read(buffer)
                    }
                }
                dataRepository.updateSongLocalUri(songInfo.id, newSongUri.toString())
            } catch (exception: Exception) {
                //todo
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
        }.flowOn(Dispatchers.IO)
    }

    /**
     * Quick actions
     */

    fun toggleQuickAction(fieldType: FieldType, actionType: ActionType) {
        val quickActions = getQuickActions().toMutableList()
        if (quickActions.removeAll { it.fieldType == fieldType && it.actionType == actionType }) {
            sharedPreferences.edit()
                .putString(
                    QUICK_ACTIONS_PREF_NAME,
                    quickActions.joinToString(separator = "#") { "${it.fieldType.javaClass.name}|${it.actionType.javaClass.name}" }
                )
                .apply()
        } else {
            val originalString = sharedPreferences.getString(QUICK_ACTIONS_PREF_NAME, "")
            sharedPreferences.edit()
                .putString(
                    QUICK_ACTIONS_PREF_NAME,
                    "$originalString#${fieldType.javaClass.name}|${actionType.javaClass.name}"
                )
                .apply()
        }
    }

    fun getQuickActions(): List<InfoRow> {
        val string = sharedPreferences.getString(QUICK_ACTIONS_PREF_NAME, "") ?: return emptyList()
        val songInfo = SongInfo.dummySongInfo()

        return string.split("#").filter { it.isNotEmpty() }.mapIndexedNotNull { index, it ->
            val fieldTypeString = it.substringBefore('|')
            val actionTypeString = it.substringAfter('|')

            val fieldType = SongFieldType.getClassFromName(fieldTypeString)
            if (fieldType != null) {
                val actionType = SongActionType.getClassFromName(actionTypeString)
                if (actionType != null) {
                    getSongAction(songInfo, fieldType, actionType, index)
                } else {
                    null
                }
            } else {
                null
            }
        }
    }

    /**
     * Private methods
     */

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

    private fun getSongAction(
        songInfo: SongInfo,
        field: FieldType,
        action: ActionType,
        order: Int? = null
    ): InfoRow? {
        return when (action) {
            is ActionType.ExpandableTitle -> when (field) {
                is SongFieldType.Title -> InfoRow(
                    R.string.info_title,
                    songInfo.title,
                    null,
                    SongFieldType.Title(),
                    ActionType.ExpandableTitle(),
                    order
                )
                is SongFieldType.Artist -> InfoRow(
                    R.string.info_artist,
                    songInfo.artistName,
                    null,
                    SongFieldType.Artist(),
                    ActionType.ExpandableTitle(),
                    order
                )
                is SongFieldType.Album -> InfoRow(
                    R.string.info_album,
                    songInfo.albumName,
                    null,
                    SongFieldType.Album(),
                    ActionType.ExpandableTitle(),
                    order
                )
                is SongFieldType.AlbumArtist -> InfoRow(
                    R.string.info_album_artist,
                    songInfo.albumArtistName,
                    null,
                    SongFieldType.AlbumArtist(),
                    ActionType.ExpandableTitle(),
                    order
                )
                is SongFieldType.Genre -> InfoRow(
                    R.string.info_genre,
                    songInfo.genreNames.first(),
                    null,
                    SongFieldType.Genre(),
                    ActionType.ExpandableTitle(),
                    order
                )
                else -> null
            }
            is ActionType.ExpandedTitle -> when (field) {
                is SongFieldType.Title -> InfoRow(
                    R.string.info_title,
                    songInfo.title,
                    null,
                    SongFieldType.Title(),
                    ActionType.ExpandedTitle(),
                    order
                )
                is SongFieldType.Artist -> InfoRow(
                    R.string.info_artist,
                    songInfo.artistName,
                    null,
                    SongFieldType.Artist(),
                    ActionType.ExpandedTitle(),
                    order
                )
                is SongFieldType.Album -> InfoRow(
                    R.string.info_album,
                    songInfo.albumName,
                    null,
                    SongFieldType.Album(),
                    ActionType.ExpandedTitle(),
                    order
                )
                is SongFieldType.AlbumArtist -> InfoRow(
                    R.string.info_album_artist,
                    songInfo.albumArtistName,
                    null,
                    SongFieldType.AlbumArtist(),
                    ActionType.ExpandedTitle(),
                    order
                )
                is SongFieldType.Genre -> InfoRow(
                    R.string.info_genre,
                    songInfo.genreNames.first(),
                    null,
                    SongFieldType.Genre(),
                    ActionType.ExpandedTitle(),
                    order
                )
                else -> null
            }
            is ActionType.InfoTitle -> when (field) {
                is SongFieldType.Track -> InfoRow(
                    R.string.info_track,
                    songInfo.track.toString(),
                    null,
                    SongFieldType.Track(),
                    ActionType.InfoTitle(),
                    order
                )
                is SongFieldType.Duration -> InfoRow(
                    R.string.info_duration,
                    songInfo.timeText,
                    null,
                    SongFieldType.Duration(),
                    ActionType.InfoTitle(),
                    order
                )
                is SongFieldType.Year -> InfoRow(
                    R.string.info_year,
                    songInfo.year.toString(),
                    null,
                    SongFieldType.Year(),
                    ActionType.InfoTitle(),
                    order
                )
                else -> null
            }
            is ActionType.None -> if (field is SongFieldType.Title) InfoRow(
                R.string.info_action_downloaded,
                null,
                R.string.info_action_downloaded_description,
                SongFieldType.Title(),
                ActionType.None(),
                order
            ) else null
            is SongActionType.AddToFilter -> when (field) {
                is SongFieldType.Title -> InfoRow(
                    R.string.info_action_filter_title,
                    songInfo.title,
                    R.string.info_action_filter_on,
                    SongFieldType.Title(),
                    SongActionType.AddToFilter(),
                    order
                )
                is SongFieldType.Artist -> InfoRow(
                    R.string.info_action_filter_title,
                    songInfo.artistName,
                    R.string.info_action_filter_on,
                    SongFieldType.Artist(),
                    SongActionType.AddToFilter(),
                    order
                )
                is SongFieldType.Album -> InfoRow(
                    R.string.info_action_filter_title,
                    songInfo.albumName,
                    R.string.info_action_filter_on,
                    SongFieldType.Album(),
                    SongActionType.AddToFilter(),
                    order
                )
                is SongFieldType.AlbumArtist -> InfoRow(
                    R.string.info_action_filter_title,
                    songInfo.albumArtistName,
                    R.string.info_action_filter_on,
                    SongFieldType.AlbumArtist(),
                    SongActionType.AddToFilter(),
                    order
                )
                is SongFieldType.Genre -> InfoRow(
                    R.string.info_action_filter_title,
                    songInfo.genreNames.first(),
                    R.string.info_action_filter_on,
                    SongFieldType.Genre(),
                    SongActionType.AddToFilter(),
                    order
                )
                else -> null
            }
            is SongActionType.AddToPlaylist -> if (field is SongFieldType.Title) InfoRow(
                R.string.info_action_select_playlist,
                null,
                R.string.info_action_select_playlist_detail,
                SongFieldType.Title(),
                SongActionType.AddToPlaylist(),
                order
            ) else null
            is SongActionType.AddNext -> if (field is SongFieldType.Title) InfoRow(
                R.string.info_action_next_title,
                null,
                R.string.info_action_track_next,
                SongFieldType.Title(),
                SongActionType.AddNext(),
                order
            ) else null
            is SongActionType.Search -> when (field) {
                is SongFieldType.Title -> InfoRow(
                    R.string.info_action_search_title,
                    songInfo.title,
                    R.string.info_action_search_on,
                    SongFieldType.Title(),
                    SongActionType.Search(),
                    order
                )
                is SongFieldType.Artist -> InfoRow(
                    R.string.info_action_search_title,
                    songInfo.artistName,
                    R.string.info_action_search_on,
                    SongFieldType.Artist(),
                    SongActionType.Search(),
                    order
                )
                is SongFieldType.Album -> InfoRow(
                    R.string.info_action_search_title,
                    songInfo.albumName,
                    R.string.info_action_search_on,
                    SongFieldType.Album(),
                    SongActionType.Search(),
                    order
                )
                is SongFieldType.AlbumArtist -> InfoRow(
                    R.string.info_action_search_title,
                    songInfo.albumArtistName,
                    R.string.info_action_search_on,
                    SongFieldType.AlbumArtist(),
                    SongActionType.Search(),
                    order
                )
                else -> null
            }
            is SongActionType.Download -> if (field is SongFieldType.Title) InfoRow(
                R.string.info_action_download,
                null,
                R.string.info_action_download_description,
                SongFieldType.Title(),
                SongActionType.Download(),
                order
            ) else null
            is LibraryActionType.SubFilter -> null
        }
    }

    companion object {
        const val DUMMY_SONG_ID = -5L
        private const val QUICK_ACTIONS_PREF_NAME = "Quick actions"
    }
}