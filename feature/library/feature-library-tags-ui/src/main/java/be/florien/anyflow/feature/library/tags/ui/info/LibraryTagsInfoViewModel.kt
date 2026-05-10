package be.florien.anyflow.feature.library.tags.ui.info

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.common.ui.domain.ImageConfig
import be.florien.anyflow.common.ui.domain.TextConfig
import be.florien.anyflow.common.utils.TimeOperations
import be.florien.anyflow.component.info.R
import be.florien.anyflow.feature.library.tags.domain.LibraryTagsRepository
import be.florien.anyflow.feature.library.tags.domain.model.IdText
import be.florien.anyflow.feature.library.ui.LibraryViewModel
import be.florien.anyflow.feature.library.ui.info.InfoRowDisplay
import be.florien.anyflow.feature.library.ui.info.LibraryActionType
import be.florien.anyflow.feature.library.ui.info.LibraryFieldType
import be.florien.anyflow.feature.library.ui.info.LibraryInfoRow
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
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class LibraryTagsInfoViewModel @Inject constructor(
    val libraryTagsRepository: LibraryTagsRepository,
    override val filtersManager: FiltersManager,
    override val navigator: Navigator
) : ViewModel(), LibraryViewModel {
    private val mutableLibraryInfoRows: MutableStateFlow<List<LibraryInfoRow>> =
        MutableStateFlow(listOf())
    private val libraryInfoRows: StateFlow<List<LibraryInfoRow>> = mutableLibraryInfoRows

    override val areFiltersInEdition: LiveData<Boolean> = MutableLiveData(true)

    var filterNavigation: Filter? = null
        set(value) {
            field = value
            updateRows()
        }

    val state: Flow<PersistentList<InfoRowDisplay>> = libraryInfoRows.map { list ->
        list
            .map {
                it.toInfoRow()
            }
            .toPersistentList()
    }

    fun executeAction(row: LibraryInfoRow): Boolean {
        return false
    }

    fun getInfoRow(position: Int): LibraryInfoRow = libraryInfoRows.value[position]

    private fun updateRows() {
        viewModelScope.launch {
            mutableLibraryInfoRows.value = getInfoRowList()
        }
    }

    private suspend fun getInfoRowList(): MutableList<LibraryInfoRow> {
        val filteredInfo =
            withContext(Dispatchers.IO) { libraryTagsRepository.getFilteredInfo(filterNavigation) }
        return mutableListOf(
            LibraryInfoRow(
                LibraryFieldType.Tags.Duration,
                LibraryActionType.InfoTitle,
                filteredInfo.duration
            ),
            LibraryInfoRow(
                LibraryFieldType.Tags.Genre,
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
                LibraryFieldType.Tags.AlbumArtist,
                getAction(filteredInfo.albumArtists),
                filteredInfo.albumArtists
            ),
            LibraryInfoRow(
                LibraryFieldType.Tags.Album,
                getAction(filteredInfo.albums),
                filteredInfo.albums
            ),
            LibraryInfoRow(
                LibraryFieldType.Tags.Artist,
                getAction(filteredInfo.artists),
                filteredInfo.artists
            ),
            LibraryInfoRow(
                LibraryFieldType.Tags.Song,
                getAction(filteredInfo.songs),
                filteredInfo.songs
            ),
            LibraryInfoRow(
                LibraryFieldType.Tags.Downloaded,
                LibraryActionType.SubFilter,
                filteredInfo.downloaded
            ),
            LibraryInfoRow(
                LibraryFieldType.Tags.Playlist,
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

    private fun getAction(count: Int): LibraryActionType {
        return if (count > 1) LibraryActionType.SubFilter else LibraryActionType.InfoTitle
        TODO("More Actions")
    }

    private suspend fun LibraryInfoRow.toInfoRow(): InfoRowDisplay {
        return when (this.actionType) {
            LibraryActionType.InfoTitle -> {
                val idText = getIdText()

                val text = if (fieldType == LibraryFieldType.Tags.Duration) {
                    TextConfig(
                        TimeOperations.toMediaDuration(
                            count.toDuration(DurationUnit.SECONDS)
                        )
                    )
                } else {
                    TextConfig(idText.text, null)
                }
                val imageUrl = this.fieldType.artType?.let { artType ->
                    libraryTagsRepository.getArtUrl(artType, idText.id)
                }
                InfoRowDisplay(
                    ImageConfig(imageUrl, fieldType.iconRes),
                    this.fieldType.titleRes,
                    text,
                    null
                )
            }

            LibraryActionType.SubFilter -> InfoRowDisplay(
                ImageConfig(null, fieldType.iconRes),
                this.fieldType.titleRes,
                TextConfig(count.toString(), null),
                R.drawable.ic_go
            )
        }
    }

    private suspend fun LibraryInfoRow.getIdText(): IdText {
        if (fieldType !is LibraryFieldType.Tags) {
            IdText(0, "")
        }
        val filter = filterNavigation
        val filterType = getField(this.fieldType as LibraryFieldType.Tags)
        val filterIfTypePresent = filter?.getFilterIfTypePresent(filterType)
        val filterData: IdText? = filterIfTypePresent?.takeIf { it.argument is Long }
            ?.let { IdText(it.argument as Long, it.displayText) }
        return filterData ?: getFilteredInfo(
            filterType,
            filter
        ) ?: IdText(0, "")
    }

    private fun getField(
        filterType: LibraryFieldType.Tags
    ): TagFilterType {
        return when (filterType) {
            LibraryFieldType.Tags.Song -> TagFilterType.SONG_IS
            LibraryFieldType.Tags.Artist -> TagFilterType.ARTIST_IS
            LibraryFieldType.Tags.AlbumArtist -> TagFilterType.ALBUM_ARTIST_IS
            LibraryFieldType.Tags.Album -> TagFilterType.ALBUM_IS
            LibraryFieldType.Tags.Playlist -> TagFilterType.PLAYLIST_IS
            LibraryFieldType.Tags.Downloaded -> TagFilterType.DOWNLOADED_STATUS_IS
            LibraryFieldType.Tags.Duration -> TagFilterType.SONG_IS
            LibraryFieldType.Tags.Genre -> TagFilterType.SONG_IS
        }
    }

    private suspend fun getFilteredInfo(
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