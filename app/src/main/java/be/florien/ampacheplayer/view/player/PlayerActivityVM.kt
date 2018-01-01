package be.florien.ampacheplayer.view.player

import android.content.ComponentName
import android.content.ServiceConnection
import android.databinding.Bindable
import android.os.IBinder
import be.florien.ampacheplayer.BR
import be.florien.ampacheplayer.player.AudioQueueManager
import be.florien.ampacheplayer.player.NO_CURRENT_SONG
import be.florien.ampacheplayer.player.DummyPlayerController
import be.florien.ampacheplayer.player.PlayerController
import be.florien.ampacheplayer.player.PlayerService
import be.florien.ampacheplayer.view.BaseVM
import io.reactivex.android.schedulers.AndroidSchedulers
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the PlayerActivity
 */
class PlayerActivityVM : BaseVM() {
    private val playerControllerIdentifierBase = "playerControllerId"

    @Inject lateinit var audioQueueManager: AudioQueueManager
    private var playerControllerNumber = 0
    internal val connection: PlayerConnection = PlayerConnection()
    private var isBackKeyPreviousSong: Boolean = false
    var player: PlayerController = DummyPlayerController()

    private var playBackTime: Long = 0L

    /**
     * Constructor
     */
    init {
        Timber.tag(this.javaClass.simpleName)
    }

    fun onViewCreated() {
        subscribe(
                observable = audioQueueManager.positionObservable.observeOn(AndroidSchedulers.mainThread()),
                onNext = {
                    notifyPropertyChanged(BR.nextPossible)
                    notifyPropertyChanged(BR.previousPossible)
                })
    }

    fun play() {
        player.play()
    }

    fun playPause() {
        player.apply {
            if (isPlaying()) {
                pause()
            } else {
                resume()
            }
        }
    }

    fun next() {
        audioQueueManager.listPosition += 1
    }

    fun replayOrPrevious() {
        if (isBackKeyPreviousSong) {
            audioQueueManager.listPosition -= 1
        } else {
            player.play()
        }
    }

    @Bindable
    fun getPlayTime(): String = "${playBackTime / 60}:${playBackTime % 60}"

    @Bindable
    fun isNextPossible(): Boolean = audioQueueManager.listPosition < audioQueueManager.itemsCount - 1 && audioQueueManager.listPosition != NO_CURRENT_SONG

    @Bindable
    fun isPreviousPossible(): Boolean = audioQueueManager.listPosition != 0


    /**
     * Private methods
     */

    private fun initController(controller: PlayerController) {
        player = controller
        playerControllerNumber += 1
        subscribe(
                observable = player.playTimeNotifier.map { it / 1000 }.distinct(),
                onNext = {
                    playBackTime = it
                    isBackKeyPreviousSong = it < 10
                    notifyPropertyChanged(BR.playTime)
                },
                onError = {
                    Timber.e(it, "error while retrieving the playtime")
                })
        subscribe(
                observable = player.songNotifier,
                onNext = {
                    //todo display song played
                },
                onError = {
                    Timber.e(it, "error while displaying the current song")
                },
                containerKey = playerControllerIdentifierBase + playerControllerNumber)
    }

    /**
     * Inner class
     */
    inner class PlayerConnection : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            dispose(playerControllerIdentifierBase + playerControllerNumber)
            initController(DummyPlayerController())
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            dispose(playerControllerIdentifierBase + playerControllerNumber)
            initController((service as PlayerService.LocalBinder).service)
        }
    }
}