package be.florien.ampacheplayer.view.player.filter

import android.databinding.Bindable
import be.florien.ampacheplayer.BR
import be.florien.ampacheplayer.persistence.DatabaseManager
import be.florien.ampacheplayer.persistence.PersistenceManager
import be.florien.ampacheplayer.player.AudioQueueManager
import be.florien.ampacheplayer.player.Filter
import be.florien.ampacheplayer.view.BaseVM
import javax.inject.Inject

const val MASTER_FILTER = "filterSelection"
const val GENRE_FILTER = "Genre"
const val ARTIST_FILTER = "Artist"
const val ALBUM_FILTER = "Album"
const val TITLE_FILTER = "Title"

/**
 * Created by FlamentF on 08-Jan-18.
 */
class FilterFragmentVM
@Inject constructor(
        private val databaseManager: DatabaseManager,
        val persistenceManager: PersistenceManager,
        private val audioQueueManager: AudioQueueManager) : BaseVM() {

    private val filtersNames = listOf(
            GENRE_FILTER,
            ARTIST_FILTER,
            ALBUM_FILTER,
            TITLE_FILTER)

    private val genres = databaseManager.getGenres() // todo transform in fun !

    var currentFilterType = MASTER_FILTER
        set(value) {
            field = value
            notifyPropertyChanged(BR.filterList)
        }

    @Bindable
    fun getFilterList(): List<String> = when (currentFilterType) {
        MASTER_FILTER -> filtersNames
        GENRE_FILTER -> genres
        else -> filtersNames
    }

    fun onFilterSelected(filterSelected: String) {
        when (currentFilterType) {
            MASTER_FILTER -> {
                currentFilterType = filterSelected
            }
            GENRE_FILTER -> audioQueueManager.addFilter(Filter.GenreIs(filterSelected))
        }
    }
}