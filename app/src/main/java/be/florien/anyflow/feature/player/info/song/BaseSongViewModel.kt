package be.florien.anyflow.feature.player.info.song

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.R
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.DownloadManager
import be.florien.anyflow.data.UrlRepository
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.extension.ImageConfig
import be.florien.anyflow.feature.player.info.InfoActions
import be.florien.anyflow.feature.player.info.InfoViewModel
import be.florien.anyflow.player.FiltersManager
import be.florien.anyflow.player.OrderComposer
import kotlinx.coroutines.launch

abstract class BaseSongViewModel(
    filtersManager: FiltersManager,
    orderComposer: OrderComposer,
    val dataRepository: DataRepository,
    val urlRepository: UrlRepository,
    sharedPreferences: SharedPreferences,
    downloadManager: DownloadManager
) : InfoViewModel<SongInfo>() {
    val songInfo: LiveData<SongInfo> = MediatorLiveData()
    val coverConfig: LiveData<ImageConfig> = MutableLiveData()
    var song: SongInfo
        get() {
            val value = songInfo.value
            return value ?: SongInfo.dummySongInfo()
        }
        set(value) {
            viewModelScope.launch {
                if (value.id == SongInfoActions.DUMMY_SONG_ID) {
                    songInfo.mutable.value = value
                } else {
                    (songInfo as MediatorLiveData).addSource(dataRepository.getSong(value.id)) {
                        songInfo.mutable.value = it

                        coverConfig.mutable.value = ImageConfig(
                            url = infoActions.getAlbumArtUrl(song.albumId),
                            resource = R.drawable.cover_placeholder
                        )
                        updateRows()
                    }
                }
            }
        }
    override val infoActions = SongInfoActions(
        filtersManager,
        orderComposer,
        urlRepository,
        sharedPreferences,
        downloadManager
    )

    override fun executeAction(row: InfoActions.InfoRow) = when (row.actionType) {
        SongInfoActions.SongActionType.ExpandableTitle -> {
            toggleExpansion(row)
            true
        }
        SongInfoActions.SongActionType.None,
        SongInfoActions.SongActionType.InfoTitle -> true
        else -> false
    }
}