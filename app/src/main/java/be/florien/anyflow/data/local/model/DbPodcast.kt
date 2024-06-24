package be.florien.anyflow.data.local.model

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
    val buildDate: String,
    val syncDate: String
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
    val publicationDate: String,
    val state: String,
    val time: Int,
    val size: Int,
    val playCount: Int,
    val played: String
)

data class DbPodcastWithEpisodes(
    @Embedded val podcast: DbPodcast,
    @Relation(
        parentColumn = "id",
        entityColumn = "podcastId"
    )
    val episodes: List<DbPodcastEpisode>
)