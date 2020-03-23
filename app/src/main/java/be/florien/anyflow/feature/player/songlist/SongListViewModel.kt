package be.florien.anyflow.feature.player.songlist

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagedList
import be.florien.anyflow.data.local.model.SongDisplay
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.feature.BaseViewModel
import be.florien.anyflow.feature.MutableValueLiveData
import be.florien.anyflow.feature.ValueLiveData
import be.florien.anyflow.injection.ActivityScope
import be.florien.anyflow.player.IdlePlayerController
import be.florien.anyflow.player.PlayerController
import be.florien.anyflow.player.PlayerService
import be.florien.anyflow.player.PlayingQueue
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

/**
 * Display a list of accounts and play it upon selection.
 */

@ActivityScope
class SongListViewModel
@Inject constructor(
        private val playingQueue: PlayingQueue
) : BaseViewModel() {

    internal var connection: PlayerConnection = PlayerConnection()
    private var player: PlayerController = IdlePlayerController()
    private val pagedListListener = object : PagedList.Callback() {
        override fun onChanged(position: Int, count: Int) {
            listPositionLoaded.mutable.value = listPosition.value in position..position + count
        }

        override fun onInserted(position: Int, count: Int) {}

        override fun onRemoved(position: Int, count: Int) {}
    }

    private val isLoadingAll: ValueLiveData<Boolean> = MutableValueLiveData(false)
    val pagedAudioQueue: LiveData<PagedList<SongDisplay>> = MutableLiveData()
    val currentSong: LiveData<SongDisplay> = MutableLiveData()

    val listPosition: ValueLiveData<Int> = MutableValueLiveData(0)
    val listPositionLoaded: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(pagedAudioQueue) {
            value = isListPositionLoaded()
        }
        addSource(listPosition) {
            value = isListPositionLoaded()
        }
    }
    val isFollowingCurrentSong = MutableValueLiveData(true)

    private fun isListPositionLoaded() =
            ((pagedAudioQueue.value?.size ?: 0) > listPosition.value
                    && listPosition.value >= 0
                    && pagedAudioQueue.value?.get(listPosition.value) != null)

    /**
     * Constructor
     */
    init {
        subscribe(playingQueue.positionUpdater.observeOn(AndroidSchedulers.mainThread()),
                onNext = {
                    listPosition.mutable.value = it
                    prepareScrollToCurrent()
                },
                onError = {
                    this@SongListViewModel.eLog(it, "Error while updating argument")
                })
        subscribe(playingQueue.songDisplayListUpdater.observeOn(AndroidSchedulers.mainThread()),
                onNext = {
                    pagedAudioQueue.value?.removeWeakCallback(pagedListListener)
                    pagedAudioQueue.mutable.value = it
                    it.addWeakCallback(null, pagedListListener)
                    isFollowingCurrentSong.value = true
                },
                onError = {
                    this@SongListViewModel.eLog(it, "Error while updating songList")
                })
        subscribe(playingQueue.currentSongUpdater.observeOn(AndroidSchedulers.mainThread()),
                onNext = { maybeSong ->
                    currentSong.mutable.value = maybeSong?.let { SongDisplay(it) }
                },
                onError = {
                    this@SongListViewModel.eLog(it, "Error while updating currentSong")
                })
        subscribe(playingQueue.queueChangeUpdater,
                onNext = {
                    pagedAudioQueue.mutable.value = null
                })
    }

    /**
     * Public methods
     */

    fun refreshSongs() {
        isLoadingAll.mutable.value = playingQueue.itemsCount == 0
    }

    fun play(position: Int) {
        playingQueue.listPosition = position
        player.play()
    }

    fun prepareScrollToCurrent() {
        if (pagedAudioQueue.value?.get(listPosition.value) == null) {
            pagedAudioQueue.value?.loadAround(listPosition.value)
        } else {
            listPositionLoaded.mutable.value = true
        }
    }

    /**
     * Inner class
     */
    inner class PlayerConnection : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            player = IdlePlayerController()
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            player = (service as PlayerService.LocalBinder).service
        }
    }
}