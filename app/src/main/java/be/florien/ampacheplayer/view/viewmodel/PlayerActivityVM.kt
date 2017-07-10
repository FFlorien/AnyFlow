package be.florien.ampacheplayer.view.viewmodel

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.databinding.BaseObservable
import android.os.IBinder
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import be.florien.ampacheplayer.business.local.Song
import be.florien.ampacheplayer.databinding.ActivityPlayerBinding
import be.florien.ampacheplayer.databinding.ItemSongBinding
import be.florien.ampacheplayer.extension.ampacheApp
import be.florien.ampacheplayer.manager.DataManager
import be.florien.ampacheplayer.player.PlayerService
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the PlayerActivity
 */
class PlayerActivityVM(val activity: Activity, binding: ActivityPlayerBinding) : BaseObservable() {
    /**
     * Fields
     */
    @field:Inject lateinit var dataManager: DataManager

    private val connection: PlayerConnection
    var player: PlayerService? = null

    /**
     * Constructor
     */
    init {
        activity.ampacheApp.applicationComponent.inject(this)
        Timber.tag(this.javaClass.simpleName)
        connection = PlayerConnection()
        bindToService()
        binding.vm = this
    }

    fun play(song: Song) {
        dataManager
                .getSong(song.id)
                .subscribeOn(Schedulers.io())
                .subscribe({
                    player?.play(it)
                }, {
                    Timber.e("Error while playing", it)
                })
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

    /**
     * Private methods
     */

    private fun bindToService() {
        activity.bindService(Intent(activity, PlayerService::class.java), connection, Context.BIND_AUTO_CREATE)
    }

    fun destroy() {
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