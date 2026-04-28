package be.florien.anyflow.feature.library.tags.ui.info

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.common.ui.data.ImageConfig
import be.florien.anyflow.common.ui.data.TextConfig
import be.florien.anyflow.component.info.InfoRow
import be.florien.anyflow.feature.library.tags.domain.LibraryInfoRow
import be.florien.anyflow.feature.library.tags.domain.LibraryTagsActionType
import be.florien.anyflow.feature.library.tags.domain.LibraryTagsFieldType
import be.florien.anyflow.feature.library.tags.domain.LibraryTagsRepository
import be.florien.anyflow.feature.library.ui.LibraryViewModel
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.management.filters.domain.model.TagFilterType
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Immutable
data class InfoRowDisplay(
    val imageConfig: ImageConfig,
    @StringRes
    val title: Int,
    val info: TextConfig,
    @DrawableRes
    val actionIcon: Int?
)

class LibraryTagsInfoViewModel @Inject constructor(
    val libraryTagsRepository: LibraryTagsRepository, //todo this doesn't get injected
    override val filtersManager: FiltersManager,
    override val navigator: Navigator
) : ViewModel(), LibraryViewModel {
    private val mutableLibraryInfoRows: MutableStateFlow<List<LibraryInfoRow>> =
        MutableStateFlow(listOf())
    val libraryInfoRows: StateFlow<List<LibraryInfoRow>> = mutableLibraryInfoRows
    private val mutableInfoRows: MutableStateFlow<List<InfoRow>> = MutableStateFlow(listOf())
    val infoRows: Flow<List<InfoRow>> = mutableInfoRows

    val state: Flow<PersistentList<InfoRowDisplay>> = infoRows.map { list ->
        list
            .map { infoRow ->
                InfoRowDisplay(
                    infoRow.image,
                    infoRow.title,
                    infoRow.text,
                    infoRow.actionIcon
                )
            }
            .toPersistentList()
    }

    override val areFiltersInEdition: LiveData<Boolean> = MutableLiveData(true)

    var filterNavigation: Filter? = null
        set(value) {
            field = value
            updateRows()
        }

    /**
     * Public methods
     */

    fun updateRows() {
        viewModelScope.launch {
            mutableLibraryInfoRows.value = getInfoRowList()
        }
    }

    fun getArtUrl(artType: String, id: Long): String? =
        libraryTagsRepository.getArtUrl(artType, id)

    suspend fun getInfoRowList(): MutableList<LibraryInfoRow> {
        val filteredInfo =
            withContext(Dispatchers.IO) { libraryTagsRepository.getFilteredInfo(filterNavigation) }
        return mutableListOf(
            LibraryInfoRow(
                LibraryTagsFieldType.Duration,
                LibraryTagsActionType.InfoTitle,
                filteredInfo.duration
            ),
            LibraryInfoRow(
                LibraryTagsFieldType.Genre,
                getAction(filteredInfo.genres.minus(filterNavigation?.let { source ->
                    val genreFilters = mutableSetOf<Long>()
                    source.forEach { filter ->
                        if (filter.type == TagFilterType.GENRE_IS) {
                            genreFilters.add(filter.argument as Long)
                        }
                    }
                    genreFilters.size
                } ?: 0)), // todo "Electro and 3 other genres" instead
                filteredInfo.genres
            ),
            LibraryInfoRow(
                LibraryTagsFieldType.AlbumArtist,
                getAction(filteredInfo.albumArtists),
                filteredInfo.albumArtists
            ),
            LibraryInfoRow(
                LibraryTagsFieldType.Album,
                getAction(filteredInfo.albums),
                filteredInfo.albums
            ),
            LibraryInfoRow(
                LibraryTagsFieldType.Artist,
                getAction(filteredInfo.artists),
                filteredInfo.artists
            ),
            LibraryInfoRow(
                LibraryTagsFieldType.Song,
                getAction(filteredInfo.songs),
                filteredInfo.songs
            ),
            LibraryInfoRow(
                LibraryTagsFieldType.Downloaded,
                LibraryTagsActionType.SubFilter,
                filteredInfo.downloaded
            ),
            LibraryInfoRow(
                LibraryTagsFieldType.Playlist,
                getAction(filteredInfo.playlists.minus(filterNavigation?.let { source ->
                    val playlistFilters = mutableSetOf<Long>()
                    source.forEach { filter ->
                        if (filter.type == TagFilterType.PLAYLIST_IS) {
                            playlistFilters.add(filter.argument as Long)
                        }
                    }
                    playlistFilters.size
                } ?: 0)), // todo "Motivation and 3 other playlists" instead
                filteredInfo.playlists
            )
        )
    }

    fun executeAction(row: LibraryInfoRow): Boolean {
        TODO("Not yet implemented")
    }

    private fun getAction(count: Int): LibraryTagsActionType {
        return if (count > 1) LibraryTagsActionType.SubFilter else LibraryTagsActionType.InfoTitle
        TODO("More Actions")
    }

    suspend fun getFilteredInfo(
        filterType: TagFilterType,
        filter: Filter?
    ) = when (filterType) {
        TagFilterType.SONG_IS -> libraryTagsRepository.getSongFiltered(filter)
        TagFilterType.ARTIST_IS -> libraryTagsRepository.getArtistFiltered(filter)
        TagFilterType.ALBUM_ARTIST_IS -> libraryTagsRepository.getAlbumArtistFiltered(
            filter
        )

        TagFilterType.ALBUM_IS -> libraryTagsRepository.getAlbumFiltered(filter)
        TagFilterType.GENRE_IS -> libraryTagsRepository.getGenreFiltered(filter)
        TagFilterType.PLAYLIST_IS -> libraryTagsRepository.getPlaylistFiltered(filter)
        TagFilterType.DOWNLOADED_STATUS_IS,
        TagFilterType.DISK_IS -> listOf(null)

    }.firstOrNull()

    fun setInfoRows(infoRow: List<InfoRow>) {
        mutableInfoRows.value = infoRow
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