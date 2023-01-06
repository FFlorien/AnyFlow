package be.florien.anyflow.feature.player.info.song

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.R
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.extension.ImageConfig
import be.florien.anyflow.feature.player.info.InfoViewModel
import be.florien.anyflow.player.FiltersManager
import be.florien.anyflow.player.OrderComposer
import kotlinx.coroutines.launch

abstract class BaseSongViewModel(
    context: Context,
    filtersManager: FiltersManager,
    orderComposer: OrderComposer,
    dataRepository: DataRepository,
    sharedPreferences: SharedPreferences
) : InfoViewModel<SongInfo>() {
    val songInfo: LiveData<SongInfo> = MutableLiveData()
    val coverConfig: LiveData<ImageConfig> = MutableLiveData()
    var song: SongInfo = SongInfo.dummySongInfo()
        set(value) {
            field = value
            viewModelScope.launch {
                songInfo.mutable.value = value
                coverConfig.mutable.value = ImageConfig(
                    url = infoActions.getAlbumArtUrl(song.albumId),
                    resource = R.drawable.cover_placeholder
                )
                updateRows()
            }
        }
    override val infoActions = SongInfoActions(
        context.contentResolver,
        filtersManager,
        orderComposer,
        dataRepository,
        sharedPreferences
    )

}