package be.florien.ampacheplayer.view.viewmodel

import android.content.ComponentName
import android.content.ServiceConnection
import android.databinding.Bindable
import android.os.IBinder
import be.florien.ampacheplayer.BR
import be.florien.ampacheplayer.exception.SessionExpiredException
import be.florien.ampacheplayer.exception.WrongIdentificationPairException
import be.florien.ampacheplayer.manager.AudioQueueManager
import be.florien.ampacheplayer.manager.DisplayHelper
import be.florien.ampacheplayer.manager.Navigator
import be.florien.ampacheplayer.manager.PersistenceManager
import be.florien.ampacheplayer.player.PlayerService
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.net.SocketTimeoutException
import javax.inject.Inject

/**
 * Display a list of songs and play it upon selection.
 */

class SongListFragmentVM : BaseVM() {

    @field:Inject lateinit var persistenceManager: PersistenceManager
    @field:Inject lateinit var audioQueueManager: AudioQueueManager
    @Inject lateinit var navigator: Navigator
    @Inject lateinit var displayHelper: DisplayHelper
    var player: PlayerService? = null

    internal var connection: PlayerConnection = PlayerConnection()

    /**
     * Constructor
     */
    init {
        Timber.tag(this.javaClass.simpleName)
    }

    /**
     * Private methods
     */

    fun onViewCreated() {
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
        subscribe(
                persistenceManager.refreshSongs().subscribeOn(Schedulers.io()),
                {
                    notifyPropertyChanged(BR.currentAudioQueue)
                },
                {
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