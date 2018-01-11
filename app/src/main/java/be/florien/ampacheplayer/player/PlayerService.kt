package be.florien.ampacheplayer.player

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import be.florien.ampacheplayer.AmpacheApp
import be.florien.ampacheplayer.persistence.model.Song
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
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
        PlayerController, Player.EventListener {

    companion object {
        private val NO_VALUE = -3L
    }

    //todo switch between 3 mediaplayers: 1 playing, the others already preparing previous and next songs
    @Inject
    lateinit var audioQueueManager: AudioQueueManager
    override val playTimeNotifier: Observable<Long> = Observable
            .interval(10, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
            .map { mediaPlayer.contentPosition }
    override val songNotifier: Subject<Song> = BehaviorSubject.create<Song>()

    private val mediaPlayer: ExoPlayer by lazy {
        val trackSelector: TrackSelector = DefaultTrackSelector()
        ExoPlayerFactory.newSimpleInstance(this, trackSelector).apply {
            addListener(this@PlayerService)
        }
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
        val song = audioQueueManager.getCurrentSong()
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
    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onSeekProcessed() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
    }

    override fun onPlayerError(error: ExoPlaybackException?) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onLoadingChanged(isLoading: Boolean) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onPositionDiscontinuity(reason: Int) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onTimelineChanged(timeline: Timeline?, manifest: Any?) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        if (playbackState == Player.STATE_ENDED) {
            audioQueueManager.listPosition += 1
        }
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