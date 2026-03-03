package be.florien.anyflow.feature.alarm.ui.di

import androidx.lifecycle.ViewModel
import be.florien.anyflow.common.di.ViewModelKey
import be.florien.anyflow.feature.alarm.ui.edit.EditAlarmViewModel
import be.florien.anyflow.feature.alarm.ui.list.AlarmListViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class AlarmViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(EditAlarmViewModel::class)
    abstract fun bindsEditAlarmViewModel(viewModel: EditAlarmViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AlarmListViewModel::class)
    abstract fun bindsAlarmListViewModel(viewModel: AlarmListViewModel): ViewModel
}