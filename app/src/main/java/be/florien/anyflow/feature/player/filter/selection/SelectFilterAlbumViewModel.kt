package be.florien.anyflow.feature.player.filter.selection

import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.player.FiltersManager
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class SelectFilterAlbumViewModel @Inject constructor(dataRepository: DataRepository, filtersManager: FiltersManager) : SelectFilterViewModel(filtersManager) {
    override val itemDisplayType = ITEM_GRID

    init {
        subscribe(dataRepository
                .getAlbums { album -> FilterItem(album.id, "${album.name}\nby ${album.artistName}", album.art, filtersManager.isFilterInEdition(Filter.AlbumIs(album.id, album.name, album.art))) }
                .observeOn(AndroidSchedulers.mainThread()),
                onNext = { albums ->
                    values.mutable.value = albums
                })
    }

    override fun getFilter(filterValue: FilterItem) = Filter.AlbumIs(filterValue.id, filterValue.displayName, filterValue.artUrl)
}