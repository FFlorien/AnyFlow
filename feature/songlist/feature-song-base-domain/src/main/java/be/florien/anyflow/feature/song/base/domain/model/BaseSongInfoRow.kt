package be.florien.anyflow.feature.song.base.domain.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import be.florien.anyflow.feature.song.base.domain.R
import be.florien.anyflow.tags.local.model.DownloadProgressState


enum class SongFieldType(
    @DrawableRes val iconRes: Int,
    @StringRes val titleRes: Int
) {
    Title(R.drawable.ic_song, R.string.info_title),
    Track(R.drawable.ic_track, R.string.info_track),
    Artist(R.drawable.ic_artist, R.string.info_artist),
    Album(R.drawable.ic_album, R.string.info_album),
    Disk(R.drawable.ic_disk, R.string.info_disk),
    AlbumArtist(R.drawable.ic_album_artist, R.string.info_album_artist),
    Genre(R.drawable.ic_genre, R.string.info_genre),
    Playlist(R.drawable.ic_playlist, R.string.info_playlist),
    Year(R.drawable.ic_year, R.string.info_year),
    Duration(R.drawable.ic_duration, R.string.info_duration);
}

enum class SongActionType(
    @DrawableRes val iconRes: Int,
    @StringRes val titleRes: Int
) {
    None(0, 0),
    InfoTitle(0, 0),
    ExpandableTitle(R.drawable.ic_next_occurence, 0),
    AddToFilter(R.drawable.ic_filter, R.string.info_action_filter_title),
    AddToPlaylist(R.drawable.ic_add_to_playlist, R.string.info_action_select_playlist),
    AddNext(R.drawable.ic_play_next, R.string.info_action_next_title),
    Search(R.drawable.ic_search, R.string.info_action_search_title),
    Download(R.drawable.ic_download, R.string.info_action_download);
}

sealed class BaseSongInfoRow(
    open val fieldType: SongFieldType,
    open val actionType: SongActionType
) {

    interface SongMultipleInfoRow {
        val index: Int
    }

    interface SongDownload {
        val progress: LiveData<DownloadProgressState>
    }

    data class SongInfoRow(
        override val fieldType: SongFieldType,
        override val actionType: SongActionType,
    ) : BaseSongInfoRow(fieldType, actionType)

    data class SongActionMultipleInfoRow(
        override val fieldType: SongFieldType,
        override val actionType: SongActionType,
        override val index: Int
    ) : BaseSongInfoRow(fieldType, actionType), SongMultipleInfoRow

    data class SongInfoContainerRow(
        override val fieldType: SongFieldType,
        val subRows: List<BaseSongInfoRow>
    ) : BaseSongInfoRow(fieldType, SongActionType.ExpandableTitle)

    data class SongInfoMultipleContainerRow(
        override val fieldType: SongFieldType,
        override val index: Int,
        val subRows: List<BaseSongInfoRow>
    ) : BaseSongInfoRow(fieldType, SongActionType.ExpandableTitle), SongMultipleInfoRow

    data class SongDownloadMultipleInfoRow(
        override val fieldType: SongFieldType,
        override val actionType: SongActionType,
        override val index: Int,
        override val progress: LiveData<DownloadProgressState>
    ) : BaseSongInfoRow(fieldType, actionType), SongDownload, SongMultipleInfoRow

    data class SongDownloadInfoRow(
        override val fieldType: SongFieldType,
        override val actionType: SongActionType,
        override val progress: LiveData<DownloadProgressState>
    ) : BaseSongInfoRow(fieldType, actionType), SongDownload

    data class ShortcutInfoRow(
        override val fieldType: SongFieldType,
        override val actionType: SongActionType,
        val order: Int
    ) : BaseSongInfoRow(fieldType, actionType) {
        constructor(other: BaseSongInfoRow, order: Int) : this(
            other.fieldType,
            other.actionType,
            order
        )
    }
}