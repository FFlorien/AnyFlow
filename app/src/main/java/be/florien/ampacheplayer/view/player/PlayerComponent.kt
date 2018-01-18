package be.florien.ampacheplayer.view.player

import android.app.Activity
import android.view.View
import be.florien.ampacheplayer.di.ActivityScope
import be.florien.ampacheplayer.view.player.filter.FilterFragment
import be.florien.ampacheplayer.view.player.songlist.SongListFragment
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent()
@ActivityScope
interface PlayerComponent {

    fun inject(playerActivity: PlayerActivity)
    fun inject(songListFragment: SongListFragment)
    fun inject(filterFragment: FilterFragment)

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun activity(activity: Activity): Builder

        @BindsInstance
        fun view(view: View): Builder

        fun build(): PlayerComponent
    }

}
