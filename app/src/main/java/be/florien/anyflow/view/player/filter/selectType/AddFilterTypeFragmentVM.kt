package be.florien.anyflow.view.player.filter.selectType

import android.app.Activity
import be.florien.anyflow.di.ActivityScope
import be.florien.anyflow.view.player.PlayerActivity
import be.florien.anyflow.view.player.filter.BaseFilterVM

const val GENRE_NAME = "Genre"
const val ARTIST_NAME = "Artist"
const val ALBUM_NAME = "Album"
const val SEARCH_NAME = "Search"

/**
 * Created by FlamentF on 08-Jan-18.
 */
@ActivityScope
class AddFilterTypeFragmentVM(activity: Activity) : BaseFilterVM() {

    init {

        (activity as PlayerActivity).activityComponent.inject(this)
    }

    val filtersNames = listOf(
            GENRE_NAME,
            ARTIST_NAME,
            ALBUM_NAME,
            SEARCH_NAME)
}