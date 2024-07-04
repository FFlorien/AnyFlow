package be.florien.anyflow.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Ordering")
data class DbOrdering(
    @PrimaryKey
    val priority: Int,
    val subject: Long,
    val orderingType: Int,
    val orderingArgument: Int
)