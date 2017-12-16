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
import be.florien.ampacheplayer.player.PlayerService
import io.reactivex.android.schedulers.AndroidSchedulers
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the PlayerActivity
 */
class PlayerActivityVM(private val activity: Activity)
    : BaseVM<ActivityPlayerBinding>(DataBindingUtil.setContentView(activity, R.layout.activity_player)) {

    private val connection: PlayerConnection

    var player: PlayerService? = null

    @field:Inject lateinit var audioQueueManager: AudioQueueManager

    /**
     * Constructor
     */
    init {
        Timber.tag(this.javaClass.simpleName)
        connection = PlayerConnection()
        bindToService()
        binding.vm = this
        activity.ampacheApp.applicationComponent.inject(this)
    }

    override fun onViewCreated() {
        super.onViewCreated()
        subscribe(audioQueueManager.changeListener.observeOn(AndroidSchedulers.mainThread()), onNext = {
            notifyPropertyChanged(BR.nextPossible)
            notifyPropertyChanged(BR.previousPossible)
        })
    }

    fun play() {
        player?.play()
    }

    fun playPause() {
        player?.apply {
            if (isPlaying()) {
                pause()
            } else {
                resume()
            }
        }
    }

    fun next() {
        try {
            audioQueueManager.listPosition += 1
        } catch (exception: IndexOutOfBoundsException) {
            //notify
        }
    }

    fun previous() {
        try {
            audioQueueManager.listPosition -= 1
        } catch (exception: IndexOutOfBoundsException) {
            //notify
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