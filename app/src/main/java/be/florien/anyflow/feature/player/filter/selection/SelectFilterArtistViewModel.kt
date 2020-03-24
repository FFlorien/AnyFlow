package be.florien.anyflow.feature.player.filter.selection

import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.player.FiltersManager
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class SelectFilterArtistViewModel @Inject constructor(dataRepository: DataRepository, filtersManager: FiltersManager) : SelectFilterViewModel(filtersManager) {
    override val itemDisplayType = ITEM_LIST

    init {
        subscribe(dataRepository
                .getArtists { artist -> FilterItem(artist.id, artist.name, artist.art, filtersManager.isFilterInEdition(Filter.ArtistIs(artist.id, artist.name, artist.art))) }
                .observeOn(AndroidSchedulers.mainThread()), onNext = { artists ->
            values.mutable.value = artists
        })
    }

    override fun getFilter(filterValue: FilterItem) = Filter.ArtistIs(filterValue.id, filterValue.displayName, filterValue.artUrl)
}