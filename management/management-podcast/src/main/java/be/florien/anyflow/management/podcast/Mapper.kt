package be.florien.anyflow.management.podcast

import be.florien.anyflow.management.filters.model.FilterPodcastCount
import be.florien.anyflow.management.podcast.model.PodcastDisplay
import be.florien.anyflow.tags.local.model.DbPodcastEpisode
import be.florien.anyflow.management.podcast.model.PodcastEpisodeDisplay
import be.florien.anyflow.tags.local.model.DbPodcast
import be.florien.anyflow.tags.local.model.DbPodcastDisplay
import be.florien.anyflow.tags.local.model.DbPodcastEpisodeDisplay
import be.florien.anyflow.tags.local.model.DbPodcastFilterCount

fun DbPodcastDisplay.toViewPodcast() = PodcastDisplay(
    id = id,
    name = name,
    lastUpdate = syncDate
)

fun DbPodcast.toViewPodcast() = PodcastDisplay(
    id = id,
    name = name,
    lastUpdate = syncDate
)

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
    podcasts = podcasts,
    podcastEpisodes = podcastEpisodes
)