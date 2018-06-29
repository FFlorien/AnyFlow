package be.florien.ampacheplayer.view.player.filter.addition

import android.app.Activity
import be.florien.ampacheplayer.BR
import be.florien.ampacheplayer.persistence.local.LocalDataManager
import be.florien.ampacheplayer.persistence.local.model.Artist
import be.florien.ampacheplayer.persistence.local.model.Filter
import be.florien.ampacheplayer.view.player.PlayerActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class AddFilterFragmentArtistVM(activity: Activity) : AddFilterFragmentVM<Artist>() {

    @Inject lateinit var localDataManager: LocalDataManager

    init {
        (activity as PlayerActivity).activityComponent.inject(this)
        subscribe(localDataManager.getArtists().observeOn(AndroidSchedulers.mainThread()), onNext = {artists ->
            values.clear()
            values.addAll(artists)
            notifyPropertyChanged(BR.displayedValues)
        })
    }

    override fun getDisplayedValues(): List<FilterItem> = values.map{ artist -> FilterItem(artist.id, artist.name) }

    override fun onFilterSelected(filterValue: Long) {
        subscribe(localDataManager.addFilters(Filter.ArtistIs(filterValue)))
    }
}