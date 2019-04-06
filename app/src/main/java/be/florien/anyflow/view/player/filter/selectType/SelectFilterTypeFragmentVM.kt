package be.florien.anyflow.view.player.filter.selectType

import android.app.Activity
import be.florien.anyflow.R
import be.florien.anyflow.di.ActivityScope
import be.florien.anyflow.view.player.PlayerActivity
import be.florien.anyflow.view.player.filter.BaseFilterVM

const val GENRE_ID = "Genre"
const val ARTIST_ID = "Artist"
const val ALBUM_ID = "Album"

@ActivityScope
class AddFilterTypeFragmentVM(activity: Activity) : BaseFilterVM() {
    private val genreName = activity.getString(R.string.filter_type_genre)
    private val artistName = activity.getString(R.string.filter_type_artist)
    private val albumName = activity.getString(R.string.filter_type_album)

    init {
        (activity as PlayerActivity).activityComponent.inject(this)
    }

    val filtersIds = listOf(
            GENRE_ID,
            ARTIST_ID,
            ALBUM_ID)

    val filtersNames = listOf(
            genreName,
            artistName,
            albumName)

    val filtersImages = listOf(
            R.drawable.ic_genre,
            R.drawable.ic_artist,
            R.drawable.ic_album)
}