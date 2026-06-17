package be.florien.anyflow.feature.song.domain

import android.content.SharedPreferences
import be.florien.anyflow.feature.song.base.domain.BaseSongInfoActions
import be.florien.anyflow.feature.song.base.domain.model.BaseSongInfoRow
import be.florien.anyflow.feature.song.base.domain.model.SongFieldType
import be.florien.anyflow.management.download.DownloadManager
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.management.filters.domain.model.FilterParam
import be.florien.anyflow.management.filters.domain.model.TagFilterType
import be.florien.anyflow.management.queue.OrderComposer
import be.florien.anyflow.tags.model.SongInfo
import javax.inject.Inject
import javax.inject.Named

class SongInfoActions @Inject constructor(
    private val filtersManager: FiltersManager,
    private val orderComposer: OrderComposer,
    private val downloadManager: DownloadManager,
    @Named("preferences") sharedPreferences: SharedPreferences
) : BaseSongInfoActions(sharedPreferences) {

    /**
     * Action methods
     */

    suspend fun playNext(songId: Long) {
        orderComposer.changeSongPositionForNext(songId)
    }

    suspend fun filterOn(songInfo: SongInfo, row: BaseSongInfoRow) {
        val filter = when (row.fieldType) {
            SongFieldType.Title -> Filter(FilterParam(
                TagFilterType.SONG_IS,
                songInfo.id,
                songInfo.title
            ))

            SongFieldType.Artist -> Filter(FilterParam(
                TagFilterType.ARTIST_IS,
                songInfo.artistId,
                songInfo.artistName
            ))

            SongFieldType.Album -> Filter(FilterParam(
                TagFilterType.ALBUM_IS,
                songInfo.albumId,
                songInfo.albumName
            ))

            SongFieldType.Disk -> Filter(FilterParam(
                TagFilterType.ALBUM_IS,
                songInfo.albumId,
                songInfo.albumName),
                    FilterParam(
                        TagFilterType.DISK_IS,
                        songInfo.disk,
                        songInfo.disk.toString()
                    )

            )

            SongFieldType.AlbumArtist -> Filter(FilterParam(
                TagFilterType.ALBUM_ARTIST_IS,
                songInfo.albumArtistId,
                songInfo.albumArtistName
            ))

            SongFieldType.Genre -> {
                val index = (row as BaseSongInfoRow.SongMultipleInfoRow).index
                Filter(FilterParam(
                    TagFilterType.GENRE_IS,
                    songInfo.genreIds[index],
                    songInfo.genreNames[index]
                ))
            }

            SongFieldType.Playlist -> {
                val index = (row as BaseSongInfoRow.SongMultipleInfoRow).index
                Filter(FilterParam(
                    TagFilterType.PLAYLIST_IS,
                    songInfo.playlistIds[index],
                    songInfo.playlistNames[index]
                ))
            }

            else -> throw IllegalArgumentException("This field can't be filtered on")
        }
        filtersManager.clearFilters()
        filtersManager.addFilter(filter)
        filtersManager.commitChanges()
    }

    fun getSearchTerms(songInfo: SongInfo, fieldType: SongFieldType): String {
        return when (fieldType) {
            SongFieldType.Title -> songInfo.title
            SongFieldType.Artist -> songInfo.artistName
            SongFieldType.Album -> songInfo.albumName
            SongFieldType.AlbumArtist -> songInfo.albumArtistName
            SongFieldType.Genre -> songInfo.genreNames.first()
            else -> throw IllegalArgumentException("This field can't be searched on")
        }
    }

    fun queueDownload(songInfo: SongInfo, fieldType: SongFieldType, index: Int?) {
        val data = when (fieldType) {
            SongFieldType.Title -> Triple(
                songInfo.id,
                TagFilterType.SONG_IS,
                -1
            )

            SongFieldType.Artist -> Triple(
                songInfo.artistId,
                TagFilterType.ARTIST_IS,
                -1
            )

            SongFieldType.Album -> Triple(
                songInfo.albumId,
                TagFilterType.ALBUM_IS,
                -1
            )

            SongFieldType.Disk -> Triple(
                songInfo.albumId,
                TagFilterType.DISK_IS,
                songInfo.disk
            )

            SongFieldType.AlbumArtist -> Triple(
                songInfo.albumArtistId,
                TagFilterType.ALBUM_ARTIST_IS,
                -1
            )

            SongFieldType.Genre -> {
                val trueIndex = index ?: return
                Triple(
                    songInfo.genreIds[trueIndex],
                    TagFilterType.GENRE_IS,
                    -1
                )
            }

            SongFieldType.Playlist -> {
                val trueIndex = index ?: return
                Triple(
                    songInfo.playlistIds[trueIndex],
                    TagFilterType.PLAYLIST_IS,
                    -1
                )
            }

            else -> return
        }
        downloadManager.queueDownload(data.first, data.second, data.third)
    }
}