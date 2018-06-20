package be.florien.ampacheplayer.view.player.filter

import android.databinding.Bindable
import be.florien.ampacheplayer.BR
import be.florien.ampacheplayer.di.ActivityScope
import be.florien.ampacheplayer.persistence.local.LocalDataManager
import be.florien.ampacheplayer.persistence.local.model.Album
import be.florien.ampacheplayer.persistence.local.model.Artist
import be.florien.ampacheplayer.persistence.local.model.Filter
import be.florien.ampacheplayer.persistence.local.model.Song
import be.florien.ampacheplayer.view.BaseVM
import javax.inject.Inject

const val GENRE_NAME = "Genre"
const val ARTIST_NAME = "Artist"
const val ALBUM_NAME = "Album"
const val SEARCH_NAME = "Search"

const val MASTER_FILTER_ID = -1L
const val GENRE_FILTER_ID = -2L
const val ARTIST_FILTER_ID = -3L
const val ALBUM_FILTER_ID = -4L
const val SEARCH_FILTER_ID = -5L

/**
 * Created by FlamentF on 08-Jan-18.
 */
@ActivityScope
class AddFilterFragmentVM
@Inject constructor(
        private val localDataManager: LocalDataManager) : BaseVM() {

    /**
     * Constructor
     */

    init {
        subscribe(localDataManager.getGenres(), onNext = { updateGenre(it) })
        subscribe(localDataManager.getArtists(), onNext = { updateArtists(it) })
        subscribe(localDataManager.getAlbums(), onNext = { updateAlbums(it) })
    }

    /**
     * Attributes
     */

    private val filtersNames = listOf(
            FilterItem(GENRE_FILTER_ID, GENRE_NAME),
            FilterItem(ARTIST_FILTER_ID, ARTIST_NAME),
            FilterItem(ALBUM_FILTER_ID, ALBUM_NAME),
            FilterItem(SEARCH_FILTER_ID, SEARCH_NAME))

    private var genresValues: List<Song>? = null
    private var artistsValues: List<Artist>? = null
    private var albumsValues: List<Album>? = null

    private var currentFilterType = MASTER_FILTER_ID
        set(value) {
            field = value
            notifyPropertyChanged(BR.filterList)
        }

    /**
     * Bindables
     */

    @Bindable
    fun getFilterList(): List<FilterItem> = when (currentFilterType) { //todo getItemId (position) and getItemDisplayName(position)
        MASTER_FILTER_ID -> filtersNames
        GENRE_FILTER_ID -> getGenresValues()
        ARTIST_FILTER_ID -> getArtistsValues()
        ALBUM_FILTER_ID -> getAlbumsValues()
        else -> filtersNames
    }

    /**
     * Actions
     */

    fun onFilterSelected(filterSelected: Long) {
        when (currentFilterType) {
            MASTER_FILTER_ID -> {
                currentFilterType = filterSelected
            }
            GENRE_FILTER_ID -> localDataManager.addFilters(listOf(Filter.GenreIs(getGenresValues()[filterSelected.toInt()].displayName)))
            ARTIST_FILTER_ID -> localDataManager.addFilters(listOf(Filter.ArtistIs(filterSelected)))
            ALBUM_FILTER_ID -> localDataManager.addFilters(listOf(Filter.AlbumIs(filterSelected)))
        }

        if (currentFilterType != filterSelected) {
            currentFilterType = MASTER_FILTER_ID
        }
    }

    /**
     * Private methods
     */

    private fun getGenresValues(): List<FilterItem> = genresValues?.mapIndexed { index, song -> FilterItem(index.toLong(), song.genre) }
            ?: listOf()

    private fun getArtistsValues(): List<FilterItem> = artistsValues?.map { FilterItem(it.id, it.name) }
            ?: listOf()

    private fun getAlbumsValues(): List<FilterItem> = albumsValues?.map { FilterItem(it.id, it.name) }
            ?: listOf()

    private fun updateGenre(songResults: List<Song>) {
        genresValues = songResults
        if (currentFilterType == GENRE_FILTER_ID) {
            notifyPropertyChanged(BR.filterList)
        }
    }

    private fun updateArtists(artistsResults: List<Artist>?) {
        artistsValues = artistsResults
        if (currentFilterType == ARTIST_FILTER_ID) {
            notifyPropertyChanged(BR.filterList)
        }
    }

    private fun updateAlbums(albumsResults: List<Album>?) {
        albumsValues = albumsResults
        if (currentFilterType == ALBUM_FILTER_ID) {
            notifyPropertyChanged(BR.filterList)
        }
    }

    /**
     * Inner classes
     */

    class FilterItem(val id: Long, val displayName: String)
}