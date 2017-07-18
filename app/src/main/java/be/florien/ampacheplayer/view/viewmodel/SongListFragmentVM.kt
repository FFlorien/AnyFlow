package be.florien.ampacheplayer.view.viewmodel

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.databinding.BaseObservable
import android.os.IBinder
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import be.florien.ampacheplayer.business.realm.RealmSong
import be.florien.ampacheplayer.databinding.FragmentSongListBinding
import be.florien.ampacheplayer.databinding.ItemSongBinding
import be.florien.ampacheplayer.exception.SessionExpiredException
import be.florien.ampacheplayer.exception.WrongIdentificationPairException
import be.florien.ampacheplayer.extension.ampacheApp
import be.florien.ampacheplayer.extension.startActivity
import be.florien.ampacheplayer.manager.AudioQueueManager
import be.florien.ampacheplayer.manager.DataManager
import be.florien.ampacheplayer.player.PlayerService
import be.florien.ampacheplayer.view.activity.ConnectActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.net.SocketTimeoutException
import javax.inject.Inject

/**
 * Display a list of songs and play it upon selection.
 */
class SongListFragmentVM(val activity: Activity, val binding: FragmentSongListBinding) : BaseObservable() {

    @field:Inject lateinit var dataManager: DataManager
    @field:Inject lateinit var audioQueueManager: AudioQueueManager
    var player: PlayerService? = null

    private var connection: PlayerConnection

    /**
     * Constructor
     */
    init {
        activity.ampacheApp.applicationComponent.inject(this)
        Timber.tag(this.javaClass.simpleName)
        connection = PlayerConnection()
        bindToService()
        binding.vm = this
        binding.songList.adapter = SongAdapter()
        binding.songList.layoutManager = LinearLayoutManager(activity)
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
     * Public methods
     */

    fun refreshSongs() {
        dataManager
                .refreshSongs()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({

                    val songAdapter = binding.songList.adapter as SongAdapter
                    songAdapter.songs = audioQueueManager.currentAudioQueue
                }, {
                    when (it) {
                        is SessionExpiredException -> {
                            Timber.i(it, "The session token is expired")
                            activity.startActivity(ConnectActivity::class)
                        }
                        is WrongIdentificationPairException -> {
                            Timber.i(it, "Couldn't reconnect the user: wrong user/pwd")
                            activity.startActivity(ConnectActivity::class)
                        }
                        is SocketTimeoutException -> {
                            Timber.e(it, "Couldn't connect to the webservice")
                            Toast.makeText(activity, "Couldn't connect to the webservice", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            Timber.e(it, "Unknown error")
                            Toast.makeText(activity, "Couldn't connect to the webservice", Toast.LENGTH_LONG).show()
                            activity.startActivity(ConnectActivity::class)
                        }
                    }
                })
    }


    fun play() {
        player?.play() //todo should play a song in particular
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

    inner class SongAdapter : RecyclerView.Adapter<SongViewHolder>() {
        var songs = listOf<RealmSong>()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun getItemCount() = songs.size

        override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
            holder.binding.song = songs[position]
            holder.binding.vm = this@SongListFragmentVM
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = SongViewHolder(ItemSongBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    inner class SongViewHolder(val binding: ItemSongBinding, root: View = binding.root) : RecyclerView.ViewHolder(root)
}