package be.florien.ampacheplayer.view.player.filter.addition

import android.app.Activity
import be.florien.ampacheplayer.BR
import be.florien.ampacheplayer.persistence.local.LibraryDatabase
import be.florien.ampacheplayer.persistence.local.model.ArtistDisplay
import be.florien.ampacheplayer.player.Filter
import be.florien.ampacheplayer.view.player.PlayerActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class AddFilterFragmentArtistVM(activity: Activity) : AddFilterFragmentVM<ArtistDisplay>() {

    @Inject lateinit var library: LibraryDatabase

    init {
        (activity as PlayerActivity).activityComponent.inject(this)
        subscribe(library.getArtists().observeOn(AndroidSchedulers.mainThread()), onNext = { artists ->
            values.clear()
            values.addAll(artists)
            notifyPropertyChanged(BR.displayedValues)
        })
    }

    override fun getDisplayedValues(): List<FilterItem> = values.map{ artist -> FilterItem(artist.id, artist.name) }

    override fun onFilterSelected(filterValue: FilterItem) {
        subscribe(library.addFilters(Filter.ArtistIs(filterValue.id, filterValue.displayName).toDbFilter()))
    }
}