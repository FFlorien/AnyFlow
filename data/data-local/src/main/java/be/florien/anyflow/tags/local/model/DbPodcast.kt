package be.florien.anyflow.tags.local.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "Podcast")
data class DbPodcast(
    @PrimaryKey
    val id: Long,
    val name: String,
    val description: String,
    val language: String,
    val feedUrl: String,
    val website: String,
    val buildDate: Long,
    val syncDate: Long
)

data class DbPodcastDisplay(
    val id: Long,
    val name: String,
    val syncDate: Long
)

@Entity(tableName = "PodcastEpisode")
data class DbPodcastEpisode(
    @PrimaryKey
    val id: Long,
    val title: String,
    val podcastId: Long,
    val description: String,
    val category: String,
    val authorFull: String,
    val website: String,
    val publicationDate: Long,
    val state: String,
    val time: Int,
    val size: Int,
    val playCount: Int,
    val played: String,
    val waveForm: String?
)

class DbPodcastEpisodeDisplay(
    val id: Long,
    val title: String,
    val podcastName: String,
    val podcastId: Long,
    val time: Int,
    val description: String,
    val publicationDate: Long
)

data class DbPodcastEpisodeWithPodcast(
    @Embedded
    val episode: DbPodcastEpisode,
    @Relation(
        parentColumn = "podcastId",
        entityColumn = "id"
    )
    val podcast: DbPodcast
)

@Entity(tableName = "PodcastChapter")
data class DbPodcastChapter(
    @PrimaryKey
    val id: Long,
    val podcastEpisodeId: Long,
    val startTime: Long,
    val endTime: Long,
    val title: String
)

data class DbPodcastEpisodeWithChapters(
    val podcastEpisodeId: Long,
    val chapterId: Long?
)