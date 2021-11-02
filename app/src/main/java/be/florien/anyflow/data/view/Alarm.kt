package be.florien.anyflow.data.view

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class Alarm(
        val id: Long,
        val hour: Int,
        val minute: Int,
        val isRepeating: Boolean,
        val daysToTrigger: List<Boolean>,
        val active: Boolean
) : Parcelable {
    fun getNextOccurrence(): Calendar? {
        if (!active) {
            return null
        }

        val now = Calendar.getInstance()
        val next = Calendar.getInstance()
        next.set(Calendar.SECOND, 0)
        next.set(Calendar.MINUTE, minute)
        next.set(Calendar.HOUR_OF_DAY, hour)
        val isEveryDay = daysToTrigger.all { it }

        if (isRepeating && !isEveryDay) {
            val currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK)
            val daysToActivate = daysToTrigger.mapIndexed { index, isActive ->
                if (!isActive) {
                    -1
                } else {
                    when (index) {
                        0 -> Calendar.MONDAY
                        1 -> Calendar.TUESDAY
                        2 -> Calendar.WEDNESDAY
                        3 -> Calendar.THURSDAY
                        4 -> Calendar.FRIDAY
                        5 -> Calendar.SATURDAY
                        else -> Calendar.SUNDAY
                    }
                }
            }.filter { it >= 0 }

            if (!daysToActivate.contains(currentDayOfWeek) || next.before(now)) {
                val nextDayOfWeek = daysToActivate.firstOrNull { it > currentDayOfWeek }

                if (nextDayOfWeek != null) {
                    next.set(Calendar.DAY_OF_WEEK, nextDayOfWeek)
                } else {
                    next.set(Calendar.DAY_OF_WEEK, daysToActivate.first())
                }

                if (next.before(now)) {
                    next.add(Calendar.WEEK_OF_YEAR, 1)
                }
            }
        } else if (next.before(now)) {
            next.add(Calendar.DAY_OF_YEAR, 1)
        }

        return next
    }
}