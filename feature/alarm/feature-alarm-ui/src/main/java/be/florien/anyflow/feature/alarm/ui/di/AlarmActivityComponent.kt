package be.florien.anyflow.feature.alarm.ui.di

import be.florien.anyflow.common.di.ActivityScope
import be.florien.anyflow.feature.alarm.ui.AlarmActivity
import dagger.Subcomponent


@Subcomponent(modules = [AlarmViewModelModule::class])
@ActivityScope
interface AlarmActivityComponent {

    fun inject(alarmActivity: AlarmActivity)

    @Subcomponent.Builder
    interface Builder {

        fun build(): AlarmActivityComponent
    }

}