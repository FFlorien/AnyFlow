package be.florien.anyflow.feature.podcast.ui

import androidx.lifecycle.viewModelScope
import be.florien.anyflow.common.ui.data.ImageConfig
import be.florien.anyflow.feature.podcast.base.domain.BasePodcastInfoActions.Companion.DUMMY_PODCAST_ID
import be.florien.anyflow.feature.podcast.base.ui.BasePodcastViewModel
import be.florien.anyflow.feature.podcast.base.domain.model.BasePodcastInfoRow
import be.florien.anyflow.management.podcast.PodcastRepository
import be.florien.anyflow.management.podcast.model.PodcastEpisodeInfo
import be.florien.anyflow.urls.UrlRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class PodcastInfoViewModel @Inject constructor(
    private val podcastRepository: PodcastRepository,
    private val urlRepository: UrlRepository,
) : BasePodcastViewModel() {

    override var podcastId: Long
        get() {
            val value = podcastInfoMediator.value?.id
            return value ?: PodcastEpisodeInfo.dummyPodcastInfo().id
        }
        set(value) {
            viewModelScope.launch {
                if (value != DUMMY_PODCAST_ID) {
                    podcastInfoMediator.addSource(podcastRepository.getPodcastEpisode(value)) {
                        podcastInfoMediator.mutable.value = it

                        coverConfig.mutable.value = ImageConfig(
                            url = urlRepository.getPodcastArtUrl(it.podcastId),
                            resource = R.drawable.cover_placeholder
                        )
                        updateRows()
                    }
                }
            }
        }

    override fun executeAction(row: BasePodcastInfoRow): Boolean {
        val actionType = row.actionType
        val fieldType = row.fieldType
        if (super.executeAction(row)) {
            return true
        }

        viewModelScope.launch { //todo
        }
        return true
    }
}