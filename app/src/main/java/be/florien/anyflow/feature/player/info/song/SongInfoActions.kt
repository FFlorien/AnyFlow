package be.florien.anyflow.feature.player.info.song

import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import be.florien.anyflow.R
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.DownloadManager
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.feature.player.info.InfoActions
import be.florien.anyflow.player.FiltersManager
import be.florien.anyflow.player.OrderComposer

class SongInfoActions(
    private val filtersManager: FiltersManager,
    private val orderComposer: OrderComposer,
    private val dataRepository: DataRepository,
    private val sharedPreferences: SharedPreferences,
    private val downloadManager: DownloadManager
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
                if (infoSource.local.isNullOrBlank()) {
                    getSongAction(infoSource, fieldType, SongActionType.Download())
                } else {
                    getSongAction(infoSource, fieldType, ActionType.None())
                }
            )
            is SongFieldType.Artist -> listOfNotNull(
                getSongAction(infoSource, fieldType, SongActionType.AddToFilter()),
                getSongAction(infoSource, fieldType, SongActionType.Search()),
                getSongAction(infoSource, fieldType, SongActionType.Download())
            )
            is SongFieldType.Album -> listOfNotNull(
                getSongAction(infoSource, fieldType, SongActionType.AddToFilter()),
                getSongAction(infoSource, fieldType, SongActionType.Search()),
                getSongAction(infoSource, fieldType, SongActionType.Download())
            )
            is SongFieldType.AlbumArtist -> listOfNotNull(
                getSongAction(infoSource, fieldType, SongActionType.AddToFilter()),
                getSongAction(infoSource, fieldType, SongActionType.Search()),
                getSongAction(infoSource, fieldType, SongActionType.Download())
            )
            is SongFieldType.Genre -> listOfNotNull(
                getSongAction(infoSource, fieldType, SongActionType.AddToFilter()),
                getSongAction(infoSource, fieldType, SongActionType.Search()),
                getSongAction(infoSource, fieldType, SongActionType.Download())
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
            //todo display multiple playlists in info view
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

    fun download(songInfo: SongInfo) = downloadManager.download(songInfo)

    fun batchDownload(batchId: Long, type: Filter.FilterType) =
        downloadManager.batchDownload(batchId, type)

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

    fun getQuickActions(): List<QuickActionInfoRow> {
        val string = sharedPreferences.getString(QUICK_ACTIONS_PREF_NAME, "") ?: return emptyList()
        val songInfo = SongInfo.dummySongInfo()

        return string.split("#").filter { it.isNotEmpty() }.mapIndexedNotNull { index, it ->
            val fieldTypeString = it.substringBefore('|')
            val actionTypeString = it.substringAfter('|')

            val fieldType = SongFieldType.getClassFromName(fieldTypeString)
            if (fieldType != null) {
                val actionType = SongActionType.getClassFromName(actionTypeString)
                if (actionType != null) {
                    getSongAction(songInfo, fieldType, actionType, index) as? QuickActionInfoRow
                } else {
                    null
                }
            } else {
                null
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
                is SongFieldType.Title -> getInfoRow(
                    R.string.info_title,
                    songInfo.title,
                    null,
                    SongFieldType.Title(),
                    ActionType.ExpandableTitle(),
                    order = order
                )
                is SongFieldType.Artist -> getInfoRow(
                    R.string.info_artist,
                    songInfo.artistName,
                    null,
                    SongFieldType.Artist(),
                    ActionType.ExpandableTitle(),
                    order = order
                )
                is SongFieldType.Album -> getInfoRow(
                    R.string.info_album,
                    songInfo.albumName,
                    null,
                    SongFieldType.Album(),
                    ActionType.ExpandableTitle(),
                    order = order
                )
                is SongFieldType.AlbumArtist -> getInfoRow(
                    R.string.info_album_artist,
                    songInfo.albumArtistName,
                    null,
                    SongFieldType.AlbumArtist(),
                    ActionType.ExpandableTitle(),
                    order = order
                )
                is SongFieldType.Genre -> getInfoRow(
                    R.string.info_genre,
                    songInfo.genreNames.first(),
                    null,
                    SongFieldType.Genre(),
                    ActionType.ExpandableTitle(),
                    order = order
                )
                else -> null
            }
            is ActionType.InfoTitle -> when (field) {
                is SongFieldType.Track -> getInfoRow(
                    R.string.info_track,
                    songInfo.track.toString(),
                    null,
                    SongFieldType.Track(),
                    ActionType.InfoTitle(),
                    order = order
                )
                is SongFieldType.Duration -> getInfoRow(
                    R.string.info_duration,
                    songInfo.timeText,
                    null,
                    SongFieldType.Duration(),
                    ActionType.InfoTitle(),
                    order = order
                )
                is SongFieldType.Year -> getInfoRow(
                    R.string.info_year,
                    songInfo.year.toString(),
                    null,
                    SongFieldType.Year(),
                    ActionType.InfoTitle(),
                    order = order
                )
                else -> null
            }
            is ActionType.None -> if (field is SongFieldType.Title) getInfoRow(
                R.string.info_action_downloaded,
                null,
                R.string.info_action_downloaded_description,
                SongFieldType.Title(),
                ActionType.None(),
                order = order
            ) else null
            is SongActionType.AddToFilter -> when (field) {
                is SongFieldType.Title -> getInfoRow(
                    R.string.info_action_filter_title,
                    songInfo.title,
                    R.string.info_action_filter_on,
                    SongFieldType.Title(),
                    SongActionType.AddToFilter(),
                    order = order
                )
                is SongFieldType.Artist -> getInfoRow(
                    R.string.info_action_filter_title,
                    songInfo.artistName,
                    R.string.info_action_filter_on,
                    SongFieldType.Artist(),
                    SongActionType.AddToFilter(),
                    order = order
                )
                is SongFieldType.Album -> getInfoRow(
                    R.string.info_action_filter_title,
                    songInfo.albumName,
                    R.string.info_action_filter_on,
                    SongFieldType.Album(),
                    SongActionType.AddToFilter(),
                    order = order
                )
                is SongFieldType.AlbumArtist -> getInfoRow(
                    R.string.info_action_filter_title,
                    songInfo.albumArtistName,
                    R.string.info_action_filter_on,
                    SongFieldType.AlbumArtist(),
                    SongActionType.AddToFilter(),
                    order = order
                )
                is SongFieldType.Genre -> getInfoRow(
                    R.string.info_action_filter_title,
                    songInfo.genreNames.first(),
                    R.string.info_action_filter_on,
                    SongFieldType.Genre(),
                    SongActionType.AddToFilter(),
                    order = order
                )
                else -> null
            }
            is SongActionType.AddToPlaylist -> if (field is SongFieldType.Title) getInfoRow(
                R.string.info_action_select_playlist,
                null,
                R.string.info_action_select_playlist_detail,
                SongFieldType.Title(),
                SongActionType.AddToPlaylist(),
                order = order
            ) else null
            is SongActionType.AddNext -> if (field is SongFieldType.Title) getInfoRow(
                R.string.info_action_next_title,
                null,
                R.string.info_action_track_next,
                SongFieldType.Title(),
                SongActionType.AddNext(),
                order = order
            ) else null
            is SongActionType.Search -> when (field) {
                is SongFieldType.Title -> getInfoRow(
                    R.string.info_action_search_title,
                    songInfo.title,
                    R.string.info_action_search_on,
                    SongFieldType.Title(),
                    SongActionType.Search(),
                    order = order
                )
                is SongFieldType.Artist -> getInfoRow(
                    R.string.info_action_search_title,
                    songInfo.artistName,
                    R.string.info_action_search_on,
                    SongFieldType.Artist(),
                    SongActionType.Search(),
                    order = order
                )
                is SongFieldType.Album -> getInfoRow(
                    R.string.info_action_search_title,
                    songInfo.albumName,
                    R.string.info_action_search_on,
                    SongFieldType.Album(),
                    SongActionType.Search(),
                    order = order
                )
                is SongFieldType.AlbumArtist -> getInfoRow(
                    R.string.info_action_search_title,
                    songInfo.albumArtistName,
                    R.string.info_action_search_on,
                    SongFieldType.AlbumArtist(),
                    SongActionType.Search(),
                    order = order
                )
                else -> null
            }
            is SongActionType.Download -> when (field) {
                is SongFieldType.Title -> getInfoRow(
                    R.string.info_action_download,
                    songInfo.title,
                    R.string.info_action_download_description,
                    SongFieldType.Title(),
                    SongActionType.Download(),
                    order = order,
                    progress = downloadManager.getDownloadState(songInfo)
                )
                is SongFieldType.Album -> getInfoRow(
                    R.string.info_action_download,
                    songInfo.albumName,
                    R.string.info_action_download_description,
                    SongFieldType.Album(),
                    SongActionType.Download(),
                    order = order,
                    progress = downloadManager.getDownloadState(songInfo)//todo
                )
                is SongFieldType.AlbumArtist -> getInfoRow(
                    R.string.info_action_download,
                    songInfo.albumArtistName,
                    R.string.info_action_download_description,
                    SongFieldType.AlbumArtist(),
                    SongActionType.Download(),
                    order = order,
                    progress = downloadManager.getDownloadState(songInfo)//todo
                )
                is SongFieldType.Artist -> getInfoRow(
                    R.string.info_action_download,
                    songInfo.artistName,
                    R.string.info_action_download_description,
                    SongFieldType.Artist(),
                    SongActionType.Download(),
                    order = order,
                    progress = downloadManager.getDownloadState(songInfo)//todo
                )
                is SongFieldType.Genre -> getInfoRow(
                    R.string.info_action_download,
                    songInfo.genreNames.first(),
                    R.string.info_action_download_description,
                    SongFieldType.Genre(),
                    SongActionType.Download(),
                    order = order,
                    progress = downloadManager.getDownloadState(songInfo)//todo
                )
                else -> null
            }
            is LibraryActionType.SubFilter -> null
        }
    }

    private fun getInfoRow(
        title: Int,
        text: String?,
        textRes: Int?,
        fieldType: FieldType,
        actionType: ActionType,
        progress: LiveData<Int>? = null,
        order: Int? = null
    ): InfoRow = if (order != null) {
        QuickActionInfoRow(title, text, textRes, fieldType, actionType, order)
    } else if (progress != null) {
        SongDownloadInfoRow(title, text, textRes, fieldType, actionType, progress)
    } else {
        SongInfoRow(title, text, textRes, fieldType, actionType)
    }

    data class SongInfoRow(
        @StringRes override val title: Int,
        override val text: String?,
        @StringRes override val textRes: Int?,
        override val fieldType: FieldType,
        override val actionType: ActionType
    ) : InfoRow(title, text, textRes, fieldType, actionType)

    data class SongDownloadInfoRow(
        @StringRes override val title: Int,
        override val text: String?,
        @StringRes override val textRes: Int?,
        override val fieldType: FieldType,
        override val actionType: ActionType,
        val progress: LiveData<Int>
    ) : InfoRow(title, text, textRes, fieldType, actionType) {
        override fun areContentTheSame(other: InfoRow): Boolean =
            super.areContentTheSame(other) && other is SongDownloadInfoRow && other.progress === progress
    }

    data class QuickActionInfoRow(
        @StringRes override val title: Int,
        override val text: String?,
        @StringRes override val textRes: Int?,
        override val fieldType: FieldType,
        override val actionType: ActionType,
        val order: Int
    ) : InfoRow(title, text, textRes, fieldType, actionType) {
        constructor(other: InfoRow, order: Int) : this(
            other.title,
            other.text,
            other.textRes,
            other.fieldType,
            other.actionType,
            order
        )

        override fun areContentTheSame(other: InfoRow): Boolean =
            super.areContentTheSame(other) && other is QuickActionInfoRow && other.order == order
    }

    companion object {
        const val DUMMY_SONG_ID = -5L
        private const val QUICK_ACTIONS_PREF_NAME = "Quick actions"
    }
}