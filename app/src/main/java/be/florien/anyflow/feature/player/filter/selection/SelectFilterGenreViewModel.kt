package be.florien.anyflow.feature.player.filter.selection

import android.text.Editable
import android.text.TextWatcher
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.player.FiltersManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

class SelectFilterGenreViewModel @Inject constructor(private val dataRepository: DataRepository, filtersManager: FiltersManager) : SelectFilterViewModel(filtersManager) {
    private var searchJob: Job?= null
    override var values: MutableLiveData<MutableLiveData<List<FilterItem>>> =
            MutableLiveData(dataRepository.getGenres(::convert).mutable) // todo move this to dataconverter or datarepo

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
                    dataRepository.getGenres(::convert).mutable
                } else {// todo move this to dataconverter or datarepo
                    dataRepository.getGenresFiltered(s.toString(), ::convert).mutable
                }
            }
        }
    }
    private var currentId = 0L

    override fun getFilter(filterValue: FilterItem) = Filter.GenreIs(filterValue.displayName)

    private fun convert(genre: String) = FilterItem(currentId++, genre, isSelected = filtersManager.isFilterInEdition(Filter.GenreIs(genre)))
}