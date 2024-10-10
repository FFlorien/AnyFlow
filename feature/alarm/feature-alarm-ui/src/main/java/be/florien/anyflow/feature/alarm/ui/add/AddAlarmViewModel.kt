package be.florien.anyflow.feature.alarm.ui.add

import androidx.lifecycle.MutableLiveData
import be.florien.anyflow.common.ui.BaseViewModel
import be.florien.anyflow.management.alarm.AlarmsSynchronizer
import javax.inject.Inject

class AddAlarmViewModel @Inject constructor(val alarmsSynchronizer: AlarmsSynchronizer) : BaseViewModel() {
    val isRepeating: MutableLiveData<Boolean> = MutableLiveData(false)
    val time: MutableLiveData<Int> = MutableLiveData()
    val monday: MutableLiveData<Boolean> = MutableLiveData()
    val tuesday: MutableLiveData<Boolean> = MutableLiveData()
    val wednesday: MutableLiveData<Boolean> = MutableLiveData()
    val thursday: MutableLiveData<Boolean> = MutableLiveData()
    val friday: MutableLiveData<Boolean> = MutableLiveData()
    val saturday: MutableLiveData<Boolean> = MutableLiveData()
    val sunday: MutableLiveData<Boolean> = MutableLiveData()

    suspend fun addAlarm() {
        when {
            isRepeating.value != true -> alarmsSynchronizer.addSingleAlarm((time.value ?: 0) / 60, (time.value ?: 0) % 60)
            isEveryday() -> alarmsSynchronizer.addRepeatingAlarm((time.value ?: 0) / 60, (time.value ?: 0) % 60)
            else -> alarmsSynchronizer.addRepeatingAlarmForWeekDays((time.value ?: 0) / 60, (time.value ?: 0) % 60, monday.value ?: false, tuesday.value ?: false, wednesday.value ?: false, thursday.value ?: false, friday.value
                    ?: false, saturday.value ?: false, sunday.value ?: false)
        }
    }

    private fun isEveryday() = monday.value == tuesday.value && tuesday.value == wednesday.value && wednesday.value == thursday.value && thursday.value == friday.value && friday.value == saturday.value && saturday.value == sunday.value
}