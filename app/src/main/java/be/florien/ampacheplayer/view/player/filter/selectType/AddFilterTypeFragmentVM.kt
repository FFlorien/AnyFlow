package be.florien.ampacheplayer.view.player.filter.selectType

import be.florien.ampacheplayer.di.ActivityScope
import be.florien.ampacheplayer.view.BaseVM
import javax.inject.Inject

const val GENRE_NAME = "Genre"
const val ARTIST_NAME = "Artist"
const val ALBUM_NAME = "Album"
const val SEARCH_NAME = "Search"

/**
 * Created by FlamentF on 08-Jan-18.
 */
@ActivityScope
class AddFilterTypeFragmentVM
@Inject constructor() : BaseVM() {

    val filtersNames = listOf(
            GENRE_NAME,
            ARTIST_NAME,
            ALBUM_NAME,
            SEARCH_NAME)
}