package be.florien.anyflow.data.view

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Alarm(
        val id: Long,
        val hour: Int,
        val minute: Int,
        val isRepeating: Boolean,
        val daysToTrigger: List<Boolean>,
        val active: Boolean
): Parcelable