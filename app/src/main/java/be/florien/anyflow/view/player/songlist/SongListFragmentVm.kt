package be.florien.anyflow.view.player.songlist

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import androidx.databinding.Bindable
import androidx.paging.PagedList
import be.florien.anyflow.BR
import be.florien.anyflow.di.ActivityScope
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.persistence.local.model.SongDisplay
import be.florien.anyflow.player.IdlePlayerController
import be.florien.anyflow.player.PlayerController
import be.florien.anyflow.player.PlayerService
import be.florien.anyflow.player.PlayingQueue
import be.florien.anyflow.view.BaseVM
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

/**
 * Display a list of accounts and play it upon selection.
 */

@ActivityScope
class SongListFragmentVm
@Inject constructor(
        private val playingQueue: PlayingQueue
) : BaseVM() {

    internal var connection: PlayerConnection = PlayerConnection()
    private var player: PlayerController = IdlePlayerController()
    private val pagedListListener = object : PagedList.Callback() {
        override fun onChanged(position: Int, count: Int) {
            if (listPosition in position..position + count) {
                notifyPropertyChanged(BR.listPositionLoaded)
            }
        }

        override fun onInserted(position: Int, count: Int) {}

        override fun onRemoved(position: Int, count: Int) {}
    }

    @get:Bindable
    var isLoadingAll: Boolean = false
        set(value) {
            notifyPropertyChanged(BR.loadingAll)
            field = value
        }
    @Bindable
    var pagedAudioQueue: PagedList<SongDisplay>? = null
        set(value) {
            field?.removeWeakCallback(pagedListListener)
            val previousSnapshot = field?.snapshot()
            field = value
            field?.addWeakCallback(previousSnapshot, pagedListListener)
        }
    @Bindable
    var currentSong: SongDisplay? = null

    @Bindable
    var listPosition = 0

    @get:Bindable
    val listPositionLoaded
        get() = (pagedAudioQueue?.size
                ?: 0) > listPosition && listPosition >= 0 && pagedAudioQueue?.get(listPosition) != null

    /**
     * Constructor
     */
    init {
        subscribe(playingQueue.positionUpdater.observeOn(AndroidSchedulers.mainThread()),
                onNext = {
                    listPosition = it
                    notifyPropertyChanged(BR.listPosition)
                },
                onError = {
                    this@SongListFragmentVm.eLog(it, "Error while updating argument")
                })
        subscribe(playingQueue.songDisplayListUpdater.observeOn(AndroidSchedulers.mainThread()),
                onNext = {
                    pagedAudioQueue = it
                    notifyPropertyChanged(BR.pagedAudioQueue)
                },
                onError = {
                    this@SongListFragmentVm.eLog(it, "Error while updating songList")
                })
        subscribe(playingQueue.currentSongUpdater.observeOn(AndroidSchedulers.mainThread()),
                onNext = { maybeSong ->
                    currentSong = maybeSong?.let { SongDisplay(it) }
                    notifyPropertyChanged(BR.currentSong)
                },
                onError = {
                    this@SongListFragmentVm.eLog(it, "Error while updating currentSong")
                })
        subscribe(playingQueue.queueChangeUpdater,
                onNext = {
                    pagedAudioQueue = null
                    notifyPropertyChanged(BR.pagedAudioQueue)
                })
    }

    /**
     * Public methods
     */

    fun refreshSongs() {
        isLoadingAll = playingQueue.itemsCount == 0
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