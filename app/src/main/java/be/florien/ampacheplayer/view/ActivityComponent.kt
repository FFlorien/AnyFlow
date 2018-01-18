package be.florien.ampacheplayer.view

import android.app.Activity
import android.view.View
import be.florien.ampacheplayer.di.ActivityScope
import be.florien.ampacheplayer.view.connect.ConnectActivity
import be.florien.ampacheplayer.view.player.PlayerActivity
import be.florien.ampacheplayer.view.player.filter.FilterFragment
import be.florien.ampacheplayer.view.player.songlist.SongListFragment
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent(modules = [(ActivityModule::class)])
@ActivityScope
interface ActivityComponent {

    fun inject(connectActivity: ConnectActivity)

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun activity(activity: Activity): Builder

        @BindsInstance
        fun view(view: View): Builder

        fun build(): ActivityComponent
    }

}
