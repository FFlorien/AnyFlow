package be.florien.anyflow.tags.local.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

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

data class DbFilterGroupWithFilters(
    @Embedded
    val group: DbFilterGroup,
    @Relation(
        parentColumn = "id",
        entityColumn = "filterGroup"
    )
    val filters: List<DbFilter>
)