package be.florien.anyflow.feature.library.ui.info

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import be.florien.anyflow.common.ui.data.info.InfoActions
import be.florien.anyflow.common.ui.info.InfoViewModel
import be.florien.anyflow.common.ui.navigation.Navigator
import be.florien.anyflow.feature.library.domain.LibraryInfoActions
import be.florien.anyflow.feature.library.domain.LibraryRepository
import be.florien.anyflow.feature.library.ui.LibraryViewModel
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.model.Filter
import javax.inject.Inject

class LibraryInfoViewModel @Inject constructor(
    libraryRepository: LibraryRepository, //todo this doesn't get injected
    override val filtersManager: FiltersManager,
    override val navigator: Navigator,
    context: Context
) : InfoViewModel<Filter<*>?>(), LibraryViewModel {

    override val infoActions: InfoActions<Filter<*>?> =
        LibraryInfoActions(libraryRepository, context)
    override val areFiltersInEdition: LiveData<Boolean> = MutableLiveData(true)

    var filterNavigation: Filter<*>? = null
        set(value) {
            field = value
            updateRows()
        }

    override suspend fun getInfoRowList(): MutableList<InfoActions.InfoRow> =
        infoActions.getInfoRows(filterNavigation).toMutableList()

    override suspend fun getActionsRowsFor(row: InfoActions.InfoRow): List<InfoActions.InfoRow> =
        infoActions.getActionsRows(filterNavigation, row)

    override fun executeAction(row: InfoActions.InfoRow) = true

    companion object {
        const val GENRE_ID = "Genre"
        const val ALBUM_ARTIST_ID = "AlbumArtist"
        const val ARTIST_ID = "Artist"
        const val ALBUM_ID = "Album"
        const val SONG_ID = "Song"
        const val PLAYLIST_ID = "Playlist"
        const val DOWNLOAD_ID = "Download"
        const val PODCAST_EPISODE_ID = "PodcastEpisode"
    }
}