package be.florien.anyflow.feature.alarm.ui

import be.florien.anyflow.common.ui.BaseViewModel
import be.florien.anyflow.management.alarm.AlarmsSynchronizer
import javax.inject.Inject

class AlarmViewModel @Inject constructor(val alarmsSynchronizer: AlarmsSynchronizer) : BaseViewModel() {

    fun shouldAskPermission() = !alarmsSynchronizer.canScheduleExactAlarms()
}