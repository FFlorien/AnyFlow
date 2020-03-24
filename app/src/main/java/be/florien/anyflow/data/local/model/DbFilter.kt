package be.florien.anyflow.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(foreignKeys = [
        ForeignKey(
                entity = DbFilterGroup::class,
                parentColumns = arrayOf("id"),
                childColumns = arrayOf("filterGroup"),
                onDelete = ForeignKey.CASCADE)])
data class DbFilter(
        @PrimaryKey(autoGenerate = true)
        val id: Int,
        val clause: String,
        val argument: String,
        val displayText: String,
        val displayImage: String?,
        val filterGroup: Long)