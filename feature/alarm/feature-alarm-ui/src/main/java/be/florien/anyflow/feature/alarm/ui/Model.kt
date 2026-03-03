package be.florien.anyflow.feature.alarm.ui

import androidx.compose.runtime.Immutable
import be.florien.anyflow.management.alarm.model.Alarm
import kotlinx.serialization.Serializable


@Serializable
@Immutable
data class ImmutableAlarm(
    val id: Long,
    val hour: Int,
    val minute: Int,
    val monday: Boolean,
    val tuesday: Boolean,
    val wednesday: Boolean,
    val thursday: Boolean,
    val friday: Boolean,
    val saturday: Boolean,
    val sunday: Boolean,
    val active: Boolean
) {
    val hasRepetition = monday || tuesday || wednesday || thursday || friday || saturday || sunday
    val isEveryday = monday && tuesday && wednesday && thursday && friday && saturday && sunday
}

fun Alarm.toViewAlarm(): ImmutableAlarm = ImmutableAlarm(
    id = id,
    hour = hour,
    minute = minute,
    monday = daysToTrigger[0],
    tuesday = daysToTrigger[1],
    wednesday = daysToTrigger[2],
    thursday = daysToTrigger[3],
    friday = daysToTrigger[4],
    saturday = daysToTrigger[5],
    sunday = daysToTrigger[6],
    active = active
)

fun ImmutableAlarm.toStorageAlarm() = Alarm(
    id = id,
    hour = hour,
    minute = minute,
    isRepeating = monday || tuesday || wednesday || thursday || friday || saturday || sunday,
    daysToTrigger = listOf(monday, tuesday, wednesday, thursday, friday, saturday, sunday),
    active = active
)