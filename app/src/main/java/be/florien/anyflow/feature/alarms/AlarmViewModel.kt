package be.florien.anyflow.feature.alarms

import be.florien.anyflow.common.ui.BaseViewModel
import javax.inject.Inject

class AlarmViewModel : BaseViewModel() {
    @Inject
    lateinit var alarmsSynchronizer: AlarmsSynchronizer

    fun shouldAskPermission() = !alarmsSynchronizer.canScheduleExactAlarms()
}