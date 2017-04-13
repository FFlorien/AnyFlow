package be.florien.ampacheplayer.view.viewmodel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.databinding.BaseObservable
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.IBinder
import be.florien.ampacheplayer.App
import be.florien.ampacheplayer.databinding.ActivityPlayerBinding
import be.florien.ampacheplayer.manager.DataManager
import be.florien.ampacheplayer.player.PlayerService
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

/**
 * Created by florien on 3/04/17.
 */
class PlayerActivityVM(val context: Context, val binding: ActivityPlayerBinding) : BaseObservable() {
    /**
     * Fields
     */
    @field:Inject lateinit var dataManager: DataManager
    var player: PlayerService? = null

    /**
     * Constructor
     */
    init {
        App.ampacheComponent.inject(this)
        bindToService()
        binding.vm = this
    }

    /**
     * Public methods
     */
    fun getSongAndPlay() {
        dataManager
                .getSongs()
                .subscribeOn(Schedulers.io())
                .subscribe {
                    val song = it[0]
                    song.load()
                    player?.play(song)
                }
    }

    fun getArtistAndInfo() {
        dataManager
                .getArtists()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { root ->
                    //                    binding.info.text = root.size.toString()
                }
    }

    fun getAlbumAndInfo() {
        dataManager
                .getAlbums()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { root ->
                    //                    binding.info.text = root.size.toString()
                }
    }

    fun getTagAndInfo() {
        dataManager
                .getTags()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { root ->
                    //                    binding.info.text = root.size.toString()
                }
    }

    fun getPlaylistAndInfo() {
        dataManager
                .getPlaylists()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { root ->
                    //                    binding.info.text = root.size.toString()
                }
    }

    /**
     * Private methods
     */

    private fun bindToService() {
        context.bindService(Intent(context, PlayerService::class.java), PlayerConnection(), Context.BIND_AUTO_CREATE)
    }

    fun destroy() {
        context.unbindService(PlayerConnection())
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