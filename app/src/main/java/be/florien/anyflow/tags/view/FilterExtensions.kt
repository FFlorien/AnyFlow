package be.florien.anyflow.tags.view

import be.florien.anyflow.feature.player.services.queue.QueueRepository
import be.florien.anyflow.management.filters.model.Filter
import be.florien.anyflow.management.filters.model.Filter.FilterType
import be.florien.anyflow.tags.model.SongInfo


suspend fun Filter<*>.contains(song: SongInfo, filterRepository: QueueRepository): Boolean { //todo move elsewhere
    return when (this.type) {
        FilterType.ALBUM_ARTIST_IS -> song.albumArtistId == argument
        FilterType.ALBUM_IS -> song.albumId == argument
        FilterType.DISK_IS -> song.disk == argument
        FilterType.ARTIST_IS -> song.artistId == argument
        FilterType.GENRE_IS -> song.genreIds.any { it == argument }
        FilterType.SONG_IS -> song.id == argument
        FilterType.PLAYLIST_IS -> filterRepository.isPlaylistContainingSong(
            argument as Long,
            song.id
        )

        FilterType.DOWNLOADED_STATUS_IS -> !song.local.isNullOrBlank()
        FilterType.PODCAST_EPISODE_IS -> false
    }
}

fun Filter<*>.contains(podcastEpisode: PodcastEpisode): Boolean {
    return if (this.type == FilterType.PODCAST_EPISODE_IS) {
        podcastEpisode.id == argument
    } else {
        false
    }
}