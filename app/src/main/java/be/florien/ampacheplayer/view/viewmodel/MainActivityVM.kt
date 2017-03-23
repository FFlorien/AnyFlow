package be.florien.ampacheplayer.view.viewmodel

import android.media.AudioManager
import android.media.MediaPlayer
import be.florien.ampacheplayer.databinding.ActivityMainBinding
import be.florien.ampacheplayer.model.retrofit.AmpacheConnection
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * Created by florien on 21/03/17.
 */
class MainActivityVM constructor(val binding: ActivityMainBinding) {

    init {
        binding.vm = this
    }

    private val ampacheConnection: AmpacheConnection = AmpacheConnection()

    fun connect() {
        ampacheConnection
                .authenticate(binding.inputUsername.text.toString(), binding.inputPassword.text.toString())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { authenticate ->
                    ampacheConnection.authToken = authenticate.auth
                    binding.connect.isEnabled = false
                    binding.getAndPlay.isEnabled = true
                    binding.getArtist.isEnabled = true
                    binding.getAlbum.isEnabled = true
                    binding.getTag.isEnabled = true
                    binding.getPlaylist.isEnabled = true
                }
    }

    fun getSongAndPlay() {
        ampacheConnection
                .getSongs()
                .subscribeOn(Schedulers.io())
                .subscribe { root ->
                    val mediaPlayer = MediaPlayer()
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
                    mediaPlayer.setDataSource(root.songs[0].url)
                    mediaPlayer.prepare()
                    mediaPlayer.start()
                }
    }

    fun getArtistAndInfo() {
        ampacheConnection
                .getArtists()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { root ->
                    binding.info.text = root.total_count.toString()
                }
    }

    fun getAlbumAndInfo() {
        ampacheConnection
                .getAlbums()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { root ->
                    binding.info.text = root.total_count.toString()
                }
    }

    fun getTagAndInfo() {
        ampacheConnection
                .getTags()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { root ->
                    binding.info.text = root.total_count.toString()
                }
    }

    fun getPlaylistAndInfo() {
        ampacheConnection
                .getPlaylists()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { root ->
                    binding.info.text = root.total_count.toString()
                }
    }
}