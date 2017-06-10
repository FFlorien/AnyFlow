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
import be.florien.ampacheplayer.databinding.ActivityPlayerBinding
import be.florien.ampacheplayer.databinding.ItemSongBinding
import be.florien.ampacheplayer.exception.SessionExpiredException
import be.florien.ampacheplayer.extension.getAmpacheApp
import be.florien.ampacheplayer.extension.startActivity
import be.florien.ampacheplayer.manager.DataManager
import be.florien.ampacheplayer.model.local.Song
import be.florien.ampacheplayer.player.PlayerService
import be.florien.ampacheplayer.view.ConnectActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.net.SocketTimeoutException
import javax.inject.Inject

/**
 * Created by florien on 3/04/17.
 */
class PlayerActivityVM(val activity: Activity, val binding: ActivityPlayerBinding) : BaseObservable() {
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
        activity.getAmpacheApp().activityComponent?.inject(this)
        Timber.tag(this.javaClass.simpleName)
        connection = PlayerConnection()
        bindToService()
        binding.vm = this
        binding.songList.apply {
            adapter = SongAdapter()
            layoutManager = LinearLayoutManager(context)
        }
    }

    /**
     * Public methods
     */

    fun getSongs() {
        dataManager
                .getSongs()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    val songAdapter = binding.songList.adapter as SongAdapter
                    songAdapter.songs = it
                }, {
                    when (it) {
                        is SessionExpiredException -> {
                            Timber.i(it, "The session token is expired")
                            activity.startActivity(ConnectActivity::class)
                        }
                        is SocketTimeoutException -> Timber.e(it, "Couldn't connect to the webservice")
                        else -> Timber.e(it, "Unknown error")
                    }
                })
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
        if (player?.isPlaying() ?: false) {
            player?.resume()
        } else {
            player?.pause()
        }
    }

    fun getArtistAndInfo() {
        dataManager
                .getArtists()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { _ ->
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
                .getPlayLists()
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

    inner class SongAdapter : RecyclerView.Adapter<SongViewHolder>() {
        var songs = listOf<Song>()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun getItemCount() = songs.size

        override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
            holder.binding.song = songs[position]
            holder.binding.vm = this@PlayerActivityVM
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = SongViewHolder(ItemSongBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    inner class SongViewHolder(val binding: ItemSongBinding, root: View = binding.root) : RecyclerView.ViewHolder(root)
}