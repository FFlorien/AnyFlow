package be.florien.anyflow.data.local.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "Podcast")
data class DbPodcast(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val language: String,
    val feedUrl: String,
    val website: String,
    val buildDate: String,
    val syncDate: String,
    val publicUrl: String,
    val art: String,
    val hasArt: Boolean
)

@Entity(tableName = "PodcastEpisode")
data class DbPodcastEpisode(
    @PrimaryKey
    val id: String,
    val title: String,
    val name: String,
    val podcastId: Long,
    val description: String,
    val category: String,
    val author: String,
    val authorFull: String,
    val website: String,
    val publicationDate: String,
    val state: String,
    val filelength: String,
    val filesize: String,
    val filename: String,
    val time: Int,
    val size: Int,
    val url: String,
    val art: String,
    val hasArt: Boolean,
    val playcount: Int,
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