package be.florien.anyflow.feature.alarms

import be.florien.anyflow.common.ui.BaseViewModel
import be.florien.anyflow.management.alarm.AlarmsSynchronizer
import javax.inject.Inject

class AlarmViewModel : BaseViewModel() {
    @Inject
    lateinit var alarmsSynchronizer: AlarmsSynchronizer

    fun shouldAskPermission() = !alarmsSynchronizer.canScheduleExactAlarms()
}