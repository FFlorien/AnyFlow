package be.florien.ampacheplayer.view

import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import be.florien.ampacheplayer.R
import be.florien.ampacheplayer.model.retrofit.AmpacheConnection
import io.reactivex.schedulers.Schedulers

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val connection = AmpacheConnection()
        connection
                .authenticate("", "")
                .subscribeOn(Schedulers.io())
                .doOnNext { authenticate ->
                    connection.authToken = authenticate.auth
                    Log.d("YES", "Auth is " + authenticate.auth) }
                .flatMap({authenticate -> connection.getSongs()})
                .doOnNext { root ->
                    Log.d("YES", "Song is " + root.songs.title)
                    val mediaPlayer = MediaPlayer()
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
                    Log.d("YES", "URL is " + root.songs.url)
                    mediaPlayer.setDataSource(root.songs.url)
                    mediaPlayer.prepare()
                    mediaPlayer.start()

                }
                .subscribe()
    }
}