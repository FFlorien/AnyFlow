package be.florien.anyflow.feature.songlist.ui

import be.florien.anyflow.common.ui.data.TagType
import be.florien.anyflow.feature.song.base.domain.model.SongFieldType
import be.florien.anyflow.management.queue.model.PodcastEpisodeDisplay
import be.florien.anyflow.management.queue.model.SongDisplay
import be.florien.anyflow.tags.local.model.DbPodcastEpisode
import be.florien.anyflow.tags.model.SongInfo

fun SongFieldType.toTagType() = when (this) {
    SongFieldType.Title -> TagType.Title
    SongFieldType.Artist -> TagType.Artist
    SongFieldType.Album -> TagType.Album
    SongFieldType.Disk -> TagType.Disk
    SongFieldType.AlbumArtist -> TagType.AlbumArtist
    SongFieldType.Genre -> TagType.Genre
    SongFieldType.Playlist -> TagType.Playlist
    SongFieldType.Year,
    SongFieldType.Duration,
    SongFieldType.Track -> throw UnsupportedOperationException()
}

fun SongInfo.toViewDisplay() = SongDisplay(
    id = id,
    title = title,
    artistName = artistName,
    albumName = albumName,
    albumId = albumId,
    time = time
)

fun DbPodcastEpisode.toViewPodcastEpisodeDisplay() = PodcastEpisodeDisplay(
    id = id,
    title = title,
    time = time,
    artist = authorFull,
    album = authorFull,
    albumId = podcastId
)