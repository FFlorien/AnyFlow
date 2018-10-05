package be.florien.anyflow.view.player.filter.addition

import android.app.Activity
import be.florien.anyflow.BR
import be.florien.anyflow.persistence.local.LibraryDatabase
import be.florien.anyflow.persistence.local.model.ArtistDisplay
import be.florien.anyflow.player.Filter
import be.florien.anyflow.view.player.PlayerActivity
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