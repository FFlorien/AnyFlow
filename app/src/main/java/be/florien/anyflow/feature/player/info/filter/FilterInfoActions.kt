package be.florien.anyflow.feature.player.info.filter

import android.content.Context
import android.content.res.Resources
import be.florien.anyflow.R
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.feature.player.info.InfoActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FilterInfoActions @Inject constructor(
    private val dataRepository: DataRepository,
    context: Context
) : InfoActions<Filter<*>?>() {

    private val resources: Resources = context.resources

    override suspend fun getInfoRows(infoSource: Filter<*>?): List<InfoRow> {
        fun Int?.getDurationString(resource: Int) =
            this?.let { resources.getQuantityString(resource, it, it) } ?: ""

        val filteredInfo =
            withContext(Dispatchers.IO) { dataRepository.getFilteredInfo(infoSource) }
        return listOfNotNull(
            InfoRow(
                R.string.filter_info_duration,
                filteredInfo.duration.toComponents { days, hours, minutes, seconds, _ ->
                    val d = days.toInt().takeIf { it > 0 }
                    val h = hours.takeIf { it > 0 || days > 0 }
                    val m = minutes.takeIf { it > 0 || days > 0 || hours > 0 }
                    val s = seconds.takeIf { it > 0 || days > 0 || hours > 0 || minutes > 0 }
                    d.getDurationString(R.plurals.days_component) +
                            h.getDurationString(R.plurals.hours_component) +
                            m.getDurationString(R.plurals.minutes_component) +
                            s.getDurationString(R.plurals.seconds_component)
                },
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