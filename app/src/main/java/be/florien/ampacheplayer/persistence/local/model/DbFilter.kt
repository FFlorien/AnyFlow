package be.florien.ampacheplayer.persistence.local.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity
data class DbFilter (
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val clause: String,
    val argument: String,
    val displayValue: String)