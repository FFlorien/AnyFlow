package be.florien.anyflow.feature.player.filter.selectType

import be.florien.anyflow.R
import be.florien.anyflow.feature.player.filter.BaseFilterViewModel
import be.florien.anyflow.player.FiltersManager
import javax.inject.Inject

class SelectFilterTypeViewModel @Inject constructor(filtersManager: FiltersManager) : BaseFilterViewModel(filtersManager) {
    private val genreName = R.string.filter_type_genre
    private val artistName = R.string.filter_type_album_artist
    private val albumName = R.string.filter_type_album

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

    companion object {
        const val GENRE_ID = "Genre"
        const val ARTIST_ID = "Artist"
        const val ALBUM_ID = "Album"
    }
}