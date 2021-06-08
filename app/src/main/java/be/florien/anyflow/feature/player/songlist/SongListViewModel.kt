package be.florien.anyflow.feature.player.songlist

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagingData
import be.florien.anyflow.data.view.Song
import be.florien.anyflow.feature.BaseViewModel
import be.florien.anyflow.injection.ActivityScope
import be.florien.anyflow.player.IdlePlayerController
import be.florien.anyflow.player.PlayerController
import be.florien.anyflow.player.PlayerService
import be.florien.anyflow.player.PlayingQueue
import javax.inject.Inject

/**
 * Display a list of accounts and play it upon selection.
 */

@ActivityScope
class SongListViewModel
@Inject constructor(private val playingQueue: PlayingQueue) : BaseViewModel() {

    internal var connection: PlayerConnection = PlayerConnection()
    private var player: PlayerController = IdlePlayerController()
    private val isLoadingAll: LiveData<Boolean> = MutableLiveData(false)
    val pagedAudioQueue: LiveData<PagingData<Song>> = MediatorLiveData<PagingData<Song>>().apply {

        addSource(playingQueue.songDisplayListUpdater) {
            value = it
        }
        addSource(playingQueue.queueChangeUpdater) {
            value = null
        }
    }
    val currentSong: LiveData<Song> = playingQueue.currentSong

    val listPosition: LiveData<Int> = playingQueue.positionUpdater

    /**
     * Public methods
     */

    fun refreshSongs() {
        isLoadingAll.mutable.value = true
    }

    fun play(position: Int) {
        playingQueue.listPosition = position
        player.play()
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