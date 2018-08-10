package be.florien.anyflow.view.player.filter.addition

import android.app.Activity
import be.florien.anyflow.BR
import be.florien.anyflow.persistence.local.LibraryDatabase
import be.florien.anyflow.persistence.local.model.AlbumDisplay
import be.florien.anyflow.player.Filter
import be.florien.anyflow.view.player.PlayerActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class AddFilterFragmentAlbumVM(activity: Activity) : AddFilterFragmentVM<AlbumDisplay>() {
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

    override fun getDisplayedValues(): List<FilterItem> = values.map { album -> FilterItem(album.id, album.name, album.art) }

    override fun onFilterSelected(filterValue: FilterItem) {
        subscribe(localDataManager.addFilters(Filter.AlbumIs(filterValue.id, filterValue.displayName).toDbFilter()))
    }
}