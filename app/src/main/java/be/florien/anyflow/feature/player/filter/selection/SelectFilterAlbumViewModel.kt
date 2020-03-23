package be.florien.anyflow.feature.player.filter.selection

import be.florien.anyflow.data.local.LibraryDatabase
import be.florien.anyflow.player.Filter
import be.florien.anyflow.player.FiltersManager
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class SelectFilterAlbumViewModel  @Inject constructor(localDataManager: LibraryDatabase, filtersManager: FiltersManager)  : SelectFilterViewModel(filtersManager) {
    override val itemDisplayType = ITEM_GRID

    init {
        subscribe(localDataManager
                .getAlbums { album -> FilterItem(album.id, "${album.name}\nby ${album.artistName}", album.art, filtersManager.isFilterInEdition(Filter.AlbumIs(album.id, album.name, album.art))) }
                .observeOn(AndroidSchedulers.mainThread()),
                onNext = { albums ->
                    values.mutable.value = albums
                })
    }

    override fun getFilter(filterValue: FilterItem) = Filter.AlbumIs(filterValue.id, filterValue.displayName, filterValue.artUrl)
}