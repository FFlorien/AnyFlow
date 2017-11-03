package be.florien.ampacheplayer.player

import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import be.florien.ampacheplayer.AmpacheApp
import be.florien.ampacheplayer.manager.AudioQueueManager
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import io.realm.Realm
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

    companion object {
        private val NO_VALUE = -3L
    }

    @Inject
    lateinit var audioQueueManager: AudioQueueManager
    private val mediaPlayer: ExoPlayer by lazy {
        val trackSelector: TrackSelector = DefaultTrackSelector()
        val player = ExoPlayerFactory.newSimpleInstance(this, trackSelector).apply {
            addListener(object : ExoPlayer.EventListener {
                override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
//                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

                override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
//                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

                override fun onPlayerError(error: ExoPlaybackException?) {
//                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
//                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

                override fun onLoadingChanged(isLoading: Boolean) {
//                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

                override fun onPositionDiscontinuity() {
//                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

                override fun onTimelineChanged(timeline: Timeline?, manifest: Any?) {
//                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

            })
        }
//        val bandwidthMeter = DefaultBandwidthMeter()
//// Produces DataSource instances through which media data is loaded.
//        val dataSourceFactory = DefaultDataSourceFactory(this, "ampachePlayerUserAgent", bandwidthMeter)
//// Produces Extractor instances for parsing the media data.
//        val extractorsFactory = DefaultExtractorsFactory()
// This is the MediaSource representing the media to be played.
//        val videoSource = ExtractorMediaSource(Uri.parse("tutu"), dataSourceFactory, extractorsFactory, null, null)
//// Prepare the player with the source.
//        player.prepare(videoSource)
        player
    }

    private var lastPosition: Long = NO_VALUE

    /**
     * Constructor
     */

    override fun onCreate() {
        super.onCreate()
        (application as AmpacheApp).applicationComponent.inject(this)
        audioQueueManager.changeListener.subscribe {
            if (isPlaying()) play()
        }
    }

    fun isPlaying() = mediaPlayer.playWhenReady

    fun play() {
        val song = audioQueueManager.getCurrentSong(Realm.getDefaultInstance())
        mediaPlayer.apply {
            stop()
            val bandwidthMeter = DefaultBandwidthMeter()
// Produces DataSource instances through which media data is loaded.
            val dataSourceFactory = DefaultDataSourceFactory(this@PlayerService, "ampachePlayerUserAgent", bandwidthMeter)
// Produces Extractor instances for parsing the media data.
            val extractorsFactory = DefaultExtractorsFactory()
// This is the MediaSource representing the media to be played.
            val audioSource = ExtractorMediaSource(Uri.parse(song.url), dataSourceFactory, extractorsFactory, null, null)
// Prepare the player with the source.
            prepare(audioSource)
            playWhenReady = true
        }
    }

    fun stop() {
        mediaPlayer.stop()
        lastPosition = NO_VALUE
    }

    fun pause() {
        lastPosition = mediaPlayer.currentPosition
        mediaPlayer.playWhenReady = false
    }

    fun resume() { //todo threading!!!!!!!!!!
        if (lastPosition == NO_VALUE) {
            val songList = audioQueueManager.getCurrentAudioQueue()
            if (songList.isNotEmpty()) {
                play()
            }
        } else {
            mediaPlayer.seekTo(lastPosition)
            mediaPlayer.playWhenReady = true
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
        audioQueueManager.listPosition += 1
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        return what == -38
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
}