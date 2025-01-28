package be.florien.anyflow.management.podcast

import be.florien.anyflow.management.filters.model.FilterPodcastCount
import be.florien.anyflow.management.podcast.model.PodcastDisplay
import be.florien.anyflow.tags.local.model.DbPodcastEpisode
import be.florien.anyflow.management.podcast.model.PodcastEpisodeDisplay
import be.florien.anyflow.management.podcast.model.PodcastEpisodeInfo
import be.florien.anyflow.tags.local.model.DbPodcast
import be.florien.anyflow.tags.local.model.DbPodcastDisplay
import be.florien.anyflow.tags.local.model.DbPodcastEpisodeDisplay
import be.florien.anyflow.tags.local.model.DbPodcastEpisodeWithPodcast
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

fun DbPodcastEpisodeWithPodcast.toViewPodcastEpisode() = PodcastEpisodeInfo(
    episode.id,
    episode.title,
    episode.state,
    episode.time,
    episode.playCount,
    episode.played,
    null,
    episode.description,
    episode.website,
    episode.publicationDate,
    episode.podcastId,
    podcast.name,
    podcast.description,
    episode.category,
    podcast.website,
    podcast.syncDate,
    null
)