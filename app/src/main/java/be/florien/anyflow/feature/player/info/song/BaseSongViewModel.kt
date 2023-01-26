package be.florien.anyflow.feature.player.info.song

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.R
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.DownloadManager
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.extension.ImageConfig
import be.florien.anyflow.feature.player.info.InfoViewModel
import be.florien.anyflow.player.FiltersManager
import be.florien.anyflow.player.OrderComposer
import kotlinx.coroutines.launch

abstract class BaseSongViewModel(
    filtersManager: FiltersManager,
    orderComposer: OrderComposer,
    val dataRepository: DataRepository,
    sharedPreferences: SharedPreferences,
    downloadManager: DownloadManager
) : InfoViewModel<SongInfo>() {
    val songInfo: LiveData<SongInfo> = MediatorLiveData()
    val coverConfig: LiveData<ImageConfig> = MutableLiveData()
    protected var song: SongInfo = SongInfo.dummySongInfo()
    var songId: Long = SongInfoActions.DUMMY_SONG_ID
        set(value) {
            field = value
            viewModelScope.launch {
                if (value == SongInfoActions.DUMMY_SONG_ID) {
                    song = SongInfo.dummySongInfo()
                    songInfo.mutable.value = song
                } else {
                    (songInfo as MediatorLiveData).addSource(dataRepository.getSong(value)) {
                        song = it
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
        dataRepository,
        sharedPreferences,
        downloadManager
    )
}