package be.florien.anyflow.view.player.filter.selection

import android.app.Activity
import be.florien.anyflow.BR
import be.florien.anyflow.persistence.local.LibraryDatabase
import be.florien.anyflow.player.Filter
import be.florien.anyflow.view.player.PlayerActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class SelectFilterFragmentArtistVM(activity: Activity) : SelectFilterFragmentVM() {
    override val itemDisplayType = ITEM_LIST

    @Inject lateinit var library: LibraryDatabase

    init {
        (activity as PlayerActivity).activityComponent.inject(this)
        subscribe(library
                .getArtists{ artist -> FilterItem(artist.id, artist.name, artist.art, filtersManager.isFilterInEdition(Filter.ArtistIs(artist.id, artist.name, artist.art))) }
                .observeOn(AndroidSchedulers.mainThread()), onNext = { artists ->
            values = artists
            notifyPropertyChanged(BR.values)
        })
    }

    override fun getFilter(filterValue: FilterItem) = Filter.ArtistIs(filterValue.id, filterValue.displayName, filterValue.artUrl)
}