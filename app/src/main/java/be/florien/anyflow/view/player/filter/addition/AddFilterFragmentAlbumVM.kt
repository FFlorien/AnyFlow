package be.florien.anyflow.view.player.filter.addition

import android.app.Activity
import be.florien.anyflow.BR
import be.florien.anyflow.persistence.local.LibraryDatabase
import be.florien.anyflow.persistence.local.model.AlbumDisplay
import be.florien.anyflow.player.Filter
import be.florien.anyflow.player.FiltersManager
import be.florien.anyflow.view.player.PlayerActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class AddFilterFragmentAlbumVM(activity: Activity) : AddFilterFragmentVM<AlbumDisplay>() {
    override val itemDisplayType = ITEM_GRID
    @Inject
    lateinit var localDataManager: LibraryDatabase

    init {
        (activity as PlayerActivity).activityComponent.inject(this)
        subscribe(localDataManager.getAlbums().observeOn(AndroidSchedulers.mainThread()), onNext = { albums ->
            values.clear()
            values.addAll(albums)
            notifyPropertyChanged(BR.displayedValues)
        })
    }

    override fun getDisplayedValues(): List<FilterItem> = values.map { album -> FilterItem(album.id, "${album.name}\nby\n${album.artistName}", album.art) }

    override fun onFilterSelected(filterValue: FilterItem) {
        filtersManager.addFilter(Filter.AlbumIs(filterValue.id, filterValue.displayName, filterValue.artUrl))
    }
}