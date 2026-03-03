package be.florien.anyflow.feature.alarm.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import be.florien.anyflow.common.di.AnyFlowViewModelFactory
import be.florien.anyflow.common.di.ViewModelFactoryProvider
import be.florien.anyflow.common.navigation.ComposeNavigator
import be.florien.anyflow.common.navigation.rememberNavigationState
import be.florien.anyflow.common.navigation.toEntries
import be.florien.anyflow.common.resources.theming.AppTheme
import be.florien.anyflow.feature.alarm.ui.di.AlarmActivityComponentCreator
import be.florien.anyflow.feature.alarm.ui.edit.EditAlarmScreen
import be.florien.anyflow.feature.alarm.ui.edit.EditAlarmViewModel
import be.florien.anyflow.feature.alarm.ui.list.AlarmListScreen
import be.florien.anyflow.feature.alarm.ui.list.AlarmListViewModel
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
internal object AlarmList: NavKey

@Serializable
internal object AddAlarm: NavKey

@Serializable
data class EditAlarm(val alarmId: Long): NavKey

class AlarmActivity : AppCompatActivity(), ViewModelFactoryProvider {
    @Inject
    override lateinit var viewModelFactory: AnyFlowViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activityComponent = (application as AlarmActivityComponentCreator)
            .createAlarmActivityComponent()
            ?: throw IllegalStateException()
        activityComponent.inject(this)
        setContent {
            AppTheme {
                val navigationState = rememberNavigationState(
                    startRoute = AlarmList,
                    topLevelRoutes = setOf(AlarmList)
                )

                val navigator = remember { ComposeNavigator(navigationState) }

                val entryProvider = entryProvider {
                    entry<AlarmList> {
                        val viewModel = viewModel<AlarmListViewModel>(factory = viewModelFactory)
                        AlarmListScreen(
                            alarmListState = viewModel.state.collectAsStateWithLifecycle().value,
                            setAlarmActive = viewModel::setAlarmActive,
                            refreshPermission = viewModel::refreshPermission,
                            onEditAlarm = { navigator.navigate(EditAlarm(it.id)) },
                            onAddAlarm = { navigator.navigate(AddAlarm) },
                            onClose = { this@AlarmActivity.finish() }
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
                NavDisplay(
                    entries = navigationState.toEntries(entryProvider),
                    onBack = {
                        if (navigationState.backStacks[navigationState.topLevelRoute]?.last() == AlarmList) {
                            this@AlarmActivity.finish()
                        } else {
                            navigator.goBack()
                        }
                    }
                )
            }
        }
    }
}