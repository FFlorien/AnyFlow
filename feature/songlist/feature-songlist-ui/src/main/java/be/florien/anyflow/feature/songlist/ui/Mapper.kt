package be.florien.anyflow.feature.songlist.ui

import androidx.core.text.HtmlCompat
import be.florien.anyflow.common.ui.data.TagType
import be.florien.anyflow.feature.song.base.domain.model.SongFieldType
import be.florien.anyflow.management.queue.model.PodcastEpisodeDisplay
import be.florien.anyflow.management.queue.model.SongDisplay
import be.florien.anyflow.tags.local.model.DbPodcastEpisodeDisplay
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

fun DbPodcastEpisodeDisplay.toViewPodcastEpisodeDisplay() = PodcastEpisodeDisplay(
    id = id,
    title = title,
    time = time,
    podcast = podcastName,
    podcastId = podcastId,
    description = HtmlCompat.fromHtml(
        HtmlCompat.fromHtml(description, HtmlCompat.FROM_HTML_MODE_COMPACT).toString(),
        HtmlCompat.FROM_HTML_MODE_COMPACT
    ).toString()
)