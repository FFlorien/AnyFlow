package be.florien.ampacheplayer.view.viewmodel

import android.app.Activity
import android.databinding.BaseObservable
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import be.florien.ampacheplayer.business.local.Song
import be.florien.ampacheplayer.databinding.FragmentSongListBinding
import be.florien.ampacheplayer.databinding.ItemSongBinding
import be.florien.ampacheplayer.exception.SessionExpiredException
import be.florien.ampacheplayer.exception.WrongIdentificationPairException
import be.florien.ampacheplayer.extension.ampacheApp
import be.florien.ampacheplayer.extension.startActivity
import be.florien.ampacheplayer.manager.AudioQueueManager
import be.florien.ampacheplayer.view.activity.ConnectActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.net.SocketTimeoutException
import javax.inject.Inject

/**
 * Created by florien on 9/07/17.
 */
class SongListFragmentVM(val activity: Activity, val binding: FragmentSongListBinding) : BaseObservable() {

    @field:Inject lateinit var audioQueueManager: AudioQueueManager

    /**
     * Constructor
     */
    init {
        activity.ampacheApp.applicationComponent.inject(this)
        Timber.tag(this.javaClass.simpleName)
        binding.vm = this
        binding.songList.adapter = SongAdapter()
        binding.songList.layoutManager = LinearLayoutManager(activity)
    }

    fun destroy() {
    }

    /**
     * Public methods
     */

    fun getSongs() {
        audioQueueManager
                .getAudioQueue()
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


    fun play(song: Song) {
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
            holder.binding.vm = this@SongListFragmentVM
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = SongViewHolder(ItemSongBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    inner class SongViewHolder(val binding: ItemSongBinding, root: View = binding.root) : RecyclerView.ViewHolder(root)
}