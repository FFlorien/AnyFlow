package be.florien.anyflow.persistence.local.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity
data class DbOrder(
        @PrimaryKey
        val priority: Int,
        val subject: Long,
        val orderingType: Int,
        val orderingArgument: Int)