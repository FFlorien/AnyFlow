package be.florien.anyflow.feature.player.filter.selection

import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.player.FiltersManager
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class SelectFilterGenreViewModel @Inject constructor(dataRepository: DataRepository, filtersManager: FiltersManager) : SelectFilterViewModel(filtersManager) {

    override val itemDisplayType = ITEM_LIST
    private var currentId = 0L

    init {
        subscribe(dataRepository
                .getGenres { genre -> FilterItem(currentId++, genre, isSelected = filtersManager.isFilterInEdition(Filter.GenreIs(genre))) }
                .observeOn(AndroidSchedulers.mainThread()),
                onNext = { genres ->
                    values.mutable.value = genres
                })
    }

    override fun getFilter(filterValue: FilterItem) =
            Filter.GenreIs(values.value?.get(filterValue.id.toInt())?.displayName ?: "")
}