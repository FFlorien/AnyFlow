package be.florien.anyflow.feature.podcast.base.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import be.florien.anyflow.common.ui.domain.ImageConfig
import be.florien.anyflow.component.info.InfoViewModel
import be.florien.anyflow.feature.podcast.base.domain.model.BasePodcastInfoRow
import be.florien.anyflow.feature.podcast.base.domain.model.PodcastActionType
import be.florien.anyflow.feature.podcast.base.domain.model.PodcastFieldType
import be.florien.anyflow.management.podcast.model.PodcastEpisodeInfo
import be.florien.anyflow.tags.local.model.DownloadProgressState

abstract class BasePodcastViewModel : InfoViewModel<BasePodcastInfoRow>() {
    protected val podcastInfoMediator = MediatorLiveData<PodcastEpisodeInfo>()
    val podcastInfoObservable: LiveData<PodcastEpisodeInfo> = podcastInfoMediator
    val coverConfig: LiveData<ImageConfig> = MutableLiveData()
    val podcastEpisodeInfo: PodcastEpisodeInfo
        get() {
            val value = podcastInfoMediator.value
            return value ?: PodcastEpisodeInfo.dummyPodcastInfo()
        }
    abstract var podcastId: Long

    override fun executeAction(row: BasePodcastInfoRow) = when (row.actionType) {
        PodcastActionType.ExpandableTitle -> true
        PodcastActionType.None,
        PodcastActionType.InfoTitle -> true

        else -> false
    }

    override suspend fun getInfoRowList(): MutableList<BasePodcastInfoRow> {
        //todo get shortcuts here (or in a subVM?), and pass on the order
        return listOfNotNull(
            getInfoRow(
                PodcastFieldType.Title,
                PodcastActionType.InfoTitle
            ),
            getInfoRow(
                PodcastFieldType.Podcast,
                PodcastActionType.InfoTitle
            ),
            BasePodcastInfoRow.PodcastHtmlRow(
                PodcastFieldType.Description,
                PodcastActionType.InfoTitle
            ),
            getInfoRow(
                PodcastFieldType.Publication,
                PodcastActionType.InfoTitle
            ),
            getInfoRow(
                PodcastFieldType.Duration,
                PodcastActionType.InfoTitle
            ),
            getInfoRow(
                PodcastFieldType.State,
                PodcastActionType.InfoTitle
            )
        ).toMutableList()
    }

    private fun getInfoRow(
        fieldType: PodcastFieldType,
        actionType: PodcastActionType,
        progress: LiveData<DownloadProgressState>? = null,
        shortcutIndex: Int? = null,
        subRows: List<BasePodcastInfoRow>? = null
    ): BasePodcastInfoRow = if (subRows != null) {
        BasePodcastInfoRow.PodcastInfoContainerRow(fieldType, subRows)
    } else if (shortcutIndex != null) {
        BasePodcastInfoRow.ShortcutInfoRow(fieldType, actionType, shortcutIndex)
    } else if (progress != null) {
        BasePodcastInfoRow.PodcastDownloadInfoRow(fieldType, actionType, progress)
    } else {
        BasePodcastInfoRow.PodcastInfoRow(fieldType, actionType)
    }
}