package be.florien.anyflow.feature.alarm.ui.edit

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import be.florien.anyflow.common.resources.component.BlueTopAppBar
import be.florien.anyflow.feature.alarm.ui.ImmutableAlarm
import be.florien.anyflow.feature.alarm.ui.R
import java.util.Calendar

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EditAlarmScreen(
    title: String,
    alarm: ImmutableAlarm,
    editAlarm: (Int, Int, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit,
    deleteAlarm: (() -> Unit)?,
    onClose: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        is24Hour = true,
        initialHour = alarm.hour,
        initialMinute = alarm.minute
    )
    LaunchedEffect(alarm) {
        timePickerState.hour = alarm.hour
        timePickerState.minute = alarm.minute
    }
    var monday by remember(alarm) { mutableStateOf(alarm.monday) }
    var tuesday by remember(alarm) { mutableStateOf(alarm.tuesday) }
    var wednesday by remember(alarm) { mutableStateOf(alarm.wednesday) }
    var thursday by remember(alarm) { mutableStateOf(alarm.thursday) }
    var friday by remember(alarm) { mutableStateOf(alarm.friday) }
    var saturday by remember(alarm) { mutableStateOf(alarm.saturday) }
    var sunday by remember(alarm) { mutableStateOf(alarm.sunday) }
    Scaffold(
        topBar = {
            BlueTopAppBar(
                title = title,
                onClose = onClose,
                actions = {
                    IconButton(
                        onClick = {
                            editAlarm(
                                timePickerState.hour,
                                timePickerState.minute,
                                monday,
                                tuesday,
                                wednesday,
                                thursday,
                                friday,
                                saturday,
                                sunday,
                            )
                        }
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_confirm),
                            contentDescription = stringResource(R.string.confirm),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    if (deleteAlarm != null) {
                        IconButton(onClick = deleteAlarm) {
                            Icon(
                                painterResource(R.drawable.ic_delete),
                                contentDescription = stringResource(R.string.content_description_delete_alarm),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        FlowRow(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .scrollable(rememberScrollState(), orientation = Orientation.Vertical),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimePicker(
                modifier = Modifier.fillMaxWidth(),
                state = timePickerState
            )
            RepetitionText(monday, tuesday, wednesday, thursday, friday, saturday, sunday)
            DaysButtons(
                monday = monday,
                tuesday = tuesday,
                wednesday = wednesday,
                thursday = thursday,
                friday = friday,
                saturday = saturday,
                sunday = sunday,
                onWeekdaySelected = { weekdayNumber, isDaySelected ->
                    when (weekdayNumber) {
                        Calendar.MONDAY -> monday = !isDaySelected
                        Calendar.TUESDAY -> tuesday = !isDaySelected
                        Calendar.WEDNESDAY -> wednesday = !isDaySelected
                        Calendar.THURSDAY -> thursday = !isDaySelected
                        Calendar.FRIDAY -> friday = !isDaySelected
                        Calendar.SATURDAY -> saturday = !isDaySelected
                        else -> sunday = !isDaySelected
                    }
                }
            )
        }
    }
}

@Composable
private fun RepetitionText(
    monday: Boolean,
    tuesday: Boolean,
    wednesday: Boolean,
    thursday: Boolean,
    friday: Boolean,
    saturday: Boolean,
    sunday: Boolean
) {
    val isAnyDay =
        monday || tuesday || wednesday || thursday || friday || saturday || sunday
    val isEveryDay =
        monday && tuesday && wednesday && thursday && friday && saturday && sunday
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = if (!isAnyDay) {
            stringResource(R.string.alarm_once)
        } else if (isEveryDay) {
            stringResource(R.string.alarm_every_day)
        } else {
            val days = getDayNames(
                monday, tuesday, wednesday, thursday, friday, saturday, sunday
            )
            stringResource(R.string.alarm_selected_days, days)
        },
        minLines = 2
    )
}

@Composable
private fun getDayNames(
    monday: Boolean,
    tuesday: Boolean,
    wednesday: Boolean,
    thursday: Boolean,
    friday: Boolean,
    saturday: Boolean,
    sunday: Boolean
): String {
    val firstDayOfWeek = Calendar.getInstance().firstDayOfWeek

    return listOf(sunday, monday, tuesday, wednesday, thursday, friday, saturday)
        .mapIndexed { index, bool ->
            if (bool) {
                index + 1
            } else {
                null
            }
        }
        .sortedBy {
            it?.let {
                (it + 7 - firstDayOfWeek) % 7
            } ?: -1
        }
        .mapNotNull {
            when (it) {
                Calendar.MONDAY -> stringResource(R.string.monday)
                Calendar.TUESDAY -> stringResource(R.string.tuesday)
                Calendar.WEDNESDAY -> stringResource(R.string.wednesday)
                Calendar.THURSDAY -> stringResource(R.string.thursday)
                Calendar.FRIDAY -> stringResource(R.string.friday)
                Calendar.SATURDAY -> stringResource(R.string.saturday)
                Calendar.SUNDAY -> stringResource(R.string.sunday)
                else -> null
            }
        }
        .joinToString()
}

@Composable
private fun DaysButtons(
    monday: Boolean,
    tuesday: Boolean,
    wednesday: Boolean,
    thursday: Boolean,
    friday: Boolean,
    saturday: Boolean,
    sunday: Boolean,
    onWeekdaySelected: (Int, Boolean) -> Unit
) {
    val firstDayOfWeek = Calendar.getInstance().firstDayOfWeek
    for (i in 0..6) {
        val weekdayNumber = ((firstDayOfWeek + i - 1) % 7) + 1
        val isDaySelected = when (weekdayNumber) {
            Calendar.MONDAY -> monday
            Calendar.TUESDAY -> tuesday
            Calendar.WEDNESDAY -> wednesday
            Calendar.THURSDAY -> thursday
            Calendar.FRIDAY -> friday
            Calendar.SATURDAY -> saturday
            else -> sunday
        }
        val initials = when (weekdayNumber) {
            Calendar.MONDAY -> stringResource(R.string.monday)
            Calendar.TUESDAY -> stringResource(R.string.tuesday)
            Calendar.WEDNESDAY -> stringResource(R.string.wednesday)
            Calendar.THURSDAY -> stringResource(R.string.thursday)
            Calendar.FRIDAY -> stringResource(R.string.friday)
            Calendar.SATURDAY -> stringResource(R.string.saturday)
            else -> stringResource(R.string.sunday)
        }
        OutlinedButton(
            colors = ButtonDefaults
                .buttonColors(
                    containerColor = if (!isDaySelected) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.tertiaryContainer
                    }
                ),
            onClick = {
                onWeekdaySelected(weekdayNumber, isDaySelected)
            }) {
            Text(
                text = initials.substring(0, 3),
                color = if (!isDaySelected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onTertiaryContainer
                }
            )
        }
    }
}