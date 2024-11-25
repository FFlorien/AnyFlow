package be.florien.anyflow.management.podcast

import be.florien.anyflow.management.filters.model.FilterPodcastCount
import be.florien.anyflow.tags.local.model.DbPodcastEpisode
import be.florien.anyflow.management.podcast.model.PodcastEpisodeDisplay
import be.florien.anyflow.tags.local.model.DbPodcastEpisodeDisplay
import be.florien.anyflow.tags.local.model.DbPodcastFilterCount

fun DbPodcastEpisode.toViewPodcastEpisode() = PodcastEpisodeDisplay(
    id = id,
    title = title,
    podcast = "",
    podcastId = podcastId,
    time = time,
)

fun DbPodcastEpisodeDisplay.toViewPodcastEpisode() = PodcastEpisodeDisplay(
    id = id,
    title = title,
    podcast = podcastName,
    podcastId = podcastId,
    time = time
)

fun DbPodcastFilterCount.toViewFilterCount() = FilterPodcastCount(
    podcastEpisodes = podcastEpisodes
)