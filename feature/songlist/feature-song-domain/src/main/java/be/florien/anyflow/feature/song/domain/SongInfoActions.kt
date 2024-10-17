package be.florien.anyflow.feature.song.domain

import android.content.SharedPreferences
import be.florien.anyflow.feature.song.base.ui.BaseSongInfoActions
import be.florien.anyflow.management.download.DownloadManager
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.model.Filter
import be.florien.anyflow.management.queue.OrderComposer
import be.florien.anyflow.tags.UrlRepository
import be.florien.anyflow.tags.model.SongInfo
import javax.inject.Inject
import javax.inject.Named

class SongInfoActions @Inject constructor(
    private val filtersManager: FiltersManager,
    private val orderComposer: OrderComposer,
    private val urlRepository: UrlRepository,
    private val downloadManager: DownloadManager,
    @Named("preferences") sharedPreferences: SharedPreferences
) : BaseSongInfoActions(sharedPreferences) {

    /**
     * Overridden methods
     */

    override fun getDownloadState(
        id: Long,
        type: Filter.FilterType,
        additionalInfo: Int?
    ) = downloadManager.getDownloadState(id, type, additionalInfo)

    /**
     * Public methods
     */

    fun getAlbumArtUrl(albumId: Long) = urlRepository.getAlbumArtUrl(albumId)
    fun getPodcastArtUrl(podcastId: Long) = urlRepository.getPodcastArtUrl(podcastId)

    /**
     * Action methods
     */

    suspend fun playNext(songId: Long) {
        orderComposer.changeSongPositionForNext(songId)
    }

    suspend fun filterOn(songInfo: SongInfo, row: InfoRow) {
        val filter = when (row.fieldType) {
            SongFieldType.Title -> Filter(
                Filter.FilterType.SONG_IS,
                songInfo.id,
                songInfo.title
            )

            SongFieldType.Artist -> Filter(
                Filter.FilterType.ARTIST_IS,
                songInfo.artistId,
                songInfo.artistName
            )

            SongFieldType.Album -> Filter(
                Filter.FilterType.ALBUM_IS,
                songInfo.albumId,
                songInfo.albumName
            )

            SongFieldType.Disk -> Filter(
                Filter.FilterType.ALBUM_IS,
                songInfo.albumId,
                songInfo.albumName,
                listOf(
                    Filter(
                        Filter.FilterType.DISK_IS,
                        songInfo.disk,
                        songInfo.disk.toString()
                    )
                )
            )

            SongFieldType.AlbumArtist -> Filter(
                Filter.FilterType.ALBUM_ARTIST_IS,
                songInfo.albumArtistId,
                songInfo.albumArtistName
            )

            SongFieldType.Genre -> {
                val index = (row as SongMultipleInfoRow).index
                Filter(
                    Filter.FilterType.GENRE_IS,
                    songInfo.genreIds[index],
                    songInfo.genreNames[index]
                )
            }

            SongFieldType.Playlist -> {
                val index = (row as SongMultipleInfoRow).index
                Filter(
                    Filter.FilterType.PLAYLIST_IS,
                    songInfo.playlistIds[index],
                    songInfo.playlistNames[index]
                )
            }

            else -> throw IllegalArgumentException("This field can't be filtered on")
        }
        filtersManager.clearFilters()
        filtersManager.addFilter(filter)
        filtersManager.commitChanges()
    }

    fun getSearchTerms(songInfo: SongInfo, fieldType: FieldType): String {
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
                Filter.FilterType.SONG_IS,
                -1
            )

            SongFieldType.Artist -> Triple(
                songInfo.artistId,
                Filter.FilterType.ARTIST_IS,
                -1
            )

            SongFieldType.Album -> Triple(
                songInfo.albumId,
                Filter.FilterType.ALBUM_IS,
                -1
            )

            SongFieldType.Disk -> Triple(
                songInfo.albumId,
                Filter.FilterType.DISK_IS,
                songInfo.disk
            )

            SongFieldType.AlbumArtist -> Triple(
                songInfo.albumArtistId,
                Filter.FilterType.ALBUM_ARTIST_IS,
                -1
            )

            SongFieldType.Genre -> {
                val trueIndex = index ?: return
                Triple(
                    songInfo.genreIds[trueIndex],
                    Filter.FilterType.GENRE_IS,
                    -1
                )
            }

            SongFieldType.Playlist -> {
                val trueIndex = index ?: return
                Triple(
                    songInfo.playlistIds[trueIndex],
                    Filter.FilterType.PLAYLIST_IS,
                    -1
                )
            }

            else -> return
        }
        downloadManager.queueDownload(data.first, data.second, data.third)
    }
}