package be.florien.ampacheplayer.view.viewmodel

import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import be.florien.ampacheplayer.databinding.ActivityMainBinding
import be.florien.ampacheplayer.model.retrofit.AmpacheConnection
import io.reactivex.schedulers.Schedulers

/**
 * Created by florien on 21/03/17.
 */
class MainActivityVM constructor(val binding: ActivityMainBinding) {
    fun connectAndRetrieve() {
        val connection = AmpacheConnection()
        connection
                .authenticate(binding.inputUsername.text.toString(), binding.inputPassword.text.toString())
                .subscribeOn(Schedulers.io())
                .doOnNext { authenticate ->
                    connection.authToken = authenticate.auth
                    Log.d("YES", "Auth is " + authenticate.auth)
                }
                .flatMap({ authenticate -> connection.getSongs() })
                .doOnNext { root ->
                    Log.d("YES", "Song is " + root.songs[0].title)
                    val mediaPlayer = MediaPlayer()
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
                    Log.d("YES", "URL is " + root.songs[0].url)
                    mediaPlayer.setDataSource(root.songs[0].url)
                    mediaPlayer.prepare()
                    mediaPlayer.start()

                }
                .subscribe()
    }

    init {
        binding.vm = this
    }

}