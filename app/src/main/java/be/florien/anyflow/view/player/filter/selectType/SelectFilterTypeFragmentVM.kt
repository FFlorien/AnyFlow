package be.florien.anyflow.view.player.filter.selectType

import android.app.Activity
import be.florien.anyflow.R
import be.florien.anyflow.di.ActivityScope
import be.florien.anyflow.view.player.PlayerActivity
import be.florien.anyflow.view.player.filter.BaseFilterVM

const val GENRE_ID = "Genre"
const val ARTIST_ID = "Artist"
const val ALBUM_ID = "Album"
const val SEARCH_ID = "Search"

@ActivityScope
class AddFilterTypeFragmentVM(activity: Activity) : BaseFilterVM() {
    private val genreName = activity.getString(R.string.filter_type_genre)
    private val artistName = activity.getString(R.string.filter_type_artist)
    private val albumName = activity.getString(R.string.filter_type_album)
    private val searchName = activity.getString(R.string.filter_type_search)

    init {
        (activity as PlayerActivity).activityComponent.inject(this)
    }

    val filtersIds = listOf(
            GENRE_ID,
            ARTIST_ID,
            ALBUM_ID,
            SEARCH_ID)

    val filtersNames = listOf(
            genreName,
            artistName,
            albumName,
            searchName)

    val filtersImages = listOf(
            R.drawable.ic_genre,
            R.drawable.ic_artist,
            R.drawable.ic_album,
            R.drawable.ic_song)
}