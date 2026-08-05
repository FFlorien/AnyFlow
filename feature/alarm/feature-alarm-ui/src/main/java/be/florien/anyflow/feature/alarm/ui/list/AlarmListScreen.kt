package be.florien.anyflow.feature.alarm.ui.list

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import be.florien.anyflow.common.resources.component.ActionIcon
import be.florien.anyflow.common.resources.component.BlueTopAppBar
import be.florien.anyflow.common.resources.theming.AppTheme
import be.florien.anyflow.feature.alarm.ui.ImmutableAlarm
import be.florien.anyflow.feature.alarm.ui.R
import kotlinx.collections.immutable.persistentListOf
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    alarmListState: AlarmListState,
    setAlarmActive: (Long) -> Unit,
    refreshPermission: () -> Unit,
    onEditAlarm: (ImmutableAlarm) -> Unit,
    onAddAlarm: () -> Unit,
    onClose: () -> Unit
) {
    Scaffold(
        topBar = {
            BlueTopAppBar(
                title = stringResource(R.string.alarm_list),
                onClose = onClose,
                actions = {
                    ActionIcon(
                        painter = { painterResource(R.drawable.ic_add_yellow) },
                        contentDescription = stringResource(R.string.alarm_add),
                        onClick = onAddAlarm
                    )
                }
            )
        }
    ) { paddingValues ->
        when {
            !alarmListState.hasPermission -> PermissionMissing(
                refreshPermission = refreshPermission,
                modifier = Modifier.padding(paddingValues)
            )

            alarmListState.alarms.isEmpty() -> EmptyScreen(modifier = Modifier.padding(paddingValues))
            else ->
                AlarmList(
                    modifier = Modifier.padding(paddingValues),
                    alarmListState = alarmListState,
                    onEditAlarm = onEditAlarm,
                    setAlarmActive = setAlarmActive
                )
        }
    }
}

@Composable
private fun PermissionMissing(
    refreshPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    LaunchedEffect(lifecycleState == Lifecycle.State.RESUMED) {
        if (lifecycleState == Lifecycle.State.RESUMED) {
            refreshPermission()
        }
    }
    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.alarm_permission_explanation))
        val context = LocalContext.current
        Button(onClick = {
            if (Build.VERSION.SDK_INT >= 31) {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                )
            }
        }) {
            Text(stringResource(R.string.alarm_give_permission))
        }
    }
}

@Composable
private fun EmptyScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(stringResource(R.string.alarm_empty))
    }
}

@Composable
private fun AlarmList(
    alarmListState: AlarmListState,
    onEditAlarm: (ImmutableAlarm) -> Unit,
    setAlarmActive: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(alarmListState.alarms) { alarm ->
            Row(
                modifier = Modifier
                    .clickable { onEditAlarm(alarm) }
                    .padding(16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        String.format(
                            Locale.FRENCH,
                            "%d:%02d",
                            alarm.hour,
                            alarm.minute
                        )
                    )
                    Text(getRepetition(alarm))
                }
                Switch(
                    checked = alarm.active,
                    onCheckedChange = { setAlarmActive(alarm.id) }
                )
            }
        }
    }
}

@Composable
fun getRepetition(alarm: ImmutableAlarm): String {
    val list = when {
        !alarm.hasRepetition -> listOf()
        alarm.isEveryday -> listOf(stringResource(R.string.weekday_everyday))
        else -> {
            val dayList = mutableListOf<String>()
            if (alarm.monday) dayList.add(stringResource(R.string.weekday_monday))
            if (alarm.tuesday) dayList.add(stringResource(R.string.weekday_tuesday))
            if (alarm.wednesday) dayList.add(stringResource(R.string.weekday_wednesday))
            if (alarm.thursday) dayList.add(stringResource(R.string.weekday_thursday))
            if (alarm.friday) dayList.add(stringResource(R.string.weekday_friday))
            if (alarm.saturday) dayList.add(stringResource(R.string.weekday_saturday))
            if (alarm.sunday) dayList.add(stringResource(R.string.weekday_sunday))
            dayList
        }
    }
    return list.joinToString()
}

@Preview
@Composable
fun ListPreview() {
    AppTheme {
        AlarmListScreen(
            alarmListState = AlarmListState(hasPermission = true, alarms = persistentListOf()),
            setAlarmActive = {},
            refreshPermission = {},
            onEditAlarm = {},
            onAddAlarm = {},
            onClose = {})
    }
}