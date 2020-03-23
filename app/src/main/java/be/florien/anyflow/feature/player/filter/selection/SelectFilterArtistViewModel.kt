package be.florien.anyflow.feature.player.filter.selection

import be.florien.anyflow.data.local.LibraryDatabase
import be.florien.anyflow.player.Filter
import be.florien.anyflow.player.FiltersManager
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class SelectFilterArtistViewModel @Inject constructor(library: LibraryDatabase, filtersManager: FiltersManager) : SelectFilterViewModel(filtersManager) {
    override val itemDisplayType = ITEM_LIST

    init {
        subscribe(library
                .getArtists { artist -> FilterItem(artist.id, artist.name, artist.art, filtersManager.isFilterInEdition(Filter.ArtistIs(artist.id, artist.name, artist.art))) }
                .observeOn(AndroidSchedulers.mainThread()), onNext = { artists ->
            values.mutable.value = artists
        })
    }

    override fun getFilter(filterValue: FilterItem) = Filter.ArtistIs(filterValue.id, filterValue.displayName, filterValue.artUrl)
}