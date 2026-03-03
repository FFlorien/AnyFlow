package be.florien.anyflow.feature.alarm.ui

import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import be.florien.anyflow.feature.alarm.ui.edit.EditAlarmScreen
import be.florien.anyflow.feature.alarm.ui.edit.EditAlarmViewModel
import be.florien.anyflow.feature.alarm.ui.list.AlarmListScreen
import be.florien.anyflow.feature.alarm.ui.list.AlarmListViewModel
import kotlinx.serialization.Serializable

@Serializable
internal object AlarmList

@Serializable
internal object AddAlarm

@Serializable
data class EditAlarm(val alarmId: Long)

fun NavGraphBuilder.alarmListDestination(
    navController: NavHostController,
    viewModelFactory: ViewModelProvider.Factory,
    onClose: () -> Unit
) {
    composable<AlarmList> {
        val viewModel = viewModel<AlarmListViewModel>(factory = viewModelFactory)
        AlarmListScreen(
            alarmListState = viewModel.state.collectAsStateWithLifecycle().value,
            setAlarmActive = viewModel::setAlarmActive,
            refreshPermission = viewModel::refreshPermission,
            onEditAlarm = { navController.navigate(EditAlarm(it.id)) },
            onAddAlarm = { navController.navigate(AddAlarm) },
            onClose = onClose
        )
    }
}

fun NavGraphBuilder.addAlarmDestination(
    navController: NavHostController,
    viewModelFactory: ViewModelProvider.Factory
) {
    composable<AddAlarm> {
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
                navController.popBackStack()
            },
            deleteAlarm = null,
            onClose = navController::popBackStack

        )
    }
}

fun NavGraphBuilder.editAlarmDestination(
    navController: NavHostController,
    viewModelFactory: ViewModelProvider.Factory
) {
    composable<EditAlarm> {
        val viewModel = viewModel<EditAlarmViewModel>(factory = viewModelFactory)
        val editAlarm = it.toRoute<EditAlarm>()
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
                navController.popBackStack()
            },
            deleteAlarm = {
                viewModel.deleteAlarm(state.toStorageAlarm())
                navController.popBackStack()
            },
            onClose = navController::popBackStack
        )
    }
}