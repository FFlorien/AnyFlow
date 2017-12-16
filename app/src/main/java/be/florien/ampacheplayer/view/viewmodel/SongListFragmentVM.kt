package be.florien.ampacheplayer.view.viewmodel

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import be.florien.ampacheplayer.business.realm.Song
import be.florien.ampacheplayer.databinding.FragmentSongListBinding
import be.florien.ampacheplayer.databinding.ItemSongPendingBinding
import be.florien.ampacheplayer.databinding.ItemSongPlayingBinding
import be.florien.ampacheplayer.exception.SessionExpiredException
import be.florien.ampacheplayer.exception.WrongIdentificationPairException
import be.florien.ampacheplayer.extension.ampacheApp
import be.florien.ampacheplayer.extension.startActivity
import be.florien.ampacheplayer.manager.AudioQueueManager
import be.florien.ampacheplayer.manager.PersistenceManager
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
private const val LIST_ITEM_TYPE_PENDING = 0
private const val LIST_ITEM_TYPE_PLAYING = 1

class SongListFragmentVM(private val activity: Activity, binding: FragmentSongListBinding) : BaseVM<FragmentSongListBinding>(binding) {

    @field:Inject lateinit var persistenceManager: PersistenceManager
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

    override fun destroy() {
        super.destroy()
        activity.unbindService(connection)
    }

    fun onViewCreated() {
        subscribe(audioQueueManager.changeListener.observeOn(AndroidSchedulers.mainThread()), onNext = {
            binding.songList.adapter.notifyDataSetChanged()

        })
    }

    /**
     * Public methods
     */

    fun refreshSongs() {
        subscribe(
                persistenceManager.refreshSongs().subscribeOn(Schedulers.io()),
                {
                    val songAdapter = binding.songList.adapter as SongAdapter
                    songAdapter.songs = audioQueueManager.getCurrentAudioQueue()
                },
                {
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


    fun play(position: Int) {
        audioQueueManager.listPosition = position
        player?.play()
    }

    fun playPause() {
        player?.let {
            if (it.isPlaying()) it.pause() else it.resume()
        }
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
            holder.bind(songs[position], position)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = if (viewType == LIST_ITEM_TYPE_PENDING) SongPendingViewHolder(parent) else SongPlayingViewHolder(parent)

        override fun getItemViewType(position: Int): Int = if (position == audioQueueManager.listPosition) LIST_ITEM_TYPE_PLAYING else LIST_ITEM_TYPE_PENDING
    }

    inner abstract class SongViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        abstract fun bind(song: Song, position: Int)
    }

    inner class SongPendingViewHolder(
            parent: ViewGroup,
            private val binding: ItemSongPendingBinding = ItemSongPendingBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        : SongViewHolder(binding.root) {

        override fun bind(song: Song, position: Int) {
            binding.song = song
            binding.position = position
            binding.vm = this@SongListFragmentVM
        }

    }

    inner class SongPlayingViewHolder(
            parent: ViewGroup,
            private val binding: ItemSongPlayingBinding = ItemSongPlayingBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        : SongViewHolder(binding.root) {

        override fun bind(song: Song, position: Int) {
            binding.song = song
            binding.position = position
            binding.vm = this@SongListFragmentVM
        }

    }
}