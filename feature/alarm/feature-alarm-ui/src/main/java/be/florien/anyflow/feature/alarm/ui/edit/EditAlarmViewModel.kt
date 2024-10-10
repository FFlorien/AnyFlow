package be.florien.anyflow.feature.alarm.ui.edit

import androidx.lifecycle.MutableLiveData
import be.florien.anyflow.management.alarm.model.Alarm
import be.florien.anyflow.common.ui.BaseViewModel
import be.florien.anyflow.management.alarm.AlarmsSynchronizer
import javax.inject.Inject

class EditAlarmViewModel @Inject constructor() : BaseViewModel() {
    val isRepeating: MutableLiveData<Boolean> = MutableLiveData(false)
    val time: MutableLiveData<Int> = MutableLiveData()
    val monday: MutableLiveData<Boolean> = MutableLiveData()
    val tuesday: MutableLiveData<Boolean> = MutableLiveData()
    val wednesday: MutableLiveData<Boolean> = MutableLiveData()
    val thursday: MutableLiveData<Boolean> = MutableLiveData()
    val friday: MutableLiveData<Boolean> = MutableLiveData()
    val saturday: MutableLiveData<Boolean> = MutableLiveData()
    val sunday: MutableLiveData<Boolean> = MutableLiveData()

    @Inject
    lateinit var alarmsSynchronizer: AlarmsSynchronizer

    var alarm: Alarm? = null
        set(value) {
            if (value != null) {
                field = value
                isRepeating.value = value.isRepeating
                time.value = value.minute + (value.hour * 60)
                monday.value = value.daysToTrigger[0]
                tuesday.value = value.daysToTrigger[1]
                wednesday.value = value.daysToTrigger[2]
                thursday.value = value.daysToTrigger[3]
                friday.value = value.daysToTrigger[4]
                saturday.value = value.daysToTrigger[5]
                sunday.value = value.daysToTrigger[6]
            }
        }

    suspend fun editAlarm() {
        val newAlarm = Alarm(
            alarm?.id ?: 0L,
            (time.value ?: 0) / 60,
            (time.value ?: 0) % 60,
            isRepeating.value ?: false,
            listOf(
                monday.value ?: false && isRepeating.value ?: false,
                tuesday.value ?: false && isRepeating.value ?: false,
                wednesday.value ?: false && isRepeating.value ?: false,
                thursday.value ?: false && isRepeating.value ?: false,
                friday.value ?: false && isRepeating.value ?: false,
                saturday.value ?: false && isRepeating.value ?: false,
                sunday.value ?: false && isRepeating.value ?: false,
            ),
            alarm?.active ?: false
        )
        if (newAlarm != alarm) {
            alarmsSynchronizer.updateAlarm(newAlarm)
        }
    }

    suspend fun deleteAlarm() {
        val alarmNullSafe = alarm
        if (alarmNullSafe != null)
            alarmsSynchronizer.deleteAlarm(alarmNullSafe)
    }
}