package be.florien.anyflow.feature.library.tags.ui.info

import android.content.Context
import be.florien.anyflow.common.ui.data.info.InfoActions
import be.florien.anyflow.common.ui.navigation.Navigator
import be.florien.anyflow.feature.library.tags.domain.LibraryTagsInfoActions
import be.florien.anyflow.feature.library.tags.domain.LibraryTagsRepository
import be.florien.anyflow.feature.library.ui.LibraryViewModel
import be.florien.anyflow.feature.library.ui.info.LibraryInfoViewModel
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.model.Filter
import javax.inject.Inject

class LibraryTagsInfoViewModel @Inject constructor( //todo abstract this for infoAction ?
    libraryTagsRepository: LibraryTagsRepository, //todo this doesn't get injected
    filtersManager: FiltersManager,
    navigator: Navigator,
    context: Context
) : LibraryInfoViewModel(filtersManager, navigator), LibraryViewModel {

    override val infoActions: InfoActions<Filter<*>?> = LibraryTagsInfoActions(libraryTagsRepository, context)

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