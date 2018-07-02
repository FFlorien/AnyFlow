package be.florien.ampacheplayer.view.player.songlist

import android.arch.paging.PagedList
import android.content.ComponentName
import android.content.ServiceConnection
import android.databinding.Bindable
import android.os.IBinder
import be.florien.ampacheplayer.BR
import be.florien.ampacheplayer.di.ActivityScope
import be.florien.ampacheplayer.persistence.local.model.SongDisplay
import be.florien.ampacheplayer.player.AudioQueue
import be.florien.ampacheplayer.player.DummyPlayerController
import be.florien.ampacheplayer.player.PlayerController
import be.florien.ampacheplayer.player.PlayerService
import be.florien.ampacheplayer.view.BaseVM
import io.reactivex.android.schedulers.AndroidSchedulers
import timber.log.Timber
import javax.inject.Inject

/**
 * Display a list of accounts and play it upon selection.
 */

@ActivityScope
class SongListFragmentVm
@Inject constructor(
        private val audioQueue: AudioQueue
) : BaseVM() {

    @get:Bindable
    var isLoadingAll: Boolean = false
        set(value) {
            notifyPropertyChanged(BR.loadingAll)
            field = value
        }

    var player: PlayerController = DummyPlayerController()
    internal var connection: PlayerConnection = PlayerConnection()

    /**
     * Constructor
     */
    init {
        Timber.tag(this.javaClass.simpleName)
        subscribe(audioQueue.positionUpdater.observeOn(AndroidSchedulers.mainThread()), onNext = {
            listPosition = it
            notifyPropertyChanged(BR.listPosition)
        }, onError = {
            Timber.e(it, "Error while updating position")
        })
        subscribe(audioQueue.songListUpdater.observeOn(AndroidSchedulers.mainThread()), onNext = {
            pagedAudioQueue = it
            notifyPropertyChanged(BR.pagedAudioQueue)
        }, onError = {
            Timber.e(it, "Error while updating songList")
        })
        subscribe(audioQueue.currentSongUpdater.observeOn(AndroidSchedulers.mainThread()), onNext = { maybeSong ->
            currentSong = maybeSong?.let { SongDisplay(it) }
            notifyPropertyChanged(BR.currentSong)
        }, onError = {
            Timber.e(it, "Error while updating currentSong")
        })
    }

    /**
     * Public methods
     */
    @Bindable
    var pagedAudioQueue: PagedList<SongDisplay>? = null

    @Bindable
    var currentSong: SongDisplay? = null

    @Bindable
    var listPosition = 0

    fun refreshSongs() {
        isLoadingAll = audioQueue.itemsCount == 0

//        subscribe(
//                persistenceManager.updateSongs().subscribeOn(AndroidSchedulers.mainThread()),
//                { _ ->
//                    isLoadingAll = false
//                    notifyPropertyChanged(BR.currentAudioQueue)
//                },
//                { throwable ->
//                    isLoadingAll = false
//                    when (throwable) {
//                        is SessionExpiredException -> {
//                            Timber.i(throwable, "The session token is expired")
//                            navigator.goToConnection()
//                        }
//                        is WrongIdentificationPairException -> {
//                            Timber.i(throwable, "Couldn't reconnect the user: wrong user/pwd")
//                            navigator.goToConnection()
//                        }
//                        is SocketTimeoutException, is NoServerException -> {
//                            Timber.e(throwable, "Couldn't connect to the webservice")
//                            displayHelper.notifyUserAboutError("Couldn't connect to the webservice")
//                        }
//                        else -> {
//                            Timber.e(throwable, "Unknown error")
//                            displayHelper.notifyUserAboutError("Couldn't connect to the webservice")
//                            navigator.goToConnection()
//                        }
//                    }
//                })

    }


    fun play(position: Int) {
        audioQueue.listPosition = position
    }

    /**
     * Inner class
     */
    inner class PlayerConnection : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            player = DummyPlayerController()
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            player = (service as PlayerService.LocalBinder).service
        }
    }
}