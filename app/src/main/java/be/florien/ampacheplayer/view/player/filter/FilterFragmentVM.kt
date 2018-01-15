package be.florien.ampacheplayer.view.player.filter

import android.databinding.Bindable
import be.florien.ampacheplayer.BR
import be.florien.ampacheplayer.persistence.DatabaseManager
import be.florien.ampacheplayer.player.AudioQueueManager
import be.florien.ampacheplayer.player.Filter
import be.florien.ampacheplayer.view.BaseVM
import javax.inject.Inject

const val MASTER_NAME = "filterSelection"
const val GENRE_NAME = "Genre"
const val ARTIST_NAME = "Artist"
const val ALBUM_NAME = "Album"
const val SEARCH_NAME = "Search"

const val MASTER_FILTER_ID = 0L
const val GENRE_FILTER_ID = 1L
const val ARTIST_FILTER_ID = 2L
const val ALBUM_FILTER_ID = 3L
const val SEARCH_FILTER_ID = 4L

/**
 * Created by FlamentF on 08-Jan-18.
 */
class FilterFragmentVM
@Inject constructor(
        private val databaseManager: DatabaseManager,
        private val audioQueueManager: AudioQueueManager) : BaseVM() {

    private val filtersNames = listOf(
            FilterItem(GENRE_FILTER_ID, GENRE_NAME),
            FilterItem(ARTIST_FILTER_ID, ARTIST_NAME),
            FilterItem(ALBUM_FILTER_ID, ALBUM_NAME),
            FilterItem(SEARCH_FILTER_ID, SEARCH_NAME))

    private fun getGenresValues() = databaseManager.getGenres().mapIndexed { index, name -> FilterItem(index.toLong(), name) }
    private fun getArtistsValues() = databaseManager.getArtists().map { FilterItem(it.id, it.name) }
    private fun getAlbumsValues() = databaseManager.getAlbums().map { FilterItem(it.artistId, it.name) }

    var currentFilterType = MASTER_FILTER_ID
        set(value) {
            field = value
            notifyPropertyChanged(BR.filterList)
        }

    @Bindable
    var currentSearch = ""

    @Bindable
    fun getFilterList(): List<FilterItem> = when (currentFilterType) { //todo getItemId (position) and getItemDisplayName(position)
        MASTER_FILTER_ID -> filtersNames
        GENRE_FILTER_ID -> getGenresValues()
        ARTIST_FILTER_ID -> getArtistsValues()
        ALBUM_FILTER_ID -> getAlbumsValues()
        else -> filtersNames
    }


    fun onFilterSelected(filterSelected: Long) {
        when (currentFilterType) {
            MASTER_FILTER_ID -> {
                currentFilterType = filterSelected
            }
            GENRE_FILTER_ID -> audioQueueManager.addFilter(Filter.GenreIs(getGenresValues()[filterSelected.toInt()].displayName))
            ARTIST_FILTER_ID -> audioQueueManager.addFilter(Filter.ArtistIs(filterSelected))
        }
    }

    class FilterItem(val id: Long, val displayName: String)
}