package be.florien.anyflow.management.podcast

import be.florien.anyflow.management.filters.model.FilterPodcastCount
import be.florien.anyflow.tags.local.model.DbPodcastEpisode
import be.florien.anyflow.management.podcast.model.PodcastEpisodeDisplay
import be.florien.anyflow.tags.local.model.DbPodcastFilterCount

fun DbPodcastEpisode.toViewPodcastEpisode() = PodcastEpisodeDisplay( //todo
    id = id,
    title = title,
    author = authorFull,
    album = "album",
    albumId = podcastId,
    time = time
)

fun DbPodcastFilterCount.toViewFilterCount() = FilterPodcastCount(
    podcastEpisodes = podcastEpisodes
)