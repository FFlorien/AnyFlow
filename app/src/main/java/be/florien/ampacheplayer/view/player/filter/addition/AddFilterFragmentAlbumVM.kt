package be.florien.ampacheplayer.view.player.filter.addition

import android.app.Activity
import be.florien.ampacheplayer.BR
import be.florien.ampacheplayer.persistence.local.LocalDataManager
import be.florien.ampacheplayer.persistence.local.model.Album
import be.florien.ampacheplayer.persistence.local.model.Filter
import be.florien.ampacheplayer.view.player.PlayerActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class AddFilterFragmentAlbumVM(activity: Activity) : AddFilterFragmentVM<Album>() {
    @Inject lateinit var localDataManager: LocalDataManager

    init {
        (activity as PlayerActivity).activityComponent.inject(this)
        subscribe(localDataManager.getAlbums().observeOn(AndroidSchedulers.mainThread()), onNext = {albums ->
            values.clear()
            values.addAll(albums)
            notifyPropertyChanged(BR.displayedValues)
        })
    }

    override fun getDisplayedValues(): List<FilterItem> = values.map{ album -> FilterItem(album.id, album.name, album.art) }

    override fun onFilterSelected(filterValue: Long) {
        subscribe(localDataManager.addFilters(Filter.AlbumIs(filterValue)))
    }
}