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

    companion object {
        const val PLAYER_CONTROLLER_IDENTIFIER = "playerControllerId"
    }

    internal val connection: PlayerConnection = PlayerConnection()
    private var playerControllerNumber = 0
    private var isBackKeyPreviousSong: Boolean = false

    var player: PlayerController = DummyPlayerController()

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
    var currentDuration: Int = 0

    @Bindable
    var totalDuration: Int = 0

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
                flowable = audioQueue.currentSongUpdater,
                onNext = {
                    totalDuration = (it?.time ?: 0) * 1000
                    notifyPropertyChanged(BR.totalDuration)
                })
        subscribe(
                observable = audioQueue.positionUpdater.observeOn(AndroidSchedulers.mainThread()),
                onNext = {
                    notifyPropertyChanged(BR.nextPossible)
                    notifyPropertyChanged(BR.previousPossible)
                    notifyPropertyChanged(BR.currentDuration)
                })
        subscribe(
                observable = player.playTimeNotifier,
                onNext = {
                    currentDuration = it.toInt()
                    isBackKeyPreviousSong = currentDuration < 10000
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
            dispose(PLAYER_CONTROLLER_IDENTIFIER + playerControllerNumber)
            initController((service as PlayerService.LocalBinder).service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            dispose(PLAYER_CONTROLLER_IDENTIFIER + playerControllerNumber)
            initController(DummyPlayerController())
        }
    }
}