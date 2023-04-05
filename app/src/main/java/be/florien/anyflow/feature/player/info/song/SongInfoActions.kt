package be.florien.anyflow.feature.player.info.song

import android.content.SharedPreferences
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import be.florien.anyflow.R
import be.florien.anyflow.data.DownloadManager
import be.florien.anyflow.data.UrlRepository
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.feature.player.info.InfoActions
import be.florien.anyflow.player.FiltersManager
import be.florien.anyflow.player.OrderComposer

class SongInfoActions(
    private val filtersManager: FiltersManager,
    private val orderComposer: OrderComposer,
    private val urlRepository: UrlRepository,
    private val sharedPreferences: SharedPreferences,
    private val downloadManager: DownloadManager
) : InfoActions<SongInfo>() {

    /**
     * Overridden methods
     */

    override suspend fun getInfoRows(infoSource: SongInfo): List<InfoRow> {
        return listOfNotNull(
            getSongAction(infoSource, SongFieldType.Title, SongActionType.ExpandableTitle),
            getSongAction(infoSource, SongFieldType.Artist, SongActionType.ExpandableTitle),
            getSongAction(infoSource, SongFieldType.Track, SongActionType.InfoTitle),
            getSongAction(infoSource, SongFieldType.Album, SongActionType.ExpandableTitle),
            getSongAction(infoSource, SongFieldType.AlbumArtist, SongActionType.ExpandableTitle),
            *List(infoSource.genreNames.size) { index ->
                getSongAction(infoSource, SongFieldType.Genre, SongActionType.ExpandableTitle, index = index)
            }.toTypedArray(),
            getSongAction(infoSource, SongFieldType.Duration, SongActionType.InfoTitle),
            getSongAction(infoSource, SongFieldType.Year, SongActionType.InfoTitle)
        )
    }

    override suspend fun getActionsRows(
        infoSource: SongInfo,
        row: InfoRow
    ): List<InfoRow> {
        val fieldType = row.fieldType
        if (fieldType !is SongFieldType) {
            return emptyList()
        }
        return when (fieldType) {
            SongFieldType.Title -> listOfNotNull(
                getSongAction(infoSource, fieldType, SongActionType.AddNext),
                getSongAction(infoSource, fieldType, SongActionType.AddToPlaylist),
                getSongAction(infoSource, fieldType, SongActionType.AddToFilter),
                getSongAction(infoSource, fieldType, SongActionType.Search),
                if (infoSource.local.isNullOrBlank()) {
                    getSongAction(infoSource, fieldType, SongActionType.Download)
                } else {
                    getSongAction(infoSource, fieldType, SongActionType.None)
                }
            )
            SongFieldType.Artist -> listOfNotNull(
                getSongAction(infoSource, fieldType, SongActionType.AddToFilter),
                getSongAction(infoSource, fieldType, SongActionType.Search),
                getSongAction(infoSource, fieldType, SongActionType.Download)
            )
            SongFieldType.Album -> listOfNotNull(
                getSongAction(infoSource, fieldType, SongActionType.AddToFilter),
                getSongAction(infoSource, fieldType, SongActionType.Search),
                getSongAction(infoSource, fieldType, SongActionType.Download)
            )
            SongFieldType.AlbumArtist -> listOfNotNull(
                getSongAction(infoSource, fieldType, SongActionType.AddToFilter),
                getSongAction(infoSource, fieldType, SongActionType.Search),
                getSongAction(infoSource, fieldType, SongActionType.Download)
            )
            SongFieldType.Genre -> {
                val index = (row as SongMultipleInfoRow).index
                listOfNotNull(
                    getSongAction(infoSource, fieldType, SongActionType.AddToFilter, index = index),
                    getSongAction(infoSource, fieldType, SongActionType.Search, index = index),
                    getSongAction(infoSource, fieldType, SongActionType.Download, index = index)
                )
            }
            else -> listOf()
        }
    }

    /**
     * Public methods
     */

    fun getAlbumArtUrl(albumId: Long) = urlRepository.getAlbumArtUrl(albumId)

    /**
     * Action methods
     */

    suspend fun playNext(songId: Long) {
        orderComposer.changeSongPositionForNext(songId)
    }

    suspend fun filterOn(songInfo: SongInfo, row: InfoRow) {
        val filter = when (row.fieldType) {
            SongFieldType.Title -> Filter(Filter.FilterType.SONG_IS, songInfo.id, songInfo.title)
            SongFieldType.Artist -> Filter(
                Filter.FilterType.ARTIST_IS,
                songInfo.artistId,
                songInfo.artistName
            )
            SongFieldType.Album -> Filter(
                Filter.FilterType.ALBUM_IS,
                songInfo.albumId,
                songInfo.albumName
            )
            SongFieldType.AlbumArtist -> Filter(
                Filter.FilterType.ALBUM_ARTIST_IS,
                songInfo.albumArtistId,
                songInfo.albumArtistName
            )
            SongFieldType.Genre -> {
                val index  = (row as SongMultipleInfoRow).index
                Filter(
                    Filter.FilterType.GENRE_IS,
                    songInfo.genreIds[index],
                    songInfo.genreNames[index]
                )
            }
            //todo display multiple playlists in info view
            else -> throw IllegalArgumentException("This field can't be filtered on")
        }
        filtersManager.clearFilters()
        filtersManager.addFilter(filter)
        filtersManager.commitChanges()
    }

    fun getSearchTerms(songInfo: SongInfo, fieldType: FieldType): String {
        return when (fieldType) {
            SongFieldType.Title -> songInfo.title
            SongFieldType.Artist -> songInfo.artistName
            SongFieldType.Album -> songInfo.albumName
            SongFieldType.AlbumArtist -> songInfo.albumArtistName
            SongFieldType.Genre -> songInfo.genreNames.first()
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
                    quickActions.joinToString(separator = "#") {
                        val fieldName = (it.fieldType as Enum<*>).name
                        val actionName = (it.actionType as Enum<*>).name
                        "$fieldName|$actionName"
                    }
                )
                .apply()
        } else {
            val fieldName = (fieldType as Enum<*>).name
            val actionName = (actionType as Enum<*>).name
            val originalString = sharedPreferences.getString(QUICK_ACTIONS_PREF_NAME, "")
            sharedPreferences.edit()
                .putString(QUICK_ACTIONS_PREF_NAME, "$originalString#$fieldName|$actionName")
                .apply()
        }
    }

    fun getQuickActions(): List<QuickActionInfoRow> {
        val string = sharedPreferences.getString(QUICK_ACTIONS_PREF_NAME, "") ?: return emptyList()
        val songInfo = SongInfo.dummySongInfo()

        return string.split("#").filter { it.isNotEmpty() }.mapIndexedNotNull { index, it ->
            val fieldTypeString = it.substringBefore('|')
            val actionTypeString = it.substringAfter('|')

            val fieldType = SongFieldType.values().firstOrNull { it.name == fieldTypeString }
            if (fieldType != null) {
                val actionType = SongActionType.values().firstOrNull { it.name == actionTypeString }
                if (actionType != null) {
                    getSongAction(
                        songInfo,
                        fieldType,
                        actionType,
                        order = index
                    ) as? QuickActionInfoRow
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
        order: Int? = null,
        index: Int = 0
    ): InfoRow? {
        if (action !is SongActionType || field !is SongFieldType) {
            return null
        }
        return when (action) {
            SongActionType.ExpandableTitle -> when (field) {
                SongFieldType.Title -> getInfoRow(
                    R.string.info_title,
                    songInfo.title,
                    null,
                    SongFieldType.Title,
                    SongActionType.ExpandableTitle,
                    order = order
                )
                SongFieldType.Artist -> getInfoRow(
                    R.string.info_artist,
                    songInfo.artistName,
                    null,
                    SongFieldType.Artist,
                    SongActionType.ExpandableTitle,
                    order = order
                )
                SongFieldType.Album -> getInfoRow(
                    R.string.info_album,
                    songInfo.albumName,
                    null,
                    SongFieldType.Album,
                    SongActionType.ExpandableTitle,
                    order = order
                )
                SongFieldType.AlbumArtist -> getInfoRow(
                    R.string.info_album_artist,
                    songInfo.albumArtistName,
                    null,
                    SongFieldType.AlbumArtist,
                    SongActionType.ExpandableTitle,
                    order = order
                )
                SongFieldType.Genre -> {
                    getInfoRow(
                        R.string.info_genre,
                        songInfo.genreNames[index],
                        null,
                        SongFieldType.Genre,
                        SongActionType.ExpandableTitle,
                        order = order,
                        index = index
                    )
                }
                else -> null
            }
            SongActionType.InfoTitle -> when (field) {
                SongFieldType.Track -> getInfoRow(
                    R.string.info_track,
                    songInfo.track.toString(),
                    null,
                    SongFieldType.Track,
                    SongActionType.InfoTitle,
                    order = order
                )
                SongFieldType.Duration -> getInfoRow(
                    R.string.info_duration,
                    songInfo.timeText,
                    null,
                    SongFieldType.Duration,
                    SongActionType.InfoTitle,
                    order = order
                )
                SongFieldType.Year -> getInfoRow(
                    R.string.info_year,
                    songInfo.year.toString(),
                    null,
                    SongFieldType.Year,
                    SongActionType.InfoTitle,
                    order = order
                )
                else -> null
            }
            SongActionType.None -> if (field == SongFieldType.Title) getInfoRow(
                R.string.info_action_downloaded,
                null,
                R.string.info_action_downloaded_description,
                SongFieldType.Title,
                SongActionType.None,
                order = order
            ) else null
            SongActionType.AddToFilter -> when (field) {
                SongFieldType.Title -> getInfoRow(
                    R.string.info_action_filter_title,
                    songInfo.title,
                    R.string.info_action_filter_on,
                    SongFieldType.Title,
                    SongActionType.AddToFilter,
                    order = order
                )
                SongFieldType.Artist -> getInfoRow(
                    R.string.info_action_filter_title,
                    songInfo.artistName,
                    R.string.info_action_filter_on,
                    SongFieldType.Artist,
                    SongActionType.AddToFilter,
                    order = order
                )
                SongFieldType.Album -> getInfoRow(
                    R.string.info_action_filter_title,
                    songInfo.albumName,
                    R.string.info_action_filter_on,
                    SongFieldType.Album,
                    SongActionType.AddToFilter,
                    order = order
                )
                SongFieldType.AlbumArtist -> getInfoRow(
                    R.string.info_action_filter_title,
                    songInfo.albumArtistName,
                    R.string.info_action_filter_on,
                    SongFieldType.AlbumArtist,
                    SongActionType.AddToFilter,
                    order = order
                )
                SongFieldType.Genre -> getInfoRow(
                    R.string.info_action_filter_title,
                    songInfo.genreNames[index],
                    R.string.info_action_filter_on,
                    SongFieldType.Genre,
                    SongActionType.AddToFilter,
                    order = order,
                    index = index
                )
                else -> null
            }
            SongActionType.AddToPlaylist -> if (field == SongFieldType.Title) getInfoRow(
                R.string.info_action_select_playlist,
                null,
                R.string.info_action_select_playlist_detail,
                SongFieldType.Title,
                SongActionType.AddToPlaylist,
                order = order
            ) else null
            SongActionType.AddNext -> if (field == SongFieldType.Title) getInfoRow(
                R.string.info_action_next_title,
                null,
                R.string.info_action_track_next,
                SongFieldType.Title,
                SongActionType.AddNext,
                order = order
            ) else null
            SongActionType.Search -> when (field) {
                SongFieldType.Title -> getInfoRow(
                    R.string.info_action_search_title,
                    songInfo.title,
                    R.string.info_action_search_on,
                    SongFieldType.Title,
                    SongActionType.Search,
                    order = order
                )
                SongFieldType.Artist -> getInfoRow(
                    R.string.info_action_search_title,
                    songInfo.artistName,
                    R.string.info_action_search_on,
                    SongFieldType.Artist,
                    SongActionType.Search,
                    order = order
                )
                SongFieldType.Album -> getInfoRow(
                    R.string.info_action_search_title,
                    songInfo.albumName,
                    R.string.info_action_search_on,
                    SongFieldType.Album,
                    SongActionType.Search,
                    order = order
                )
                SongFieldType.AlbumArtist -> getInfoRow(
                    R.string.info_action_search_title,
                    songInfo.albumArtistName,
                    R.string.info_action_search_on,
                    SongFieldType.AlbumArtist,
                    SongActionType.Search,
                    order = order
                )
                else -> null
            }
            SongActionType.Download -> when (field) {
                SongFieldType.Title -> getInfoRow(
                    R.string.info_action_download,
                    songInfo.title,
                    R.string.info_action_download_description,
                    SongFieldType.Title,
                    SongActionType.Download,
                    order = order,
                    progress = downloadManager.getDownloadState(songInfo)
                )
                SongFieldType.Album -> getInfoRow(
                    R.string.info_action_download,
                    songInfo.albumName,
                    R.string.info_action_download_description,
                    SongFieldType.Album,
                    SongActionType.Download,
                    order = order,
                    progress = downloadManager.getDownloadState(songInfo)//todo
                )
                SongFieldType.AlbumArtist -> getInfoRow(
                    R.string.info_action_download,
                    songInfo.albumArtistName,
                    R.string.info_action_download_description,
                    SongFieldType.AlbumArtist,
                    SongActionType.Download,
                    order = order,
                    progress = downloadManager.getDownloadState(songInfo)//todo
                )
                SongFieldType.Artist -> getInfoRow(
                    R.string.info_action_download,
                    songInfo.artistName,
                    R.string.info_action_download_description,
                    SongFieldType.Artist,
                    SongActionType.Download,
                    order = order,
                    progress = downloadManager.getDownloadState(songInfo)//todo
                )
                SongFieldType.Genre -> getInfoRow(
                    R.string.info_action_download,
                    songInfo.genreNames[index],
                    R.string.info_action_download_description,
                    SongFieldType.Genre,
                    SongActionType.Download,
                    order = order,
                    progress = downloadManager.getDownloadState(songInfo),//todo
                            index = index
                )
                else -> null
            }
        }
    }

    private fun getInfoRow(
        title: Int,
        text: String?,
        textRes: Int?,
        fieldType: FieldType,
        actionType: ActionType,
        progress: LiveData<Int>? = null,
        order: Int? = null,
        index: Int? = null
    ): InfoRow = if (index != null) {
        SongMultipleInfoRow(title, text, textRes, fieldType, actionType, index)
    } else if (order != null) {
        QuickActionInfoRow(title, text, textRes, fieldType, actionType, order)
    } else if (progress != null) {
        SongDownloadInfoRow(title, text, textRes, fieldType, actionType, progress)
    } else {
        SongInfoRow(title, text, textRes, fieldType, actionType)
    }

    enum class SongFieldType(
        @DrawableRes override val iconRes: Int,
        override val couldGetUrl: Boolean
    ) : FieldType {
        Title(R.drawable.ic_song, false),
        Track(R.drawable.ic_track, false),
        Artist(R.drawable.ic_artist, false),
        Album(R.drawable.ic_album, false),
        AlbumArtist(R.drawable.ic_album_artist, false),
        Genre(R.drawable.ic_genre, false),
        Year(R.drawable.ic_year, false),
        Duration(R.drawable.ic_duration, false);
    }

    enum class SongActionType(
        @DrawableRes override val iconRes: Int,
        override val category: ActionTypeCategory
    ) : ActionType {
        None(0, ActionTypeCategory.None),
        InfoTitle(0, ActionTypeCategory.None),
        ExpandableTitle(R.drawable.ic_next_occurence, ActionTypeCategory.Navigation),
        AddToFilter(R.drawable.ic_filter, ActionTypeCategory.Action),
        AddToPlaylist(R.drawable.ic_add_to_playlist, ActionTypeCategory.Action),
        AddNext(R.drawable.ic_play_next, ActionTypeCategory.Action),
        Search(R.drawable.ic_search, ActionTypeCategory.Action),
        Download(R.drawable.ic_download, ActionTypeCategory.Action);
    }

    data class SongInfoRow(
        @StringRes override val title: Int,
        override val text: String?,
        @StringRes override val textRes: Int?,
        override val fieldType: FieldType,
        override val actionType: ActionType
    ) : InfoRow(title, text, textRes, fieldType, actionType, null)

    data class SongMultipleInfoRow(
        @StringRes override val title: Int,
        override val text: String?,
        @StringRes override val textRes: Int?,
        override val fieldType: FieldType,
        override val actionType: ActionType,
        val index: Int
    ) : InfoRow(title, text, textRes, fieldType, actionType, null)

    data class SongDownloadInfoRow(
        @StringRes override val title: Int,
        override val text: String?,
        @StringRes override val textRes: Int?,
        override val fieldType: FieldType,
        override val actionType: ActionType,
        val progress: LiveData<Int>
    ) : InfoRow(title, text, textRes, fieldType, actionType, null) {
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
    ) : InfoRow(title, text, textRes, fieldType, actionType, null) {
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