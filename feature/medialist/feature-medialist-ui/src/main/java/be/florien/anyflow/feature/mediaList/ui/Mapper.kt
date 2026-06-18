package be.florien.anyflow.feature.mediaList.ui

import androidx.core.text.HtmlCompat
import be.florien.anyflow.common.ui.domain.TagType
import be.florien.anyflow.common.utils.TimeOperations
import be.florien.anyflow.feature.song.base.domain.model.SongFieldType
import be.florien.anyflow.management.queue.model.Chapter
import be.florien.anyflow.management.queue.model.PodcastEpisodeDisplay
import be.florien.anyflow.management.queue.model.SongDisplay
import be.florien.anyflow.tags.local.model.DbPodcastEpisodeDisplay
import be.florien.anyflow.tags.local.model.DbQueueItemDisplay
import be.florien.anyflow.tags.local.model.PODCAST_MEDIA_TYPE
import be.florien.anyflow.tags.local.model.SONG_MEDIA_TYPE
import be.florien.anyflow.tags.model.SongInfo
import kotlin.text.substring

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

fun DbQueueItemDisplay.toMediaItemData(getSongArtUrl: (Long) -> String, getPodcastArtUrl: (Long) -> String): MediaItemData.Full {
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
                duration = TimeOperations.toShortDuration(podcastTimeNS),
                chapters = podcastDescriptionNS.let {
                    val timestampRegex =
                        Regex("(<[a-zA-Z]+>)*\\(?\\{?\\[?([0-5]?\\d:)?[0-5]?\\d:[0-5]\\d\\)?\\}?]?")
                    val digitsRegex = Regex("([0-5]?\\d)")
                    val timeStampsTimes = timestampRegex.findAll(it)
                    val chapterList = mutableListOf<Chapter>()
                    timeStampsTimes.forEach { timeStamp ->
                        val next = timeStamp.next()
                        val end = next?.range?.start ?: it.length
                        var time = 0L
                        digitsRegex.findAll(timeStamp.value).forEach {
                            time = (time * 60) + it.value.toLong()
                        }
                        val text = it.substring(timeStamp.range.first, end)
                        chapterList += Chapter(time, text)
                    }
                    chapterList
                }
            )
        }

        else -> {
            throw IllegalArgumentException()
        }
    }
}