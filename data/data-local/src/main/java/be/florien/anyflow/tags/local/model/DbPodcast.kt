package be.florien.anyflow.tags.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

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
