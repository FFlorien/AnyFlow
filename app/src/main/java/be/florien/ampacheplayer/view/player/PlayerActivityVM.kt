package be.florien.ampacheplayer.view.player

import android.content.ComponentName
import android.content.ServiceConnection
import android.databinding.Bindable
import android.os.IBinder
import be.florien.ampacheplayer.BR
import be.florien.ampacheplayer.di.ActivityScope
import be.florien.ampacheplayer.player.*
import be.florien.ampacheplayer.view.BaseVM
import be.florien.ampacheplayer.view.customView.PlayerControls
import io.reactivex.android.schedulers.AndroidSchedulers
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.absoluteValue

/**
 * ViewModel for the PlayerActivity
 */
@ActivityScope
class PlayerActivityVM
@Inject
constructor(private val audioQueue: AudioQueue) : BaseVM(), PlayerControls.OnActionListener {

    private val playerControllerIdentifierBase = "playerControllerId"

    private var playerControllerNumber = 0
    internal val connection: PlayerConnection = PlayerConnection()
    private var isBackKeyPreviousSong: Boolean = false
    var player: PlayerController = DummyPlayerController()

    @Bindable
    var currentDuration: Int = 0

    /**
     * Constructor
     */
    init {
        Timber.tag(this.javaClass.simpleName)
    }

    /**
     * PlayerControls.OnActionListener methods
     */
    override fun onPreviousClicked() {
        if (isBackKeyPreviousSong) {
            audioQueue.listPosition -= 1
        } else {
            player.play()
        }
    }

    override fun onNextClicked() {
        audioQueue.listPosition += 1
    }

    override fun onPlayPauseClicked() {
        player.apply {
            if (isPlaying()) {
                pause()
            } else {
                resume()
            }
        }
    }

    override fun onCurrentDurationChanged(newDuration: Int) {
        if ((currentDuration - newDuration).absoluteValue > 1000) {
            player.seekTo(newDuration)
        }
    }

    /**
     * Bindables
     */

    @Bindable
    fun getTotalDuration(): Int {
        return audioQueue.getCurrentSong().time * 1000
    }

    @Bindable
    fun isNextPossible(): Boolean = audioQueue.listPosition < audioQueue.itemsCount - 1 && audioQueue.listPosition != NO_CURRENT_SONG

    @Bindable
    fun isPreviousPossible(): Boolean = audioQueue.listPosition != 0 || currentDuration > 10000


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
                    currentDuration = it.toInt()
                    isBackKeyPreviousSong = currentDuration < 10000
                    notifyPropertyChanged(BR.nextPossible)
                    notifyPropertyChanged(BR.previousPossible)
                    notifyPropertyChanged(BR.currentDuration)
                },
                onError = {
                    Timber.e(it, "error while retrieving the playtime")
                })
    }

    /**
     * Inner class
     */
    inner class PlayerConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            dispose(playerControllerIdentifierBase + playerControllerNumber)
            initController((service as PlayerService.LocalBinder).service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            dispose(playerControllerIdentifierBase + playerControllerNumber)
            initController(DummyPlayerController())
        }
    }
}