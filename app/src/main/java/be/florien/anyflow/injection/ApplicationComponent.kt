package be.florien.anyflow.injection

import android.app.Application
import be.florien.anyflow.AnyFlowApp
import be.florien.anyflow.data.user.UserComponent
import be.florien.anyflow.extension.MyAppGlideModule
import be.florien.anyflow.feature.alarms.AlarmViewModel
import be.florien.anyflow.feature.alarms.add.AddAlarmViewModel
import be.florien.anyflow.feature.alarms.edit.EditAlarmViewModel
import be.florien.anyflow.feature.alarms.list.AlarmListViewModel
import be.florien.anyflow.feature.connect.ConnectViewModel
import be.florien.anyflow.feature.playlist.list.PlaylistListViewModel
import be.florien.anyflow.feature.playlist.songs.PlaylistSongsViewModel
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton


/**
 * Component used to add dependency injection about data into classes
 */
@Singleton
@Component(modules = [ApplicationModule::class, ApplicationWideModule::class])
interface ApplicationComponent {

    fun inject(anyFlowApp: AnyFlowApp)

    fun inject(connectViewModel: ConnectViewModel)

    fun inject(viewModel: AlarmViewModel)
    fun inject(viewModel: AddAlarmViewModel)
    fun inject(viewModel: AlarmListViewModel)
    fun inject(viewModel: EditAlarmViewModel)
    fun inject(viewModel: PlaylistSongsViewModel)
    fun inject(viewModel: PlaylistListViewModel)
    fun inject(myAppGlideModule: MyAppGlideModule)

    fun userComponentBuilder(): UserComponent.Builder

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: Application): Builder

        fun build(): ApplicationComponent
    }
}