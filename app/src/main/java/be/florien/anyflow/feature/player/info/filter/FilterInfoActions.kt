package be.florien.anyflow.feature.player.info.filter

import be.florien.anyflow.R
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.feature.player.info.InfoActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FilterInfoActions @Inject constructor(private val dataRepository: DataRepository) :
    InfoActions<Filter<*>?>() {
    override suspend fun getInfoRows(infoSource: Filter<*>?): List<InfoRow> {
        val filteredInfo =
            withContext(Dispatchers.IO) { dataRepository.getFilteredInfo(infoSource) }
        return listOfNotNull(
            InfoRow(
                R.string.filter_info_duration,
                filteredInfo.duration.toIsoString(),
                null,
                FilterFieldType.Duration(),
                ActionType.InfoTitle()
            ),
            filteredInfo.genres?.let {
                InfoRow(
                    R.string.filter_info_genre,
                    it.toString(),
                    null,
                    FilterFieldType.Genre(),
                    FilterActionType.SubFilter()
                )
            },
            filteredInfo.albumArtists?.let {
                InfoRow(
                    R.string.filter_info_album_artist,
                    it.toString(),
                    null,
                    FilterFieldType.AlbumArtist(),
                    FilterActionType.SubFilter()
                )
            },
            filteredInfo.albums?.let {
                InfoRow(
                    R.string.filter_info_album,
                    it.toString(),
                    null,
                    FilterFieldType.Album(),
                    FilterActionType.SubFilter()
                )
            },
            filteredInfo.artists?.let {
                InfoRow(
                    R.string.filter_info_artist,
                    it.toString(),
                    null,
                    FilterFieldType.Artist(),
                    FilterActionType.SubFilter()
                )
            },
            filteredInfo.songs?.let {
                InfoRow(
                    R.string.filter_info_song,
                    it.toString(),
                    null,
                    FilterFieldType.Song(),
                    FilterActionType.SubFilter()
                )
            },
            filteredInfo.playlists?.let {
                InfoRow(
                    R.string.filter_info_playlist,
                    it.toString(),
                    null,
                    FilterFieldType.Playlist(),
                    FilterActionType.SubFilter()
                )
            }
        )
    }

    override suspend fun getActionsRows(
        infoSource: Filter<*>?,
        fieldType: FieldType
    ): List<InfoRow> = emptyList()
}