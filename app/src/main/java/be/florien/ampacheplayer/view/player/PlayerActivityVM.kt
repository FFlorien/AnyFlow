package be.florien.ampacheplayer.view.player

import android.content.ComponentName
import android.content.ServiceConnection
import android.databinding.Bindable
import android.os.IBinder
import be.florien.ampacheplayer.BR
import be.florien.ampacheplayer.di.ActivityScope
import be.florien.ampacheplayer.player.*
import be.florien.ampacheplayer.view.BaseVM
import io.reactivex.android.schedulers.AndroidSchedulers
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the PlayerActivity
 */
@ActivityScope
class PlayerActivityVM
@Inject
constructor(private val audioQueue: AudioQueue) : BaseVM() {
    private val playerControllerIdentifierBase = "playerControllerId"

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

    /**
     * Public methods
     */

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
        audioQueue.listPosition += 1
    }

    fun replayOrPrevious() {
        if (isBackKeyPreviousSong) {
            audioQueue.listPosition -= 1
        } else {
            player.play()
        }
    }

    /**
     * Bindables
     */

    @Bindable
    fun getCurrentDuration(): Int {
        return playBackTime.toInt()
    }

    @Bindable
    fun getTotalDuration(): Int {
        return audioQueue.getCurrentSong().time * 1000
    }

    @Bindable
    fun getPlayTimeDisplay(): String {
        val playBackTimeInSeconds = playBackTime / 1000
        val minutesDisplay = String.format("%02d", (playBackTimeInSeconds / 60))
        val secondsDisplay = String.format("%02d", (playBackTimeInSeconds % 60))
        return "$minutesDisplay:$secondsDisplay"
    }

    @Bindable
    fun isNextPossible(): Boolean = audioQueue.listPosition < audioQueue.itemsCount - 1 && audioQueue.listPosition != NO_CURRENT_SONG

    @Bindable
    fun isPreviousPossible(): Boolean = audioQueue.listPosition != 0 || playBackTime > 10000


    /**
     * Private methods
     */

    private fun initController(controller: PlayerController) {
        player = controller
        playerControllerNumber += 1
        subscribe(
                observable = audioQueue.positionObservable.observeOn(AndroidSchedulers.mainThread()),
                onNext = {
                    notifyPropertyChanged(BR.nextPossible)
                    notifyPropertyChanged(BR.previousPossible)
                    notifyPropertyChanged(BR.totalDuration)
                    notifyPropertyChanged(BR.currentDuration)
                })
        subscribe(
                observable = player.playTimeNotifier,
                onNext = {
                    playBackTime = it
                    isBackKeyPreviousSong = playBackTime < 10000
                    notifyPropertyChanged(BR.previousPossible)
                    notifyPropertyChanged(BR.playTimeDisplay)
                    notifyPropertyChanged(BR.currentDuration)
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