package be.florien.anyflow.management.queue.model

import be.florien.anyflow.common.utils.TimeOperations


sealed interface QueueItemDisplay

data class SongDisplay(
    val id: Long,
    val title: String,
    val artistName: String,
    val albumName: String,
    val albumId: Long,
    val time: Int
) : QueueItemDisplay {
    val timeText: String
        get() = TimeOperations.toMediaDuration(time)
}

data class PodcastEpisodeDisplay(
    val id: Long,
    val title: String,
    val podcast: String,
    val podcastId: Long,
    val description: String,
    val time: Int
) : QueueItemDisplay {

    val timeText: String
        get() = TimeOperations.toMediaDuration(time)

    val chapters: List<Chapter> by lazy {
            val timestampRegex = Regex("(<[a-zA-Z]+>)*\\(?\\{?\\[?([0-5]?\\d:)?[0-5]?\\d:[0-5]\\d\\)?\\}?]?")
            val digitsRegex = Regex("([0-5]?\\d)")
            val timeStampsTimes = timestampRegex.findAll(description)
            val chapterList = mutableListOf<Chapter>()
            timeStampsTimes.forEach { timeStamp ->
                val next = timeStamp.next()
                val end = next?.range?.start ?: description.length
                var time = 0L
                digitsRegex.findAll(timeStamp.value).forEach {
                    time = (time * 60) + it.value.toLong()
                }
                val text = description.substring(timeStamp.range.first, end)
                chapterList += Chapter(time, text)
            }
            chapterList
        }
}

data class Chapter(
    val time: Long,
    val title: String
)