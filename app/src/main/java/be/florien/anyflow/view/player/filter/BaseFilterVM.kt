package be.florien.anyflow.view.player.filter

import be.florien.anyflow.player.FiltersManager
import be.florien.anyflow.view.BaseVM
import javax.inject.Inject

open class BaseFilterVM: BaseVM() {
    @Inject
    lateinit var filtersManager: FiltersManager

    fun confirmChanges() {
        subscribe(
                completable = filtersManager.commitChanges(),
                onComplete = {}//todo alert playerActivity?
        )
    }
}