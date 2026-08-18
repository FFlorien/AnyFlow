package be.florien.anyflow.feature.mediaList.ui

import androidx.core.text.HtmlCompat
import be.florien.anyflow.common.ui.domain.TagType
import be.florien.anyflow.common.utils.TimeOperations
import be.florien.anyflow.feature.song.base.domain.model.SongFieldType
import be.florien.anyflow.management.queue.model.PodcastEpisodeDisplay
import be.florien.anyflow.management.queue.model.SongDisplay
import be.florien.anyflow.tags.local.model.DbPodcastEpisodeDisplay
import be.florien.anyflow.tags.local.model.DbQueueItemDisplay
import be.florien.anyflow.tags.local.model.PODCAST_CHAPTER_MEDIA_TYPE
import be.florien.anyflow.tags.local.model.PODCAST_MEDIA_TYPE
import be.florien.anyflow.tags.local.model.SONG_MEDIA_TYPE
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
        description,
        HtmlCompat.FROM_HTML_MODE_COMPACT
    ).toString()
)

fun DbQueueItemDisplay.toMediaItemData(
    getSongArtUrl: (Long) -> String,
    getPodcastArtUrl: (Long) -> String
): MediaItemData {
    val songIdNS = songId
    val songTitleNS = songTitle
    val songArtistNameNS = songArtistName
    val songAlbumNameNS = songAlbumName
    val songAlbumIdNS = songAlbumId
    val songTimeNS = songTime
    val podcastEpisodeIdNS = podcastEpisodeId
    val podcastTitleNS = podcastTitle
    val podcastTimeNS = podcastTime
    val podcastIdNS = podcastId
    val podcastNameNS = podcastName
    val podcastDescriptionNS = podcastDescription
    val podcastChapterIdNS = podcastChapterId
    val podcastChapterTitleNS = podcastChapterTitle
    val podcastChapterEpisodeIdNS = podcastChapterEpisodeId
    return when (mediaType) {
        SONG_MEDIA_TYPE if songIdNS != null &&
                songTitleNS != null &&
                songArtistNameNS != null &&
                songAlbumNameNS != null &&
                songAlbumIdNS != null &&
                songTimeNS != null
            -> {
            MediaItemData.Full.Song(
                id = songIdNS,
                position = position - 1,
                artUrl = getSongArtUrl(songAlbumIdNS),
                title = songTitleNS,
                author = songArtistNameNS,
                album = songAlbumNameNS,
                duration = TimeOperations.toShortDuration(songTimeNS)
            )
        }

        PODCAST_MEDIA_TYPE if podcastEpisodeIdNS != null &&
                podcastTitleNS != null &&
                podcastTimeNS != null &&
                podcastNameNS != null &&
                podcastIdNS != null &&
                podcastDescriptionNS != null
            -> {
            MediaItemData.Full.PodcastEpisode(
                id = podcastEpisodeIdNS,
                position = position - 1,
                artUrl = getPodcastArtUrl(podcastIdNS),
                title = podcastTitleNS,
                author = podcastNameNS,
                duration = TimeOperations.toShortDuration(podcastTimeNS)
            )
        }

        PODCAST_CHAPTER_MEDIA_TYPE if podcastChapterIdNS != null
                && podcastChapterTitleNS != null
                && podcastChapterEpisodeIdNS != null -> {
            MediaItemData.PodcastChapter(
                podcastChapterIdNS,
                podcastChapterTitleNS,
                podcastChapterEpisodeIdNS
            )
        }

        else -> {
            throw IllegalArgumentException()
        }
    }
}