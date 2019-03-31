package be.florien.anyflow.view.player

import android.app.Activity
import android.view.View
import be.florien.anyflow.di.ActivityScope
import be.florien.anyflow.view.player.filter.selection.SelectFilterFragmentAlbumVM
import be.florien.anyflow.view.player.filter.selection.SelectFilterFragmentArtistVM
import be.florien.anyflow.view.player.filter.selection.SelectFilterFragmentGenreVM
import be.florien.anyflow.view.player.filter.display.DisplayFilterFragment
import be.florien.anyflow.view.player.filter.display.DisplayFilterFragmentVM
import be.florien.anyflow.view.player.filter.selectType.SelectFilterTypeFragment
import be.florien.anyflow.view.player.filter.selectType.AddFilterTypeFragmentVM
import be.florien.anyflow.view.player.songlist.SongListFragment
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
@ActivityScope
interface PlayerComponent {

    fun inject(playerActivity: PlayerActivity)
    fun inject(songListFragment: SongListFragment)
    fun inject(displayFilterFragment: DisplayFilterFragment)
    fun inject(addFilterFragmentGenreVM: SelectFilterFragmentGenreVM)
    fun inject(addFilterFragmentArtistVM: SelectFilterFragmentArtistVM)
    fun inject(addFilterFragmentAlbumVM: SelectFilterFragmentAlbumVM)
    fun inject(selectFilterTypeFragment: SelectFilterTypeFragment)
    fun inject(filterFragmentVM: DisplayFilterFragmentVM)
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
