package be.florien.ampacheplayer.player

import android.content.Context
import android.net.Uri
import be.florien.ampacheplayer.api.AmpacheConnection
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.util.Util
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import okhttp3.OkHttpClient
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ExoPlayerController
@Inject constructor(context: Context, private var audioQueueManager: AudioQueue, private var ampacheConnection: AmpacheConnection, okHttpClient: OkHttpClient) : PlayerController, Player.EventListener {

    companion object {
        private const val NO_VALUE = -3L
    }

    //todo switch between 3 mediaplayers: 1 playing, the others already preparing previous and next accounts
    override val playTimeNotifier: Observable<Long> = Observable
            .interval(10, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
            .map { mediaPlayer.contentPosition }

    private val mediaPlayer: ExoPlayer
    private var lastPosition: Long = NO_VALUE
    private var dataSourceFactory: DefaultDataSourceFactory
    private var extractorsFactory: DefaultExtractorsFactory

    /**
     * Constructor
     */

    init {
        audioQueueManager.positionObservable.subscribe {
            if (isPlaying()) play() else lastPosition = NO_VALUE
        }
        val trackSelector: TrackSelector = DefaultTrackSelector()
        mediaPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector).apply {
            addListener(this@ExoPlayerController)
        }
        val bandwidthMeter = DefaultBandwidthMeter()
        val userAgent = Util.getUserAgent(context, "ampachePlayerUserAgent")
        dataSourceFactory = DefaultDataSourceFactory(context, DefaultBandwidthMeter(), OkHttpDataSourceFactory(okHttpClient, userAgent, bandwidthMeter))
        extractorsFactory = DefaultExtractorsFactory()

    }

    override fun isPlaying() = mediaPlayer.playWhenReady

    override fun play() {
        val song = audioQueueManager.getCurrentSong()
        mediaPlayer.apply {
            stop()
            val audioSource = ExtractorMediaSource.Factory(dataSourceFactory)
                    .setExtractorsFactory(extractorsFactory)
                    .createMediaSource(Uri.parse(ampacheConnection.getSongUrl(song)))
            prepare(audioSource)
            playWhenReady = true
        }
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

    override fun seekTo(duration: Int) {
        mediaPlayer.seekTo(duration.toLong())
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

    override fun onPlayerError(error: ExoPlaybackException) {
        Timber.i(error, "Error while playback")
        if (error.cause is HttpDataSource.InvalidResponseCodeException) {
            if ((error.cause as HttpDataSource.InvalidResponseCodeException).responseCode == 403) {
                //todo reconnect
            }
        }
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

}