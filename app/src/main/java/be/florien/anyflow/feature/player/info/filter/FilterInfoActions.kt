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
                    ActionType.ExpandableTitle()
                )
            },
            filteredInfo.albumArtists?.let {
                InfoRow(
                    R.string.filter_info_album_artist,
                    it.toString(),
                    null,
                    FilterFieldType.AlbumArtist(),
                    ActionType.ExpandableTitle()
                )
            },
            filteredInfo.albums?.let {
                InfoRow(
                    R.string.filter_info_album,
                    it.toString(),
                    null,
                    FilterFieldType.Album(),
                    ActionType.ExpandableTitle()
                )
            },
            filteredInfo.artists?.let {
                InfoRow(
                    R.string.filter_info_artist,
                    it.toString(),
                    null,
                    FilterFieldType.Artist(),
                    ActionType.ExpandableTitle()
                )
            },
            filteredInfo.songs?.let {
                InfoRow(
                    R.string.filter_info_song,
                    it.toString(),
                    null,
                    FilterFieldType.Song(),
                    ActionType.ExpandableTitle()
                )
            },
            filteredInfo.playlists?.let {
                InfoRow(
                    R.string.filter_info_playlist,
                    it.toString(),
                    null,
                    FilterFieldType.Playlist(),
                    ActionType.ExpandableTitle()
                )
            }
        )
    }

    override suspend fun getActionsRows(
        infoSource: Filter<*>?,
        fieldType: FieldType
    ): List<InfoRow> = listOf(
        InfoRow(
            R.string.filter_info_filter_on,
            null,
            null,
            fieldType,
            FilterActionType.SubFilter()
        )
    )
}