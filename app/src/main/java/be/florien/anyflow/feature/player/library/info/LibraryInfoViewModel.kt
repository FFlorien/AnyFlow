package be.florien.anyflow.feature.player.library.info

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.UrlRepository
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.feature.player.info.InfoActions
import be.florien.anyflow.feature.player.info.InfoViewModel
import be.florien.anyflow.feature.player.info.library.LibraryInfoActions
import be.florien.anyflow.feature.player.library.LibraryViewModel
import be.florien.anyflow.feature.playlist.PlaylistRepository
import be.florien.anyflow.player.FiltersManager
import javax.inject.Inject

class LibraryInfoViewModel @Inject constructor(
    override val filtersManager: FiltersManager,
    dataRepository: DataRepository,
    playlistRepository: PlaylistRepository,
    urlRepository: UrlRepository,
    context: Context
) : InfoViewModel<Filter<*>?>(), LibraryViewModel {

    override val infoActions: InfoActions<Filter<*>?> = LibraryInfoActions(dataRepository, playlistRepository, urlRepository, context)
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
    }
}