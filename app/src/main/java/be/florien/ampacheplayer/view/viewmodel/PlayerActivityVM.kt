package be.florien.ampacheplayer.view.viewmodel

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.databinding.Bindable
import android.databinding.DataBindingUtil
import android.os.IBinder
import be.florien.ampacheplayer.BR
import be.florien.ampacheplayer.R
import be.florien.ampacheplayer.databinding.ActivityPlayerBinding
import be.florien.ampacheplayer.extension.ampacheApp
import be.florien.ampacheplayer.manager.AudioQueueManager
import be.florien.ampacheplayer.manager.NO_CURRENT_SONG
import be.florien.ampacheplayer.player.DummyPlayerController
import be.florien.ampacheplayer.player.PlayerController
import be.florien.ampacheplayer.player.PlayerService
import io.reactivex.android.schedulers.AndroidSchedulers
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the PlayerActivity
 */
class PlayerActivityVM(private val activity: Activity) : BaseVM<ActivityPlayerBinding>(DataBindingUtil.setContentView(activity, R.layout.activity_player)) {
    private val playerControllerIdentifierBase = "playerControllerId"

    @Inject lateinit var audioQueueManager: AudioQueueManager
    private var playerControllerNumber = 0
    private val connection: PlayerConnection = PlayerConnection()
    private var isBackKeyPreviousSong: Boolean = false
    var player: PlayerController = DummyPlayerController()

    /**
     * Constructor
     */
    init {
        Timber.tag(this.javaClass.simpleName)
        activity.ampacheApp.applicationComponent.inject(this)
        bindToService()
        binding.vm = this
    }

    fun onViewCreated() {
        subscribe(audioQueueManager.changeListener.observeOn(AndroidSchedulers.mainThread()), onNext = {
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
    fun isNextPossible(): Boolean = audioQueueManager.listPosition < audioQueueManager.itemsCount - 1 && audioQueueManager.listPosition != NO_CURRENT_SONG

    @Bindable
    fun isPreviousPossible(): Boolean = audioQueueManager.listPosition != 0


    /**
     * Private methods
     */

    private fun bindToService() {
        activity.bindService(Intent(activity, PlayerService::class.java), connection, Context.BIND_AUTO_CREATE)
    }

    override fun destroy() {
        super.destroy()
        activity.unbindService(connection)
    }

    private fun initController(controller: PlayerController) {
        player = controller
        playerControllerNumber += 1
        subscribe(player.playTimeNotifier.map { it / 1000 }.distinct(),
                {
                    isBackKeyPreviousSong = it < 10
                    binding.playTime.text = "${it / 60}:${it % 60}"
                })// todo onError
        subscribe(player.songNotifier,
                {
                    //todo display song played
                },
                containerKey = playerControllerIdentifierBase + playerControllerNumber)// todo onError
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