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
    protected val songInfoMediator = MediatorLiveData<SongInfo>()
    val songInfoObservable: LiveData<SongInfo> = songInfoMediator
    val coverConfig: LiveData<ImageConfig> = MutableLiveData()
    val songInfo: SongInfo
        get() {
            val value = songInfoMediator.value
            return value ?: SongInfo.dummySongInfo()
        }
    var songId: Long
        get() {
            val value = songInfoMediator.value?.id
            return value ?: SongInfo.dummySongInfo().id
        }
        set(value) {
            viewModelScope.launch {
                if (value != SongInfoActions.DUMMY_SONG_ID) {
                    songInfoMediator.addSource(dataRepository.getSong(value)) {
                        songInfoMediator.mutable.value = it

                        coverConfig.mutable.value = ImageConfig(
                            url = infoActions.getAlbumArtUrl(it.albumId),
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