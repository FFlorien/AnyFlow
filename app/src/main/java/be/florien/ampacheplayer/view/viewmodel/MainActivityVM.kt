package be.florien.ampacheplayer.view.viewmodel

import android.media.AudioManager
import android.media.MediaPlayer
import be.florien.ampacheplayer.App
import be.florien.ampacheplayer.databinding.ActivityMainBinding
import be.florien.ampacheplayer.model.manager.AmpacheConnection
import be.florien.ampacheplayer.model.manager.DataManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

/**
 * ViewModel for the main activity
 */
class MainActivityVM constructor(val binding: ActivityMainBinding) {
    /**
     * Fields
     */
    @Inject lateinit var ampacheConnection: AmpacheConnection
    @Inject lateinit var dataManager: DataManager

    /**
     * Constructor
     */
    init {
        binding.vm = this
        App.ampacheComponent.inject(this)
    }

    /**
     * Buttons calls
     */
    fun connect() {
        ampacheConnection
                .authenticate(binding.inputUsername.text.toString(), binding.inputPassword.text.toString())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { authenticate ->
                    ampacheConnection.authSession = authenticate.auth
                    binding.connect.isEnabled = false
                    binding.getAndPlay.isEnabled = true
                    binding.getArtist.isEnabled = true
                    binding.getAlbum.isEnabled = true
                    binding.getTag.isEnabled = true
                    binding.getPlaylist.isEnabled = true
                }
    }

    fun getSongAndPlay() {
        dataManager
                .getSongs()
                .subscribeOn(Schedulers.io())
                .subscribe {
                    val mediaPlayer = MediaPlayer()
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
                    mediaPlayer.setDataSource(it[0].url)
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