package be.florien.ampacheplayer.view.player.songlist

import android.content.ComponentName
import android.content.ServiceConnection
import android.databinding.Bindable
import android.os.IBinder
import be.florien.ampacheplayer.BR
import be.florien.ampacheplayer.exception.SessionExpiredException
import be.florien.ampacheplayer.exception.WrongIdentificationPairException
import be.florien.ampacheplayer.persistence.PersistenceManager
import be.florien.ampacheplayer.player.AudioQueueManager
import be.florien.ampacheplayer.player.PlayerService
import be.florien.ampacheplayer.view.BaseVM
import be.florien.ampacheplayer.view.DisplayHelper
import be.florien.ampacheplayer.view.Navigator
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.net.SocketTimeoutException
import javax.inject.Inject

/**
 * Display a list of songs and play it upon selection.
 */

class SongListFragmentVm
@Inject constructor(
        private val persistenceManager: PersistenceManager,
        private val audioQueueManager: AudioQueueManager,
        private val navigator: Navigator,
        private val displayHelper: DisplayHelper
) : BaseVM() {

    @get:Bindable
    var isLoadingAll: Boolean = false
        set(value) {
            notifyPropertyChanged(BR.loadingAll)
            field = value
        }

    var player: PlayerService? = null

    internal var connection: PlayerConnection = PlayerConnection()

    /**
     * Constructor
     */
    init {
        Timber.tag(this.javaClass.simpleName)
        subscribe(audioQueueManager.positionObservable.observeOn(AndroidSchedulers.mainThread()), onNext = {
            notifyPropertyChanged(BR.listPosition)
        })
    }

    /**
     * Public methods
     */
    @Bindable
    fun getCurrentAudioQueue() = audioQueueManager.getCurrentAudioQueue()

    @Bindable
    fun getListPosition() = audioQueueManager.listPosition

    fun refreshSongs() {
        isLoadingAll = audioQueueManager.itemsCount == 0
        subscribe(
                persistenceManager.getSongs().toObservable().subscribeOn(Schedulers.io()),
                {
                    isLoadingAll = false
                    notifyPropertyChanged(BR.currentAudioQueue)
                },
                {
                    isLoadingAll = false
                    when (it) {
                        is SessionExpiredException -> {
                            Timber.i(it, "The session token is expired")
                            navigator.goToConnection()
                        }
                        is WrongIdentificationPairException -> {
                            Timber.i(it, "Couldn't reconnect the user: wrong user/pwd")
                            navigator.goToConnection()
                        }
                        is SocketTimeoutException -> {
                            Timber.e(it, "Couldn't connect to the webservice")
                            displayHelper.notifyUserAboutError("Couldn't connect to the webservice")
                        }
                        else -> {
                            Timber.e(it, "Unknown error")
                            displayHelper.notifyUserAboutError("Couldn't connect to the webservice")
                            navigator.goToConnection()
                        }
                    }
                })

    }


    fun play(position: Int) {
        audioQueueManager.listPosition = position
        player?.play()
    }

    fun playPause() {
        player?.let {
            if (it.isPlaying()) it.pause() else it.resume()
        }
    }

    /**
     * Inner class
     */
    inner class PlayerConnection : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            player = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            player = (service as PlayerService.LocalBinder).service
        }
    }
}