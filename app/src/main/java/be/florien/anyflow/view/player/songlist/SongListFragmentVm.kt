package be.florien.anyflow.view.player.songlist

import android.arch.paging.PagedList
import android.content.ComponentName
import android.content.ServiceConnection
import android.databinding.Bindable
import android.os.IBinder
import be.florien.anyflow.BR
import be.florien.anyflow.di.ActivityScope
import be.florien.anyflow.persistence.local.model.SongDisplay
import be.florien.anyflow.player.PlayingQueue
import be.florien.anyflow.player.IdlePlayerController
import be.florien.anyflow.player.PlayerController
import be.florien.anyflow.player.PlayerService
import be.florien.anyflow.view.BaseVM
import io.reactivex.android.schedulers.AndroidSchedulers
import timber.log.Timber
import javax.inject.Inject

/**
 * Display a list of accounts and play it upon selection.
 */

@ActivityScope
class SongListFragmentVm
@Inject constructor(
        private val playingQueue: PlayingQueue
) : BaseVM() {

    @get:Bindable
    var isLoadingAll: Boolean = false
        set(value) {
            notifyPropertyChanged(BR.loadingAll)
            field = value
        }

    var player: PlayerController = IdlePlayerController()
    internal var connection: PlayerConnection = PlayerConnection()

    /**
     * Constructor
     */
    init {
        Timber.tag(this.javaClass.simpleName)
        subscribe(playingQueue.positionUpdater.observeOn(AndroidSchedulers.mainThread()),
                onNext = {
                    listPosition = it
                    notifyPropertyChanged(BR.listPosition)
                },
                onError = {
                    Timber.e(it, "Error while updating position")
                })
        subscribe(playingQueue.songListUpdater.observeOn(AndroidSchedulers.mainThread()),
                onNext = {
                    pagedAudioQueue = it
                    notifyPropertyChanged(BR.pagedAudioQueue)
                },
                onError = {
                    Timber.e(it, "Error while updating songList")
                })
        subscribe(playingQueue.currentSongUpdater.observeOn(AndroidSchedulers.mainThread()),
                onNext = { maybeSong ->
                    currentSong = maybeSong?.let { SongDisplay(it) }
                    notifyPropertyChanged(BR.currentSong)
                },
                onError = {
                    Timber.e(it, "Error while updating currentSong")
                })
        subscribe(playingQueue.queueChangeUpdater,
                onNext = {
                    upToDate = false
                })
    }

    /**
     * Public methods
     */
    @Bindable
    var pagedAudioQueue: PagedList<SongDisplay>? = null
        set(value) {
            field = value
            upToDate = true
        }

    @Bindable
    var currentSong: SongDisplay? = null

    @Bindable
    var listPosition = 0

    @Bindable
    var upToDate = true
        set(value) {
            field = value
            notifyPropertyChanged(BR.upToDate)
        }

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