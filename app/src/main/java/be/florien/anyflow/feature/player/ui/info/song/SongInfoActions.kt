package be.florien.anyflow.feature.player.ui.info.song

import android.content.SharedPreferences
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import be.florien.anyflow.R
import be.florien.anyflow.common.ui.data.info.InfoActions
import be.florien.anyflow.tags.local.model.DownloadProgressState
import be.florien.anyflow.tags.model.SongInfo
import be.florien.anyflow.feature.download.DownloadManager
import be.florien.anyflow.feature.player.services.queue.OrderComposer
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.model.Filter
import be.florien.anyflow.tags.UrlRepository

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
            getSongAction(infoSource, SongFieldType.Disk, SongActionType.ExpandableTitle),
            getSongAction(infoSource, SongFieldType.AlbumArtist, SongActionType.ExpandableTitle),
            *List(infoSource.genreNames.size) { index ->
                getSongAction(
                    infoSource,
                    SongFieldType.Genre,
                    SongActionType.ExpandableTitle,
                    index = index
                )
            }.toTypedArray(),
            *List(infoSource.playlistNames.size) { index ->
                getSongAction(
                    infoSource,
                    SongFieldType.Playlist,
                    SongActionType.ExpandableTitle,
                    index = index
                )
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
                getSongAction(infoSource, fieldType, SongActionType.AddToFilter),
                getSongAction(infoSource, fieldType, SongActionType.Search),
                getSongAction(infoSource, fieldType, SongActionType.AddToPlaylist),
                if (infoSource.local.isNullOrBlank()) {
                    getSongAction(infoSource, fieldType, SongActionType.Download)
                } else {
                    getSongAction(infoSource, fieldType, SongActionType.None)
                }
            )

            SongFieldType.Artist -> listOfNotNull(
                getSongAction(infoSource, fieldType, SongActionType.AddToFilter),
                getSongAction(infoSource, fieldType, SongActionType.Search),
                getSongAction(infoSource, fieldType, SongActionType.AddToPlaylist),
                getSongAction(infoSource, fieldType, SongActionType.Download)
            )

            SongFieldType.Album -> listOfNotNull(
                getSongAction(infoSource, fieldType, SongActionType.AddToFilter),
                getSongAction(infoSource, fieldType, SongActionType.Search),
                getSongAction(infoSource, fieldType, SongActionType.AddToPlaylist),
                getSongAction(infoSource, fieldType, SongActionType.Download)
            )

            SongFieldType.Disk -> listOfNotNull(
                getSongAction(infoSource, fieldType, SongActionType.AddToFilter),
                getSongAction(infoSource, fieldType, SongActionType.AddToPlaylist),
                getSongAction(infoSource, fieldType, SongActionType.Download)
            )

            SongFieldType.AlbumArtist -> listOfNotNull(
                getSongAction(infoSource, fieldType, SongActionType.AddToFilter),
                getSongAction(infoSource, fieldType, SongActionType.Search),
                getSongAction(infoSource, fieldType, SongActionType.AddToPlaylist),
                getSongAction(infoSource, fieldType, SongActionType.Download)
            )

            SongFieldType.Genre -> {
                val index = (row as SongMultipleInfoRow).index
                listOfNotNull(
                    getSongAction(infoSource, fieldType, SongActionType.AddToFilter, index = index),
                    getSongAction(infoSource, fieldType, SongActionType.Search, index = index),
                    getSongAction(
                        infoSource,
                        fieldType,
                        SongActionType.AddToPlaylist,
                        index = index
                    ),
                    getSongAction(infoSource, fieldType, SongActionType.Download, index = index)
                )
            }

            SongFieldType.Playlist -> {
                val index = (row as SongMultipleInfoRow).index
                listOfNotNull(
                    getSongAction(infoSource, fieldType, SongActionType.AddToFilter, index = index),
                    getSongAction(
                        infoSource,
                        fieldType,
                        SongActionType.AddToPlaylist,
                        index = index
                    ),
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
    fun getPodcastArtUrl(podcastId: Long) = urlRepository.getPodcastArtUrl(podcastId)

    /**
     * Action methods
     */

    suspend fun playNext(songId: Long) {
        orderComposer.changeSongPositionForNext(songId)
    }

    suspend fun filterOn(songInfo: SongInfo, row: InfoRow) {
        val filter = when (row.fieldType) {
            SongFieldType.Title -> Filter(
                Filter.FilterType.SONG_IS,
                songInfo.id,
                songInfo.title
            )

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

            SongFieldType.Disk -> Filter(
                Filter.FilterType.ALBUM_IS,
                songInfo.albumId,
                songInfo.albumName,
                listOf(
                    Filter(
                        Filter.FilterType.DISK_IS,
                        songInfo.disk,
                        songInfo.disk.toString()
                    )
                )
            )

            SongFieldType.AlbumArtist -> Filter(
                Filter.FilterType.ALBUM_ARTIST_IS,
                songInfo.albumArtistId,
                songInfo.albumArtistName
            )

            SongFieldType.Genre -> {
                val index = (row as SongMultipleInfoRow).index
                Filter(
                    Filter.FilterType.GENRE_IS,
                    songInfo.genreIds[index],
                    songInfo.genreNames[index]
                )
            }

            SongFieldType.Playlist -> {
                val index = (row as SongMultipleInfoRow).index
                Filter(
                    Filter.FilterType.PLAYLIST_IS,
                    songInfo.playlistIds[index],
                    songInfo.playlistNames[index]
                )
            }

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

    fun queueDownload(songInfo: SongInfo, fieldType: SongFieldType, index: Int?) {
        val data = when (fieldType) {
            SongFieldType.Title -> Triple(
                songInfo.id,
                Filter.FilterType.SONG_IS,
                -1
            )

            SongFieldType.Artist -> Triple(
                songInfo.artistId,
                Filter.FilterType.ARTIST_IS,
                -1
            )

            SongFieldType.Album -> Triple(
                songInfo.albumId,
                Filter.FilterType.ALBUM_IS,
                -1
            )

            SongFieldType.Disk -> Triple(
                songInfo.albumId,
                Filter.FilterType.DISK_IS,
                songInfo.disk
            )

            SongFieldType.AlbumArtist -> Triple(
                songInfo.albumArtistId,
                Filter.FilterType.ALBUM_ARTIST_IS,
                -1
            )

            SongFieldType.Genre -> {
                val trueIndex = index ?: return
                Triple(
                    songInfo.genreIds[trueIndex],
                    Filter.FilterType.GENRE_IS,
                    -1
                )
            }

            SongFieldType.Playlist -> {
                val trueIndex = index ?: return
                Triple(
                    songInfo.playlistIds[trueIndex],
                    Filter.FilterType.PLAYLIST_IS,
                    -1
                )
            }

            else -> return
        }
        downloadManager.queueDownload(data.first, data.second, data.third)
    }

    /**
     * Shortcuts
     */

    fun toggleShortcut(fieldType: FieldType, actionType: ActionType) {
        val shortcuts = getShortcuts().toMutableList()
        if (shortcuts.removeAll { it.fieldType == fieldType && it.actionType == actionType }) {
            sharedPreferences.edit()
                .putString(
                    SHORTCUTS_PREF_NAME,
                    shortcuts.joinToString(separator = "#") {
                        val fieldName = (it.fieldType as Enum<*>).name
                        val actionName = (it.actionType as Enum<*>).name
                        "$fieldName|$actionName"
                    }
                )
                .apply()
        } else {
            val fieldName = (fieldType as Enum<*>).name
            val actionName = (actionType as Enum<*>).name
            val originalString = sharedPreferences.getString(SHORTCUTS_PREF_NAME, "")
            sharedPreferences.edit()
                .putString(SHORTCUTS_PREF_NAME, "$originalString#$fieldName|$actionName")
                .apply()
        }
    }

    fun getShortcuts(): List<ShortcutInfoRow> {
        val string = sharedPreferences.getString(SHORTCUTS_PREF_NAME, "") ?: return emptyList()
        val songInfo = SongInfo.dummySongInfo()

        return string.split("#").filter { it.isNotEmpty() }.mapIndexedNotNull { index, it ->
            val fieldTypeString = it.substringBefore('|')
            val actionTypeString = it.substringAfter('|')

            val fieldType = SongFieldType.entries.firstOrNull { it.name == fieldTypeString }
            if (fieldType != null) {
                val actionType = SongActionType.entries.firstOrNull { it.name == actionTypeString }
                if (actionType != null) {
                    getSongAction(
                        songInfo,
                        fieldType,
                        actionType,
                        order = index
                    ) as? ShortcutInfoRow
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

                SongFieldType.Disk -> getInfoRow(
                    R.string.info_disk,
                    songInfo.disk.toString(),
                    null,
                    SongFieldType.Disk,
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

                SongFieldType.Genre -> getInfoRow(
                    R.string.info_genre,
                    songInfo.genreNames[index],
                    null,
                    SongFieldType.Genre,
                    SongActionType.ExpandableTitle,
                    order = order,
                    index = index
                )

                SongFieldType.Playlist -> getInfoRow(
                    R.string.info_playlist,
                    songInfo.playlistNames[index],
                    null,
                    SongFieldType.Playlist,
                    SongActionType.ExpandableTitle,
                    order = order,
                    index = index
                )

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
            ) else null //todo Change to Delete from local behaviour
            // if not present -> download
            // if partially present (and not song) -> both actions (download & delete)
            // if complete -> delete from local
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

                SongFieldType.Disk -> getInfoRow(
                    R.string.info_action_filter_title,
                    "${songInfo.albumName} #${songInfo.disk}",
                    R.string.info_action_filter_on,
                    SongFieldType.Disk,
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

                SongFieldType.Playlist -> getInfoRow(
                    R.string.info_action_filter_title,
                    songInfo.playlistNames[index],
                    R.string.info_action_filter_on,
                    SongFieldType.Playlist,
                    SongActionType.AddToFilter,
                    order = order,
                    index = index
                )

                else -> null
            }

            SongActionType.AddToPlaylist -> when (field) {
                SongFieldType.Title -> getInfoRow(
                    R.string.info_action_select_playlist,
                    songInfo.title,
                    R.string.info_action_select_playlist_detail,
                    SongFieldType.Title,
                    SongActionType.AddToPlaylist,
                    order = order
                )

                SongFieldType.Artist -> getInfoRow(
                    R.string.info_action_select_playlist,
                    songInfo.artistName,
                    R.string.info_action_select_playlist_detail,
                    SongFieldType.Artist,
                    SongActionType.AddToPlaylist,
                    order = order
                )

                SongFieldType.Album -> getInfoRow(
                    R.string.info_action_select_playlist,
                    songInfo.albumName,
                    R.string.info_action_select_playlist_detail,
                    SongFieldType.Album,
                    SongActionType.AddToPlaylist,
                    order = order
                )

                SongFieldType.Disk -> getInfoRow(
                    R.string.info_action_select_playlist,
                    "${songInfo.albumName} #${songInfo.disk}",
                    R.string.info_action_select_playlist_detail,
                    SongFieldType.Disk,
                    SongActionType.AddToPlaylist,
                    order = order
                )

                SongFieldType.AlbumArtist -> getInfoRow(
                    R.string.info_action_select_playlist,
                    songInfo.albumArtistName,
                    R.string.info_action_select_playlist_detail,
                    SongFieldType.AlbumArtist,
                    SongActionType.AddToPlaylist,
                    order = order
                )

                SongFieldType.Genre -> getInfoRow(
                    R.string.info_action_select_playlist,
                    songInfo.genreNames[index],
                    R.string.info_action_select_playlist_detail,
                    SongFieldType.Genre,
                    SongActionType.AddToPlaylist,
                    order = order
                )

                SongFieldType.Playlist -> getInfoRow(
                    R.string.info_action_select_playlist,
                    songInfo.playlistNames[index],
                    R.string.info_action_select_playlist_detail,
                    SongFieldType.Playlist,
                    SongActionType.AddToPlaylist,
                    order = order
                )

                else -> null
            }

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
                    progress = downloadManager.getDownloadState(
                        songInfo.id,
                        Filter.FilterType.SONG_IS
                    )
                )

                SongFieldType.Album -> getInfoRow(
                    R.string.info_action_download,
                    songInfo.albumName,
                    R.string.info_action_download_description,
                    SongFieldType.Album,
                    SongActionType.Download,
                    order = order,
                    progress = downloadManager.getDownloadState(
                        songInfo.albumId,
                        Filter.FilterType.ALBUM_IS
                    )
                )

                SongFieldType.Disk -> getInfoRow(
                    R.string.info_action_download,
                    "${songInfo.albumName} #${songInfo.disk}",
                    R.string.info_action_download_description,
                    SongFieldType.Disk,
                    SongActionType.Download,
                    order = order,
                    progress = downloadManager.getDownloadState(
                        songInfo.albumId,
                        Filter.FilterType.DISK_IS,
                        songInfo.disk
                    )
                )

                SongFieldType.AlbumArtist -> getInfoRow(
                    R.string.info_action_download,
                    songInfo.albumArtistName,
                    R.string.info_action_download_description,
                    SongFieldType.AlbumArtist,
                    SongActionType.Download,
                    order = order,
                    progress = downloadManager.getDownloadState(
                        songInfo.albumArtistId,
                        Filter.FilterType.ALBUM_ARTIST_IS
                    )
                )

                SongFieldType.Artist -> getInfoRow(
                    R.string.info_action_download,
                    songInfo.artistName,
                    R.string.info_action_download_description,
                    SongFieldType.Artist,
                    SongActionType.Download,
                    order = order,
                    progress = downloadManager.getDownloadState(
                        songInfo.artistId,
                        Filter.FilterType.ARTIST_IS
                    )
                )

                SongFieldType.Genre -> getInfoRow(
                    R.string.info_action_download,
                    songInfo.genreNames[index],
                    R.string.info_action_download_description,
                    SongFieldType.Genre,
                    SongActionType.Download,
                    order = order,
                    progress = downloadManager.getDownloadState(
                        songInfo.genreIds[index],
                        Filter.FilterType.GENRE_IS
                    ),
                    index = index
                )

                SongFieldType.Playlist -> getInfoRow(
                    R.string.info_action_download,
                    songInfo.playlistNames[index],
                    R.string.info_action_download_description,
                    SongFieldType.Playlist,
                    SongActionType.Download,
                    order = order,
                    progress = downloadManager.getDownloadState(
                        songInfo.playlistIds[index],
                        Filter.FilterType.PLAYLIST_IS
                    ),
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
        progress: LiveData<DownloadProgressState>? = null,
        order: Int? = null,
        index: Int? = null
    ): InfoRow = if (index != null && progress != null) {
        SongDownloadMultipleInfoRow(title, text, textRes, fieldType, actionType, index, progress)
    } else if (index != null) {
        SongActionMultipleInfoRow(title, text, textRes, fieldType, actionType, index)
    } else if (order != null) {
        ShortcutInfoRow(title, text, textRes, fieldType, actionType, order)
    } else if (progress != null) {
        SongDownloadInfoRow(title, text, textRes, fieldType, actionType, progress)
    } else {
        SongInfoRow(title, text, textRes, fieldType, actionType)
    }

    enum class SongFieldType(
        @DrawableRes override val iconRes: Int
    ) : FieldType {
        Title(R.drawable.ic_song),
        Track(R.drawable.ic_track),
        Artist(R.drawable.ic_artist),
        Album(R.drawable.ic_album),
        Disk(R.drawable.ic_disk),
        AlbumArtist(R.drawable.ic_album_artist),
        Genre(R.drawable.ic_genre),
        Playlist(R.drawable.ic_playlist),
        Year(R.drawable.ic_year),
        Duration(R.drawable.ic_duration),
        PodcastEpisode(R.drawable.ic_duration);
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

    abstract class SongMultipleInfoRow(
        @StringRes override val title: Int,
        override val text: String?,
        @StringRes override val textRes: Int?,
        override val fieldType: FieldType,
        override val actionType: ActionType,
        open val index: Int
    ) : InfoRow(title, text, textRes, fieldType, actionType, null) {
        override fun isExpandableForItem(other: InfoRow): Boolean =
            actionType == SongActionType.ExpandableTitle
                    && fieldType == other.fieldType
                    && (other as? SongMultipleInfoRow)?.index == index
    }

    interface SongDownload {
        val progress: LiveData<DownloadProgressState>
    }

    data class SongInfoRow(
        @StringRes override val title: Int,
        override val text: String?,
        @StringRes override val textRes: Int?,
        override val fieldType: FieldType,
        override val actionType: ActionType
    ) : InfoRow(title, text, textRes, fieldType, actionType, null) {
        override fun isExpandableForItem(other: InfoRow): Boolean =
            actionType == SongActionType.ExpandableTitle
                    && fieldType == other.fieldType
    }

    data class SongActionMultipleInfoRow(
        @StringRes override val title: Int,
        override val text: String?,
        @StringRes override val textRes: Int?,
        override val fieldType: FieldType,
        override val actionType: ActionType,
        override val index: Int
    ) : SongMultipleInfoRow(title, text, textRes, fieldType, actionType, index)

    data class SongDownloadMultipleInfoRow(
        @StringRes override val title: Int,
        override val text: String?,
        @StringRes override val textRes: Int?,
        override val fieldType: FieldType,
        override val actionType: ActionType,
        override val index: Int,
        override val progress: LiveData<DownloadProgressState>
    ) : SongMultipleInfoRow(title, text, textRes, fieldType, actionType, index), SongDownload

    data class SongDownloadInfoRow(
        @StringRes override val title: Int,
        override val text: String?,
        @StringRes override val textRes: Int?,
        override val fieldType: FieldType,
        override val actionType: ActionType,
        override val progress: LiveData<DownloadProgressState>
    ) : InfoRow(title, text, textRes, fieldType, actionType, null), SongDownload {
        override fun areContentTheSame(other: InfoRow): Boolean =
            super.areContentTheSame(other) && other is SongDownloadInfoRow && other.progress === progress
    }

    data class ShortcutInfoRow(
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
            super.areContentTheSame(other) && other is ShortcutInfoRow && other.order == order

    }

    companion object {
        const val DUMMY_SONG_ID = -5L
        private const val SHORTCUTS_PREF_NAME = "Shortcuts"
    }
}