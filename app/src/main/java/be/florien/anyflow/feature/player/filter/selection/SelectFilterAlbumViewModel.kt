package be.florien.anyflow.feature.player.filter.selection

import android.text.Editable
import android.text.TextWatcher
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.local.model.DbAlbumDisplay
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.player.FiltersManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

class SelectFilterAlbumViewModel @Inject constructor(dataRepository: DataRepository, filtersManager: FiltersManager) : SelectFilterViewModel(filtersManager) {
    private var searchJob: Job? = null
    override var values: MutableLiveData<MutableLiveData<List<FilterItem>>> =
            MutableLiveData(dataRepository.getAlbums(::convert).mutable)

    override val itemDisplayType = ITEM_GRID
    override val searchTextWatcher: TextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            searchJob?.cancel()
            searchJob = viewModelScope.launch {
                delay(300)
                values.value = if (s.isEmpty()) {
                    dataRepository.getAlbums(::convert).mutable
                } else {// todo move this to dataconverter or datarepo
                    dataRepository.getAlbumsFiltered(s.toString(), ::convert).mutable
                }
            }
        }
    }

    override fun getFilter(filterValue: FilterItem) = Filter.AlbumIs(filterValue.id, filterValue.displayName, filterValue.artUrl)

    private fun convert(album: DbAlbumDisplay) =
            FilterItem(album.id, "${album.name}\nby ${album.artistName}", album.art, filtersManager.isFilterInEdition(Filter.AlbumIs(album.id, album.name, album.art)))

}