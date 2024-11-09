package be.florien.anyflow.feature.song.base.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import be.florien.anyflow.common.ui.data.ImageConfig
import be.florien.anyflow.component.info.InfoViewModel
import be.florien.anyflow.feature.song.base.domain.model.BaseSongInfoRow
import be.florien.anyflow.feature.song.base.domain.model.SongActionType
import be.florien.anyflow.feature.song.base.domain.model.SongFieldType
import be.florien.anyflow.management.filters.model.Filter
import be.florien.anyflow.tags.local.model.DownloadProgressState
import be.florien.anyflow.tags.model.SongInfo

abstract class BaseSongViewModel : InfoViewModel<BaseSongInfoRow>() {
    protected val songInfoMediator = MediatorLiveData<SongInfo>()
    val songInfoObservable: LiveData<SongInfo> = songInfoMediator
    val coverConfig: LiveData<ImageConfig> = MutableLiveData()
    val songInfo: SongInfo
        get() {
            val value = songInfoMediator.value
            return value ?: SongInfo.dummySongInfo()
        }
    abstract var songId: Long

    override fun executeAction(row: BaseSongInfoRow) = when (row.actionType) {
        SongActionType.ExpandableTitle -> true
        SongActionType.None,
        SongActionType.InfoTitle -> true

        else -> false
    }

    override suspend fun getInfoRowList(): MutableList<BaseSongInfoRow> {
        //todo get shortcuts here (or in a subVM?), and pass on the order
        return listOfNotNull(
            getInfoRow(
                SongFieldType.Title,
                SongActionType.ExpandableTitle,
                subRows = listOfNotNull(
                    getSongAction(songInfo, SongFieldType.Title, SongActionType.AddNext),
                    getSongAction(songInfo, SongFieldType.Title, SongActionType.AddToFilter),
                    getSongAction(songInfo, SongFieldType.Title, SongActionType.Search),
                    getSongAction(songInfo, SongFieldType.Title, SongActionType.AddToPlaylist),
                    if (songInfo.local.isNullOrBlank()) {
                        getSongAction(songInfo, SongFieldType.Title, SongActionType.Download)
                    } else {
                        getSongAction(songInfo, SongFieldType.Title, SongActionType.None)
                    }
                )
            ),
            getInfoRow(
                SongFieldType.Artist,
                SongActionType.ExpandableTitle,
                subRows = listOfNotNull(
                    getSongAction(songInfo, SongFieldType.Artist, SongActionType.AddToFilter),
                    getSongAction(songInfo, SongFieldType.Artist, SongActionType.Search),
                    getSongAction(songInfo, SongFieldType.Artist, SongActionType.AddToPlaylist),
                    getSongAction(songInfo, SongFieldType.Artist, SongActionType.Download)
                )
            ),
            getInfoRow(
                SongFieldType.Track,
                SongActionType.InfoTitle,
            ),
            getInfoRow(
                SongFieldType.Album,
                SongActionType.ExpandableTitle,
                subRows = listOfNotNull(
                    getSongAction(songInfo, SongFieldType.Album, SongActionType.AddToFilter),
                    getSongAction(songInfo, SongFieldType.Album, SongActionType.Search),
                    getSongAction(songInfo, SongFieldType.Album, SongActionType.AddToPlaylist),
                    getSongAction(songInfo, SongFieldType.Album, SongActionType.Download)
                )
            ),




            getInfoRow(
                SongFieldType.Disk,
                SongActionType.ExpandableTitle,
                subRows = listOfNotNull(
                    getSongAction(songInfo, SongFieldType.Disk, SongActionType.AddToFilter),
                    getSongAction(songInfo, SongFieldType.Disk, SongActionType.AddToPlaylist),
                    getSongAction(songInfo, SongFieldType.Disk, SongActionType.Download)
                )
            ),
            getInfoRow(
                SongFieldType.AlbumArtist,
                SongActionType.ExpandableTitle,
                subRows = listOfNotNull(
                    getSongAction(songInfo, SongFieldType.AlbumArtist, SongActionType.AddToFilter),
                    getSongAction(songInfo, SongFieldType.AlbumArtist, SongActionType.Search),
                    getSongAction(songInfo, SongFieldType.AlbumArtist, SongActionType.AddToPlaylist),
                    getSongAction(songInfo, SongFieldType.AlbumArtist, SongActionType.Download)
                )
            ),
            *List(songInfo.genreNames.size) { index ->
                getInfoRow(
                    SongFieldType.Genre,
                    SongActionType.ExpandableTitle,
                    index = index,
                    subRows =
                    listOfNotNull(
                        getSongAction(songInfo, SongFieldType.Genre, SongActionType.AddToFilter, index = index),
                        getSongAction(songInfo, SongFieldType.Genre, SongActionType.Search, index = index),
                        getSongAction(
                            songInfo,
                            SongFieldType.Genre,
                            SongActionType.AddToPlaylist,
                            index = index
                        ),
                        getSongAction(songInfo, SongFieldType.Genre, SongActionType.Download, index = index)
                    )
                )
            }.toTypedArray(),
            *List(songInfo.playlistNames.size) { index ->
                getInfoRow(
                    SongFieldType.Playlist,
                    SongActionType.ExpandableTitle,
                    index = index,
                    subRows = listOfNotNull(
                        getSongAction(songInfo, SongFieldType.Playlist, SongActionType.AddToFilter, index = index),
                        getSongAction(
                            songInfo,
                            SongFieldType.Playlist,
                            SongActionType.AddToPlaylist,
                            index = index
                        ),
                        getSongAction(songInfo, SongFieldType.Playlist, SongActionType.Download, index = index)
                    )
                )
            }.toTypedArray(),
            getInfoRow(
                SongFieldType.Duration,
                SongActionType.InfoTitle,
            ),
            getInfoRow(
                SongFieldType.Year,
                SongActionType.InfoTitle,
            )
        ).toMutableList()
    }

    private fun getSongAction(
        songInfo: SongInfo,
        field: SongFieldType,
        action: SongActionType,
        order: Int? = null,
        index: Int = 0
    ): BaseSongInfoRow? {
        return when (action) {

            SongActionType.None -> if (field == SongFieldType.Title) getInfoRow(
                SongFieldType.Title,
                SongActionType.None,
                order = order
            ) else null //todo Change to Delete from local behaviour
            // if not present -> download
            // if partially present (and not song) -> both actions (download & delete)
            // if complete -> delete from local
            SongActionType.AddToFilter -> when (field) {
                SongFieldType.Title -> getInfoRow(
                    SongFieldType.Title,
                    SongActionType.AddToFilter,
                    order = order
                )

                SongFieldType.Artist -> getInfoRow(
                    SongFieldType.Artist,
                    SongActionType.AddToFilter,
                    order = order
                )

                SongFieldType.Album -> getInfoRow(
                    SongFieldType.Album,
                    SongActionType.AddToFilter,
                    order = order
                )

                SongFieldType.Disk -> getInfoRow(
                    SongFieldType.Disk,
                    SongActionType.AddToFilter,
                    order = order
                )

                SongFieldType.AlbumArtist -> getInfoRow(
                    SongFieldType.AlbumArtist,
                    SongActionType.AddToFilter,
                    order = order
                )

                SongFieldType.Genre -> getInfoRow(
                    SongFieldType.Genre,
                    SongActionType.AddToFilter,
                    order = order,
                    index = index
                )

                SongFieldType.Playlist -> getInfoRow(
                    SongFieldType.Playlist,
                    SongActionType.AddToFilter,
                    order = order,
                    index = index
                )

                else -> null
            }

            SongActionType.AddToPlaylist -> when (field) {
                SongFieldType.Title -> getInfoRow(
                    SongFieldType.Title,
                    SongActionType.AddToPlaylist,
                    order = order
                )

                SongFieldType.Artist -> getInfoRow(
                    SongFieldType.Artist,
                    SongActionType.AddToPlaylist,
                    order = order
                )

                SongFieldType.Album -> getInfoRow(
                    SongFieldType.Album,
                    SongActionType.AddToPlaylist,
                    order = order
                )

                SongFieldType.Disk -> getInfoRow(
                    SongFieldType.Disk,
                    SongActionType.AddToPlaylist,
                    order = order
                )

                SongFieldType.AlbumArtist -> getInfoRow(
                    SongFieldType.AlbumArtist,
                    SongActionType.AddToPlaylist,
                    order = order
                )

                SongFieldType.Genre -> getInfoRow(
                    SongFieldType.Genre,
                    SongActionType.AddToPlaylist,
                    order = order
                )

                SongFieldType.Playlist -> getInfoRow(
                    SongFieldType.Playlist,
                    SongActionType.AddToPlaylist,
                    order = order
                )

                else -> null
            }

            SongActionType.AddNext -> if (field == SongFieldType.Title) getInfoRow(
                SongFieldType.Title,
                SongActionType.AddNext,
                order = order
            ) else null

            SongActionType.Search -> when (field) {
                SongFieldType.Title -> getInfoRow(
                    SongFieldType.Title,
                    SongActionType.Search,
                    order = order
                )

                SongFieldType.Artist -> getInfoRow(
                    SongFieldType.Artist,
                    SongActionType.Search,
                    order = order
                )

                SongFieldType.Album -> getInfoRow(
                    SongFieldType.Album,
                    SongActionType.Search,
                    order = order
                )

                SongFieldType.AlbumArtist -> getInfoRow(
                    SongFieldType.AlbumArtist,
                    SongActionType.Search,
                    order = order
                )

                else -> null
            }

            SongActionType.Download -> when (field) {
                SongFieldType.Title -> getInfoRow(
                    SongFieldType.Title,
                    SongActionType.Download,
                    order = order,
                    progress = getDownloadState(
                        songInfo.id,
                        Filter.FilterType.SONG_IS
                    )
                )

                SongFieldType.Album -> getInfoRow(
                    SongFieldType.Album,
                    SongActionType.Download,
                    order = order,
                    progress = getDownloadState(
                        songInfo.albumId,
                        Filter.FilterType.ALBUM_IS
                    )
                )

                SongFieldType.Disk -> getInfoRow(
                    SongFieldType.Disk,
                    SongActionType.Download,
                    order = order,
                    progress = getDownloadState(
                        songInfo.albumId,
                        Filter.FilterType.DISK_IS,
                        songInfo.disk
                    )
                )

                SongFieldType.AlbumArtist -> getInfoRow(
                    SongFieldType.AlbumArtist,
                    SongActionType.Download,
                    order = order,
                    progress = getDownloadState(
                        songInfo.albumArtistId,
                        Filter.FilterType.ALBUM_ARTIST_IS
                    )
                )

                SongFieldType.Artist -> getInfoRow(
                    SongFieldType.Artist,
                    SongActionType.Download,
                    order = order,
                    progress = getDownloadState(
                        songInfo.artistId,
                        Filter.FilterType.ARTIST_IS
                    )
                )

                SongFieldType.Genre -> getInfoRow(
                    SongFieldType.Genre,
                    SongActionType.Download,
                    order = order,
                    progress = getDownloadState(
                        songInfo.genreIds[index],
                        Filter.FilterType.GENRE_IS
                    ),
                    index = index
                )

                SongFieldType.Playlist -> getInfoRow(
                    SongFieldType.Playlist,
                    SongActionType.Download,
                    order = order,
                    progress = getDownloadState(
                        songInfo.playlistIds[index],
                        Filter.FilterType.PLAYLIST_IS
                    ),
                    index = index
                )

                else -> null
            }

            else -> {
                getInfoRow(
                    SongFieldType.Duration,
                    SongActionType.InfoTitle
                )
            }
        }
    }

    private fun getInfoRow(
        fieldType: SongFieldType,
        actionType: SongActionType,
        progress: LiveData<DownloadProgressState>? = null,
        order: Int? = null,
        index: Int? = null,
        subRows: List<BaseSongInfoRow>? = null
    ): BaseSongInfoRow = if (subRows != null && index != null) {
        BaseSongInfoRow.SongInfoMultipleContainerRow(fieldType, index, subRows)
    } else if (subRows != null && index == null) {
        BaseSongInfoRow.SongInfoContainerRow(fieldType, subRows)
    } else if (index != null && progress != null) {
        BaseSongInfoRow.SongDownloadMultipleInfoRow(fieldType, actionType, index, progress)
    } else if (index != null) {
        BaseSongInfoRow.SongActionMultipleInfoRow(fieldType, actionType, index)
    } else if (order != null) {
        BaseSongInfoRow.ShortcutInfoRow(fieldType, actionType, order)
    } else if (progress != null) {
        BaseSongInfoRow.SongDownloadInfoRow(fieldType, actionType, progress)
    } else {
        BaseSongInfoRow.SongInfoRow(fieldType, actionType)
    }

    abstract fun getDownloadState(id: Long, type: Filter.FilterType, additionalInfo: Int? = null) : LiveData<DownloadProgressState>

}