package be.florien.anyflow.feature.player.songlist

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagedList
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
    private val pagedListListener = object : PagedList.Callback() {
        override fun onChanged(position: Int, count: Int) {
            listPositionLoaded.mutable.value = listPosition.value in position..position + count
        }

        override fun onInserted(position: Int, count: Int) {}

        override fun onRemoved(position: Int, count: Int) {}
    }

    private val isLoadingAll: LiveData<Boolean> = MutableLiveData(false)
    val pagedAudioQueue: LiveData<PagedList<Song>> = MediatorLiveData<PagedList<Song>>().apply {

        addSource(playingQueue.songDisplayListUpdater) {
            value?.removeWeakCallback(pagedListListener)
            value = it
            it.addWeakCallback(null, pagedListListener)
            isFollowingCurrentSong.value = true
        }
        addSource(playingQueue.queueChangeUpdater) {
            value = null
        }
    }
    val currentSong: LiveData<Song> = playingQueue.currentSong

    val listPosition: LiveData<Int> = MediatorLiveData<Int>().apply {
        addSource(playingQueue.positionUpdater) {
            value = it
            prepareScrollToCurrent()
        }
    }
    val listPositionLoaded: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(pagedAudioQueue) {
            value = isListPositionLoaded()
        }
        addSource(listPosition) {
            value = isListPositionLoaded()
        }
    }
    val isFollowingCurrentSong = MutableLiveData(true)

    private fun isListPositionLoaded(): Boolean {
        val position = listPosition.value ?: 0
        return ((pagedAudioQueue.value?.size ?: 0) > position
                && position >= 0
                && pagedAudioQueue.value?.get(position) != null)
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
        val position = listPosition.value ?: 0
        if (position in 0 until (pagedAudioQueue.value?.size ?: 0)) {
            if (pagedAudioQueue.value?.getOrNull(position) == null) {
                pagedAudioQueue.value?.loadAround(position)
            } else {
                listPositionLoaded.mutable.value = true
            }
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