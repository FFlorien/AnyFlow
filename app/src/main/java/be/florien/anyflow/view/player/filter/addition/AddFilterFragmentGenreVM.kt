package be.florien.anyflow.view.player.filter.addition

import android.app.Activity
import be.florien.anyflow.BR
import be.florien.anyflow.persistence.local.LibraryDatabase
import be.florien.anyflow.player.Filter
import be.florien.anyflow.view.player.PlayerActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class AddFilterFragmentGenreVM(activity: Activity) : AddFilterFragmentVM<String>() {

    override val itemDisplayType = ITEM_LIST
    @Inject
    lateinit var libraryDatabase: LibraryDatabase

    init {
        (activity as PlayerActivity).activityComponent.inject(this)
        subscribe(libraryDatabase.getGenres().observeOn(AndroidSchedulers.mainThread()), onNext = { genres ->
            values.clear()
            values.addAll(genres)
            notifyPropertyChanged(BR.displayedValues)
        })
    }

    override fun getDisplayedValues(): List<FilterItem> = values.mapIndexed { index, genre -> FilterItem(index.toLong(), genre, isSelected = filtersManager.isFilterInEdition(Filter.GenreIs(genre))) }

    override fun getFilter(filterValue: FilterItem) = Filter.GenreIs(values[filterValue.id.toInt()])
}