package be.florien.anyflow.feature.player.filter.selectType

import androidx.lifecycle.LiveData
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.feature.player.filter.FilterActions
import be.florien.anyflow.feature.player.info.InfoActions
import be.florien.anyflow.feature.player.info.InfoViewModel
import be.florien.anyflow.feature.player.info.filter.FilterInfoActions
import be.florien.anyflow.player.FiltersManager
import javax.inject.Inject

class SelectFilterTypeViewModel @Inject constructor(
    private val filterActions: FilterActions,
    dataRepository: DataRepository
) : InfoViewModel<Filter<*>?>(), FilterActions {

    var filter: Filter<*>? = null
        set(value) {
            field = value
            updateRows()
        }

    override val infoActions: InfoActions<Filter<*>?> = FilterInfoActions(dataRepository)

    override suspend fun getInfoRowList(): MutableList<InfoActions.InfoRow> =
        infoActions.getInfoRows(filter).toMutableList()

    override suspend fun getActionsRows(field: InfoActions.FieldType): List<InfoActions.InfoRow> =
        infoActions.getActionsRows(filter, field)

    override fun executeInfoAction(
        fieldType: InfoActions.FieldType,
        actionType: InfoActions.ActionType
    ) {
        //todo
    }

    override fun mapActionsRows(initialList: List<InfoActions.InfoRow>): List<InfoActions.InfoRow> =
        initialList

    override val filtersManager: FiltersManager
        get() = filterActions.filtersManager
    override val areFiltersInEdition: LiveData<Boolean>
        get() = filterActions.areFiltersInEdition
    override val currentFilters: LiveData<List<Filter<*>>>
        get() = filterActions.currentFilters
    override val hasChangeFromCurrentFilters: LiveData<Boolean>
        get() = filterActions.hasChangeFromCurrentFilters

    override suspend fun confirmChanges() {
        filterActions.confirmChanges()
    }

    override fun cancelChanges() {
        filterActions.cancelChanges()
    }

    override suspend fun saveFilterGroup(name: String) {
        filterActions.saveFilterGroup(name)
    }

    companion object {
        const val GENRE_ID = "Genre"
        const val ARTIST_ID = "Artist"
        const val ALBUM_ID = "Album"
        const val SONG_ID = "Song"
        const val PLAYLIST_ID = "Playlist"
        const val DOWNLOAD_ID = "Download"
    }
}