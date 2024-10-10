package be.florien.anyflow.feature.alarm.ui.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import be.florien.anyflow.architecture.di.AnyFlowViewModelFactory
import be.florien.anyflow.architecture.di.ViewModelKey
import be.florien.anyflow.feature.alarm.ui.AlarmViewModel
import be.florien.anyflow.feature.alarm.ui.add.AddAlarmViewModel
import be.florien.anyflow.feature.alarm.ui.edit.EditAlarmViewModel
import be.florien.anyflow.feature.alarm.ui.list.AlarmListViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class AlarmViewModelModule {

    @Binds
    abstract fun bindsViewModelFactory(factory: AnyFlowViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @ViewModelKey(AlarmViewModel::class)
    abstract fun bindsAlarmViewModel(viewModel: AlarmViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AddAlarmViewModel::class)
    abstract fun bindsAddAlarmViewModel(viewModel: AddAlarmViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(EditAlarmViewModel::class)
    abstract fun bindsEditAlarmViewModel(viewModel: EditAlarmViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AlarmListViewModel::class)
    abstract fun bindsAlarmListViewModel(viewModel: AlarmListViewModel): ViewModel
}