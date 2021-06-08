package be.florien.anyflow.feature.player.filter.selection

import android.text.Editable
import android.text.TextWatcher
import androidx.lifecycle.LiveData
import androidx.paging.PagingData
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.local.model.DbAlbumDisplay
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.player.FiltersManager
import javax.inject.Inject

class SelectFilterAlbumViewModel @Inject constructor(dataRepository: DataRepository, filtersManager: FiltersManager) : SelectFilterViewModel(filtersManager) {
    override var values: LiveData<PagingData<FilterItem>> =
            dataRepository.getAlbums(::convert)

    override val itemDisplayType = ITEM_GRID
    override val searchTextWatcher: TextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            values = if (s.isEmpty()) {
                dataRepository.getAlbums(::convert)
            } else {// todo move this to dataconverter or datarepo
                dataRepository.getAlbumsFiltered(s.toString(), ::convert)
            }
        }
    }

    override fun getFilter(filterValue: FilterItem) = Filter.AlbumIs(filterValue.id, filterValue.displayName, filterValue.artUrl)

    private fun convert(album: DbAlbumDisplay) =
            FilterItem(album.id, "${album.name}\nby ${album.artistName}", album.art, filtersManager.isFilterInEdition(Filter.AlbumIs(album.id, album.name, album.art)))

}