package be.florien.ampacheplayer.view.player

import android.app.Activity
import android.view.View
import be.florien.ampacheplayer.di.ActivityScope
import be.florien.ampacheplayer.view.player.filter.addition.AddFilterFragmentAlbumVM
import be.florien.ampacheplayer.view.player.filter.addition.AddFilterFragmentArtistVM
import be.florien.ampacheplayer.view.player.filter.addition.AddFilterFragmentGenreVM
import be.florien.ampacheplayer.view.player.filter.display.FilterFragment
import be.florien.ampacheplayer.view.player.filter.selectType.AddFilterTypeFragment
import be.florien.ampacheplayer.view.player.songlist.SongListFragment
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent()
@ActivityScope
interface PlayerComponent {

    fun inject(playerActivity: PlayerActivity)
    fun inject(songListFragment: SongListFragment)
    fun inject(filterFragment: FilterFragment)
    fun inject(addFilterFragmentGenreVM: AddFilterFragmentGenreVM)
    fun inject(addFilterFragmentArtistVM: AddFilterFragmentArtistVM)
    fun inject(addFilterFragmentAlbumVM: AddFilterFragmentAlbumVM)
    fun inject(addFilterTypeFragment: AddFilterTypeFragment)

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun activity(activity: Activity): Builder

        @BindsInstance
        fun view(view: View): Builder

        fun build(): PlayerComponent
    }

}
