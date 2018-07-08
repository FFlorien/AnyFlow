package be.florien.ampacheplayer.view.player.filter.addition

import android.app.Activity
import be.florien.ampacheplayer.BR
import be.florien.ampacheplayer.persistence.local.LibraryDatabase
import be.florien.ampacheplayer.player.Filter
import be.florien.ampacheplayer.view.player.PlayerActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class AddFilterFragmentGenreVM(activity: Activity) : AddFilterFragmentVM<String>() {
    @Inject lateinit var libraryDatabase: LibraryDatabase

    init {
        (activity as PlayerActivity).activityComponent.inject(this)
        subscribe(libraryDatabase.getGenres().observeOn(AndroidSchedulers.mainThread()), onNext = { genres ->
            values.clear()
            values.addAll(genres)
            notifyPropertyChanged(BR.displayedValues)
        })
    }

    override fun getDisplayedValues(): List<FilterItem> = values.mapIndexed{ index, genre -> FilterItem(index.toLong(), genre) }

    override fun onFilterSelected(filterValue: FilterItem) {
        subscribe(libraryDatabase.addFilters(Filter.GenreIs(values[filterValue.id.toInt()]).toDbFilter()))
    }
}