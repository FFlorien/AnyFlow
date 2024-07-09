package be.florien.anyflow.tags.model

import androidx.room.PrimaryKey

data class Artist(
    val id: Long,
    val name: String,
    var basename: String)