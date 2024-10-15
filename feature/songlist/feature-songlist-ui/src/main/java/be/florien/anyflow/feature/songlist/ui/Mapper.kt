package be.florien.anyflow.feature.songlist.ui

import be.florien.anyflow.common.ui.data.TagType
import be.florien.anyflow.feature.song.base.ui.BaseSongInfoActions
import be.florien.anyflow.management.queue.model.PodcastEpisodeDisplay
import be.florien.anyflow.management.queue.model.SongDisplay
import be.florien.anyflow.tags.local.model.DbPodcastEpisode
import be.florien.anyflow.tags.model.SongInfo

fun BaseSongInfoActions.SongFieldType.toTagType() = when (this) {
    BaseSongInfoActions.SongFieldType.Title -> TagType.Title
    BaseSongInfoActions.SongFieldType.Artist -> TagType.Artist
    BaseSongInfoActions.SongFieldType.Album -> TagType.Album
    BaseSongInfoActions.SongFieldType.Disk -> TagType.Disk
    BaseSongInfoActions.SongFieldType.AlbumArtist -> TagType.AlbumArtist
    BaseSongInfoActions.SongFieldType.Genre -> TagType.Genre
    BaseSongInfoActions.SongFieldType.Playlist -> TagType.Playlist
    BaseSongInfoActions.SongFieldType.Year,
    BaseSongInfoActions.SongFieldType.Duration,
    BaseSongInfoActions.SongFieldType.PodcastEpisode,
    BaseSongInfoActions.SongFieldType.Track -> throw UnsupportedOperationException()
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