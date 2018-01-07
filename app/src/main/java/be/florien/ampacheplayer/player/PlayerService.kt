package be.florien.ampacheplayer.player

import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import be.florien.ampacheplayer.AmpacheApp
import be.florien.ampacheplayer.persistence.model.Song
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import io.realm.Realm
import java.util.concurrent.TimeUnit
import javax.inject.Inject


/**
 * Service used to handle the media player.
 */
class PlayerService : Service(),
        PlayerController,
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

    //todo switch between 3 mediaplayers: 1 playing, the others already preparing previous and next songs
    @Inject
    lateinit var audioQueueManager: AudioQueueManager
    override val playTimeNotifier: Observable<Long> = Observable
            .interval(100, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
            .map { mediaPlayer.contentPosition }
    override val songNotifier: Subject<Song> = BehaviorSubject.create<Song>()

    private val mediaPlayer: ExoPlayer by lazy {
        val trackSelector: TrackSelector = DefaultTrackSelector()
        ExoPlayerFactory.newSimpleInstance(this, trackSelector)
    }

    private var lastPosition: Long = NO_VALUE

    /**
     * Constructor
     */

    override fun onCreate() {
        super.onCreate()
        (application as AmpacheApp).applicationComponent.inject(this)
        audioQueueManager.positionObservable.subscribe {
            if (isPlaying()) play()
        }
    }

    override fun isPlaying() = mediaPlayer.playWhenReady

    override fun play() {
        val song = audioQueueManager.getCurrentSong(Realm.getDefaultInstance())
        mediaPlayer.apply {
            stop()
            val bandwidthMeter = DefaultBandwidthMeter()
// Produces DataSource instances through which media data is loaded.
            val dataSourceFactory = DefaultDataSourceFactory(this@PlayerService, "ampachePlayerUserAgent", bandwidthMeter)
// Produces Extractor instances for parsing the media data.
            val extractorsFactory = DefaultExtractorsFactory()
// This is the MediaSource representing the media to be played.
            val audioSource = ExtractorMediaSource.Factory(dataSourceFactory)
                    .setExtractorsFactory(extractorsFactory)
                    .createMediaSource(Uri.parse(song.url))
// Prepare the player with the source.
            prepare(audioSource)
            playWhenReady = true
        }
        songNotifier.onNext(song)
    }

    override fun stop() {
        mediaPlayer.stop()
        lastPosition = NO_VALUE
    }

    override fun pause() {
        lastPosition = mediaPlayer.currentPosition
        mediaPlayer.playWhenReady = false
    }

    override fun resume() { //todo threading!!!!!!!!!!
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