package be.florien.anyflow.feature.alarm.ui

import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import be.florien.anyflow.common.di.AnyFlowViewModelFactory
import be.florien.anyflow.common.navigation.AddAlarm
import be.florien.anyflow.common.navigation.AlarmList
import be.florien.anyflow.common.navigation.ComposeNavigator
import be.florien.anyflow.common.navigation.EditAlarm
import be.florien.anyflow.feature.alarm.ui.edit.EditAlarmScreen
import be.florien.anyflow.feature.alarm.ui.edit.EditAlarmViewModel
import be.florien.anyflow.feature.alarm.ui.list.AlarmListScreen
import be.florien.anyflow.feature.alarm.ui.list.AlarmListViewModel


fun EntryProviderScope<NavKey>.alarmEntry(
    viewModelFactory: AnyFlowViewModelFactory,
    navigator: ComposeNavigator
) {
    entry<AlarmList> {
        val viewModel = viewModel<AlarmListViewModel>(factory = viewModelFactory)
        AlarmListScreen(
            alarmListState = viewModel.state.collectAsStateWithLifecycle().value,
            setAlarmActive = viewModel::setAlarmActive,
            refreshPermission = viewModel::refreshPermission,
            onEditAlarm = { navigator.navigate(EditAlarm(it.id)) },
            onAddAlarm = { navigator.navigate(AddAlarm) },
            onClose = { navigator.goBack() }
        )
    }

    entry<AddAlarm> {
        val viewModel = viewModel<EditAlarmViewModel>(factory = viewModelFactory)
        val state = viewModel.state.collectAsStateWithLifecycle().value
        EditAlarmScreen(
            title = stringResource(R.string.alarm_add),
            alarm = state,
            editAlarm = { hour: Int, minute: Int, monday: Boolean, tuesday: Boolean, wednesday: Boolean, thursday: Boolean, friday: Boolean, saturday: Boolean, sunday: Boolean ->
                viewModel.addAlarm(
                    hour,
                    minute,
                    monday,
                    tuesday,
                    wednesday,
                    thursday,
                    friday,
                    saturday,
                    sunday
                )
                navigator.goBack()
            },
            deleteAlarm = null,
            onClose = navigator::goBack

        )
    }
    entry<EditAlarm> { editAlarm ->
        val viewModel = viewModel<EditAlarmViewModel>(factory = viewModelFactory)
        val alarmId = editAlarm.alarmId
        viewModel.setAlarm(alarmId)
        val state = viewModel.state.collectAsStateWithLifecycle().value
        EditAlarmScreen(
            title = stringResource(R.string.alarm_edit),
            alarm = state,
            editAlarm = { hour: Int, minute: Int, monday: Boolean, tuesday: Boolean, wednesday: Boolean, thursday: Boolean, friday: Boolean, saturday: Boolean, sunday: Boolean ->
                viewModel.editAlarm(
                    state.id,
                    state.active,
                    hour,
                    minute,
                    monday,
                    tuesday,
                    wednesday,
                    thursday,
                    friday,
                    saturday,
                    sunday
                )
                navigator.goBack()
            },
            deleteAlarm = {
                viewModel.deleteAlarm(state.toStorageAlarm())
                navigator.goBack()
            },
            onClose = navigator::goBack
        )
    }
}