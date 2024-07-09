package be.florien.anyflow.tags.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Alarm")
data class DbAlarm(
        @PrimaryKey(autoGenerate = true)
        val id: Long = 0,
        val hour: Int,
        val minute: Int,
        val active: Boolean,
        val monday: Boolean,
        val tuesday: Boolean,
        val wednesday: Boolean,
        val thursday: Boolean,
        val friday: Boolean,
        val saturday: Boolean,
        val sunday: Boolean
)