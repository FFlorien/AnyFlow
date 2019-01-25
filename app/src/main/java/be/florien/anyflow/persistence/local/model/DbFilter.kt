package be.florien.anyflow.persistence.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class DbFilter (
        @PrimaryKey(autoGenerate = true)
    val id: Long,
        val clause: String,
        val argument: String,
        val displayText: String,
        val displayImage: String?)