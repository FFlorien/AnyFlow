package be.florien.anyflow.view.player.filter.selection

import android.app.Activity
import be.florien.anyflow.BR
import be.florien.anyflow.persistence.local.LibraryDatabase
import be.florien.anyflow.player.Filter
import be.florien.anyflow.view.player.PlayerActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class SelectFilterFragmentAlbumVM(activity: Activity) : SelectFilterFragmentVM() {
    override val itemDisplayType = ITEM_GRID
    @Inject
    lateinit var localDataManager: LibraryDatabase

    init {
        (activity as PlayerActivity).activityComponent.inject(this)
        subscribe(localDataManager
                .getAlbums { album -> FilterItem(album.id, "${album.name}\nby ${album.artistName}", album.art, filtersManager.isFilterInEdition(Filter.AlbumIs(album.id, album.name, album.art))) }
                .observeOn(AndroidSchedulers.mainThread()),
                onNext = { albums ->
                    values = albums
                    notifyPropertyChanged(BR.values)
                })
    }

    override fun getFilter(filterValue: FilterItem) = Filter.AlbumIs(filterValue.id, filterValue.displayName, filterValue.artUrl)

    override fun downloadItem(id: Long) {
        downloadHelper.addAlbumDownload(id)
    }
}