package be.florien.anyflow.feature.song.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import be.florien.anyflow.common.ui.data.ImageConfig
import be.florien.anyflow.common.ui.data.info.InfoActions
import be.florien.anyflow.common.ui.info.InfoViewModel
import be.florien.anyflow.tags.model.SongInfo

abstract class BaseSongViewModel<IA: InfoActions<SongInfo>>: InfoViewModel<SongInfo, IA>() {
    protected val songInfoMediator = MediatorLiveData<SongInfo>()
    val songInfoObservable: LiveData<SongInfo> = songInfoMediator
    val coverConfig: LiveData<ImageConfig> = MutableLiveData()
    val songInfo: SongInfo
        get() {
            val value = songInfoMediator.value
            return value ?: SongInfo.dummySongInfo()
        }
    abstract var songId: Long

    override fun executeAction(row: InfoActions.InfoRow) = when (row.actionType) {
        BaseSongInfoActions.SongActionType.ExpandableTitle -> {
            toggleExpansion(row)
            true
        }

        BaseSongInfoActions.SongActionType.None,
        BaseSongInfoActions.SongActionType.InfoTitle -> true

        else -> false
    }
}