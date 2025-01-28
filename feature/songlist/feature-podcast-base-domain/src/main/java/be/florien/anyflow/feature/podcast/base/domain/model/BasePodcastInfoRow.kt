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
    Title(R.drawable.ic_podcast_episode, R.string.info_title),
    Podcast(R.drawable.ic_podcast, R.string.info_podcast),
    Description(R.drawable.ic_erase, R.string.info_description),//todo
    Publication(R.drawable.ic_year, R.string.info_publication),
    Website(R.drawable.ic_erase, R.string.info_website),//todo
    State(R.drawable.ic_erase, R.string.info_state),//todo
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
    data class PodcastInfoRow(
        override val fieldType: PodcastFieldType,
        override val actionType: PodcastActionType,
    ) : BasePodcastInfoRow(fieldType, actionType)

    data class PodcastHtmlRow(
        override val fieldType: PodcastFieldType,
        override val actionType: PodcastActionType,
    ) : BasePodcastInfoRow(fieldType, actionType)

    data class PodcastInfoContainerRow(
        override val fieldType: PodcastFieldType,
        val subRows: List<BasePodcastInfoRow>
    ) : BasePodcastInfoRow(fieldType, PodcastActionType.ExpandableTitle)

    data class PodcastDownloadInfoRow(
        override val fieldType: PodcastFieldType,
        override val actionType: PodcastActionType,
        val progress: LiveData<DownloadProgressState>
    ) : BasePodcastInfoRow(fieldType, actionType)

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