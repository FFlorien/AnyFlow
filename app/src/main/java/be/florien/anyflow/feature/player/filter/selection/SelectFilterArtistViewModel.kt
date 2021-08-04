package be.florien.anyflow.feature.player.filter.selection

import android.text.Editable
import android.text.TextWatcher
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.local.model.DbArtistDisplay
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.player.FiltersManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

class SelectFilterArtistViewModel @Inject constructor(dataRepository: DataRepository, filtersManager: FiltersManager) : SelectFilterViewModel(filtersManager) {
    private var searchJob: Job? = null
    override val values: MutableLiveData<MutableLiveData<List<FilterItem>>> = MutableLiveData(
            dataRepository.getArtists(::convert).mutable)
    override val itemDisplayType = ITEM_LIST
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
                    dataRepository.getArtists(::convert).mutable
                } else {// todo move this to dataconverter or datarepo
                    dataRepository.getArtistsFiltered(s.toString(), ::convert).mutable
                }
            }
        }
    }

    override fun getFilter(filterValue: FilterItem) = Filter.ArtistIs(filterValue.id, filterValue.displayName, filterValue.artUrl)

    private fun convert(artist: DbArtistDisplay) = FilterItem(artist.id, artist.name, artist.art, filtersManager.isFilterInEdition(Filter.ArtistIs(artist.id, artist.name, artist.art)))
}