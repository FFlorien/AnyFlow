package be.florien.anyflow.management.podcast

import be.florien.anyflow.tags.local.model.DbPodcastEpisode
import be.florien.anyflow.management.podcast.model.PodcastEpisodeDisplay

fun DbPodcastEpisode.toViewPodcastEpisode() = PodcastEpisodeDisplay( //todo
    id = id,
    title = title,
    author = authorFull,
    album = "album",
    albumId = podcastId,
    time = time
)