package be.florien.anyflow.feature.podcast.base.domain.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import be.florien.anyflow.feature.podcast.base.domain.R
import be.florien.anyflow.feature.songlist.base.domain.model.QueueItemActionType
import be.florien.anyflow.feature.songlist.base.domain.model.QueueItemFieldType
import be.florien.anyflow.feature.songlist.base.domain.model.QueueItemInfoRow
import be.florien.anyflow.tags.local.model.DownloadProgressState


enum class PodcastFieldType(
    @DrawableRes
    override val iconRes: Int,
    @StringRes val titleRes: Int
): QueueItemFieldType {
    Title(R.drawable.ic_song, R.string.info_title),
    Artist(R.drawable.ic_artist, R.string.info_artist),
    Album(R.drawable.ic_album, R.string.info_album),
    Year(R.drawable.ic_year, R.string.info_year),
    Duration(R.drawable.ic_duration, R.string.info_duration);
}

enum class PodcastActionType(
    @DrawableRes
    override val iconRes: Int,
    @StringRes val titleRes: Int
): QueueItemActionType {
    None(0, 0),
    InfoTitle(0, 0),
    ExpandableTitle(R.drawable.ic_next_occurence, 0),
    AddToFilter(R.drawable.ic_filter, R.string.info_action_filter_title),
    AddNext(R.drawable.ic_play_next, R.string.info_action_next_title),
    Search(R.drawable.ic_search, R.string.info_action_search_title),
    Download(R.drawable.ic_download, R.string.info_action_download);
}

sealed class BasePodcastInfoRow(
    override val fieldType: PodcastFieldType,
    override val actionType: PodcastActionType
): QueueItemInfoRow<PodcastFieldType, PodcastActionType> {

    interface SongMultipleInfoRow {
        val index: Int
    }

    interface SongDownload {
        val progress: LiveData<DownloadProgressState>
    }

    data class PodcastInfoRow(
        override val fieldType: PodcastFieldType,
        override val actionType: PodcastActionType,
    ) : BasePodcastInfoRow(fieldType, actionType)

    data class PodcastActionMultipleInfoRow(
        override val fieldType: PodcastFieldType,
        override val actionType: PodcastActionType,
        override val index: Int
    ) : BasePodcastInfoRow(fieldType, actionType), SongMultipleInfoRow

    data class PodcastInfoContainerRow(
        override val fieldType: PodcastFieldType,
        val subRows: List<BasePodcastInfoRow>
    ) : BasePodcastInfoRow(fieldType, PodcastActionType.ExpandableTitle)

    data class PodcastInfoMultipleContainerRow(
        override val fieldType: PodcastFieldType,
        override val index: Int,
        val subRows: List<BasePodcastInfoRow>
    ) : BasePodcastInfoRow(fieldType, PodcastActionType.ExpandableTitle), SongMultipleInfoRow

    data class PodcastDownloadMultipleInfoRow(
        override val fieldType: PodcastFieldType,
        override val actionType: PodcastActionType,
        override val index: Int,
        override val progress: LiveData<DownloadProgressState>
    ) : BasePodcastInfoRow(fieldType, actionType), SongDownload, SongMultipleInfoRow

    data class PodcastDownloadInfoRow(
        override val fieldType: PodcastFieldType,
        override val actionType: PodcastActionType,
        override val progress: LiveData<DownloadProgressState>
    ) : BasePodcastInfoRow(fieldType, actionType), SongDownload

    data class ShortcutInfoRow(
        override val fieldType: PodcastFieldType,
        override val actionType: PodcastActionType,
        val order: Int
    ) : BasePodcastInfoRow(fieldType, actionType) {
        constructor(other: BasePodcastInfoRow, order: Int) : this(
            other.fieldType,
            other.actionType,
            order
        )
    }
}