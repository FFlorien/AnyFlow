package be.florien.anyflow.view.player

import android.app.Activity
import android.view.View
import be.florien.anyflow.di.ActivityScope
import be.florien.anyflow.view.player.filter.addition.AddFilterFragmentAlbumVM
import be.florien.anyflow.view.player.filter.addition.AddFilterFragmentArtistVM
import be.florien.anyflow.view.player.filter.addition.AddFilterFragmentGenreVM
import be.florien.anyflow.view.player.filter.display.FilterFragment
import be.florien.anyflow.view.player.filter.display.FilterFragmentVM
import be.florien.anyflow.view.player.filter.selectType.AddFilterTypeFragment
import be.florien.anyflow.view.player.filter.selectType.AddFilterTypeFragmentVM
import be.florien.anyflow.view.player.songlist.SongListFragment
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
    fun inject(filterFragmentVM: FilterFragmentVM)
    fun inject(addFilterTypeFragmentVM: AddFilterTypeFragmentVM)

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun activity(activity: Activity): Builder

        @BindsInstance
        fun view(view: View): Builder

        fun build(): PlayerComponent
    }

}
