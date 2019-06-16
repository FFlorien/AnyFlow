package be.florien.anyflow.view.player.filter.selection

import android.app.Activity
import be.florien.anyflow.BR
import be.florien.anyflow.persistence.local.LibraryDatabase
import be.florien.anyflow.player.Filter
import be.florien.anyflow.view.player.PlayerActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class SelectFilterFragmentGenreVM(activity: Activity) : SelectFilterFragmentVM() {

    override val itemDisplayType = ITEM_LIST
    @Inject
    lateinit var libraryDatabase: LibraryDatabase
    private var currentId = 0L

    init {
        (activity as PlayerActivity).activityComponent.inject(this)
        subscribe(libraryDatabase
                .getGenres { genre -> FilterItem(currentId++, genre, isSelected = filtersManager.isFilterInEdition(Filter.GenreIs(genre))) }
                .observeOn(AndroidSchedulers.mainThread()),
                onNext = { genres ->
                    values = genres
                    notifyPropertyChanged(BR.values)
                })
    }

    override fun getFilter(filterValue: FilterItem) = Filter.GenreIs(values?.get(filterValue.id.toInt())?.displayName
            ?: "")

    override fun downloadItem(id: Long) {
        values?.get(id.toInt())?.let {downloadHelper.addGenreDownload(it.displayName)}
    }
}