package be.florien.anyflow.feature.player.library.info

import android.content.Context
import androidx.lifecycle.LiveData
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.feature.player.library.LibraryActions
import be.florien.anyflow.feature.player.info.InfoActions
import be.florien.anyflow.feature.player.info.InfoViewModel
import be.florien.anyflow.feature.player.info.filter.FilterInfoActions
import be.florien.anyflow.player.FiltersManager
import javax.inject.Inject

class LibraryInfoViewModel @Inject constructor(
    private val libraryActions: LibraryActions,
    dataRepository: DataRepository,
    context: Context
) : InfoViewModel<Filter<*>?>(), LibraryActions {

    override val filtersManager: FiltersManager
        get() = libraryActions.filtersManager
    override val areFiltersInEdition: LiveData<Boolean>
        get() = libraryActions.areFiltersInEdition
    override val currentFilters: LiveData<List<Filter<*>>>
        get() = libraryActions.currentFilters
    override val hasChangeFromCurrentFilters: LiveData<Boolean>
        get() = libraryActions.hasChangeFromCurrentFilters

    override val infoActions: InfoActions<Filter<*>?> = FilterInfoActions(dataRepository, context)

    var filterNavigation: Filter<*>? = null
        set(value) {
            field = value
            updateRows()
        }

    override suspend fun getInfoRowList(): MutableList<InfoActions.InfoRow> =
        infoActions.getInfoRows(filterNavigation).toMutableList()

    override suspend fun getActionsRows(field: InfoActions.FieldType): List<InfoActions.InfoRow> =
        infoActions.getActionsRows(filterNavigation, field)

    override fun executeInfoAction(
        fieldType: InfoActions.FieldType,
        actionType: InfoActions.ActionType
    ) {
    }

    override fun mapActionsRows(initialList: List<InfoActions.InfoRow>): List<InfoActions.InfoRow> =
        initialList

    override suspend fun confirmChanges() {
        libraryActions.confirmChanges()
    }

    override fun cancelChanges() {
        libraryActions.cancelChanges()
    }

    override suspend fun saveFilterGroup(name: String) {
        libraryActions.saveFilterGroup(name)
    }

    companion object {
        const val GENRE_ID = "Genre"
        const val ALBUM_ARTIST_ID = "AlbumArtist"
        const val ARTIST_ID = "Artist"
        const val ALBUM_ID = "Album"
        const val SONG_ID = "Song"
        const val PLAYLIST_ID = "Playlist"
        const val DOWNLOAD_ID = "Download"
    }
}