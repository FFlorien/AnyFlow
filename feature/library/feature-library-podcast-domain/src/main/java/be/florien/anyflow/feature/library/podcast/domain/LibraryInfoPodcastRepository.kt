package be.florien.anyflow.feature.library.podcast.domain

import be.florien.anyflow.feature.library.domain.LibraryInfoRepository
import be.florien.anyflow.feature.library.domain.model.IdText
import be.florien.anyflow.feature.library.domain.model.LibraryActionType
import be.florien.anyflow.feature.library.domain.model.LibraryFieldType
import be.florien.anyflow.feature.library.domain.model.LibraryInfoRow
import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.management.filters.domain.model.FilterType
import be.florien.anyflow.management.filters.domain.model.PodcastFilterType
import be.florien.anyflow.management.filters.domain.model.TagFilterType
import be.florien.anyflow.management.podcast.PodcastRepository
import be.florien.anyflow.management.podcast.model.PodcastDisplay
import be.florien.anyflow.management.podcast.model.PodcastEpisodeDisplay
import be.florien.anyflow.urls.UrlRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LibraryInfoPodcastRepository @Inject constructor(
    private val urlRepository: UrlRepository,
    private val podcastRepository: PodcastRepository
) : LibraryInfoRepository {

    override fun getArtUrl(artType: String?, id: Long): String? =
        if (artType == null) {
            null
        } else {
            urlRepository.getArtUrl(artType, id)
        }

    override suspend fun getFilteredInfo(
        filterType: FilterType,
        filter: Filter?
    ): IdText? = when (filterType) {
        PodcastFilterType.PODCAST_IS ->
            podcastRepository
                .getAllPodcastsList()
                .map(PodcastDisplay::toIdText)

        PodcastFilterType.PODCAST_EPISODE_IS ->
            podcastRepository
                .getAllPodcastsEpisodesList()
                .map(PodcastEpisodeDisplay::toIdText)

        PodcastFilterType.STATE_IS,
        TagFilterType.SONG_IS,
        TagFilterType.ARTIST_IS,
        TagFilterType.ALBUM_ARTIST_IS,
        TagFilterType.ALBUM_IS,
        TagFilterType.GENRE_IS,
        TagFilterType.PLAYLIST_IS,
        TagFilterType.DOWNLOADED_STATUS_IS,
        TagFilterType.DISK_IS -> emptyList()//todo
    }.firstOrNull()

    override suspend fun getInfoRowList(filter: Filter?): MutableList<LibraryInfoRow> {
        val count =
            withContext(Dispatchers.IO) { podcastRepository.getFilteredInfo(filter) }
        return mutableListOf(
            LibraryInfoRow(
                LibraryFieldType.Podcast.Podcast,
                getAction(count.podcasts),
                count.podcasts
            ),
            LibraryInfoRow(
                LibraryFieldType.Podcast.PodcastEpisode,
                getAction(count.podcastEpisodes),
                count.podcastEpisodes
            )
        )
    }

    private fun getAction(count: Int) = if (count > 1) LibraryActionType.SubFilter else LibraryActionType.InfoTitle
}