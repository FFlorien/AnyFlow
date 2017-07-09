package be.florien.ampacheplayer.player

import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.AsyncTask
import android.os.Binder
import android.os.IBinder
import be.florien.ampacheplayer.AmpacheApp
import be.florien.ampacheplayer.manager.AudioQueueManager
import be.florien.ampacheplayer.business.local.Song
import javax.inject.Inject


/**
 * Service used to handle the media player.
 */
class PlayerService : Service(),
        MediaPlayer.OnInfoListener,
        MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        AudioManager.OnAudioFocusChangeListener {
    val NO_VALUE = -3

    @Inject
    lateinit var audioQueueManager: AudioQueueManager
    private var mediaPlayer: MediaPlayer = MediaPlayer()

    private var lastPosition: Int = NO_VALUE
    private var songUrl: String? = null

    /**
     * Constructor
     */
    init {
        mediaPlayer.setOnCompletionListener(this)
        mediaPlayer.setOnErrorListener(this)
        mediaPlayer.setOnPreparedListener(this)
        mediaPlayer.setOnBufferingUpdateListener(this)
        mediaPlayer.setOnSeekCompleteListener(this)
        mediaPlayer.setOnInfoListener(this)
        mediaPlayer.reset()
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
        (application as AmpacheApp).applicationComponent.inject(this)
    }

    /**
     * Player methods
     */

    fun isPlaying() = mediaPlayer.isPlaying

    fun play(song: Song) {
        songUrl = song.url
        Background().execute()
    }

    fun stop() {
        mediaPlayer.stop()

    }

    fun pause() {
        lastPosition = mediaPlayer.currentPosition
        mediaPlayer.pause()
    }

    fun resume() {
        if (lastPosition == NO_VALUE) {
            audioQueueManager.getAudioQueue().subscribe({
                play(it[0])
            })
        } else {
            mediaPlayer.seekTo(lastPosition)
            mediaPlayer.start()
        }
    }

    /**
     * Listener implementation
     */
    override fun onInfo(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        return false
    }

    override fun onBufferingUpdate(mp: MediaPlayer?, percent: Int) {
    }

    override fun onPrepared(mp: MediaPlayer?) {
    }

    override fun onSeekComplete(mp: MediaPlayer?) {
    }

    override fun onCompletion(mp: MediaPlayer?) {
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        return false
    }

    override fun onAudioFocusChange(focusChange: Int) {
    }

    /**
     * Binder methods
     */

    private val iBinder = LocalBinder()

    override fun onBind(intent: Intent): IBinder? {
        return iBinder
    }

    inner class LocalBinder : Binder() {
        val service: PlayerService
            get() = this@PlayerService
    }

    inner class Background : AsyncTask<Void, Void, Boolean>() {
        override fun doInBackground(vararg params: Void?): Boolean {
            mediaPlayer.apply {
                stop()
                reset()
                setDataSource(this@PlayerService, Uri.parse(songUrl))
                prepare()
                start()
            }
            return true
        }
    }
}