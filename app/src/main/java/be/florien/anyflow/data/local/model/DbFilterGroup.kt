package be.florien.anyflow.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "FilterGroup")
data class DbFilterGroup(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val name: String?,
    val dateAdded: Long?
) {

    companion object {
        const val CURRENT_FILTER_GROUP_ID = 1L
    }
}