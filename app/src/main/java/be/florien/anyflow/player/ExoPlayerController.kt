package be.florien.anyflow.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import be.florien.anyflow.extension.iLog
import be.florien.anyflow.persistence.server.AmpacheConnection
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.util.Util
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ExoPlayerController
@Inject constructor(
        private var playingQueue: PlayingQueue,
        private var ampacheConnection: AmpacheConnection,
        private val context: Context,
        okHttpClient: OkHttpClient) : PlayerController, Player.EventListener {

    private val stateChangePublisher: BehaviorSubject<PlayerController.State> = BehaviorSubject.create()
    override val stateChangeNotifier: Flowable<PlayerController.State> =
            stateChangePublisher
                    .toFlowable(BackpressureStrategy.LATEST)
                    .share()
                    .publish()
                    .autoConnect()

    companion object {
        private const val NO_VALUE = -3L
    }

    override val playTimeNotifier: Observable<Long> = Observable
            .interval(10, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
            .map { mediaPlayer.contentPosition }
            .doOnNext { lastPosition = it }
            .share()
            .publish()
            .autoConnect()

    private var subscription = CompositeDisposable()

    private val mediaPlayer: ExoPlayer
    private var lastPosition: Long = NO_VALUE
    private var dataSourceFactory: DefaultDataSourceFactory

    private var isReceiverRegistered: Boolean = false
    private val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private val myNoisyAudioStreamReceiver = BecomingNoisyReceiver()

    inner class BecomingNoisyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
                pause()
            }
        }
    }

    private var currentSongsUrls: List<String> = listOf()
    private val audioSource = ConcatenatingMediaSource()

    /**
     * Constructor
     */

    init {
        val trackSelector: TrackSelector = DefaultTrackSelector()
        mediaPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector).apply {
            addListener(this@ExoPlayerController)
        }
        val bandwidthMeter = DefaultBandwidthMeter()
        val userAgent = Util.getUserAgent(context, "anyflowUserAgent")
        dataSourceFactory = DefaultDataSourceFactory(context, DefaultBandwidthMeter(), OkHttpDataSourceFactory(okHttpClient, userAgent, bandwidthMeter))
        subscription.add(playingQueue.songUrlListUpdater
                .withLatestFrom(
                        playingQueue.positionUpdater.toFlowable(BackpressureStrategy.LATEST),
                        BiFunction { songUrls: List<String>, position: Int ->
                            val topIndex = position + 10
                            songUrls.subList(position, if (topIndex > songUrls.size) songUrls.size else topIndex)
                        })
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    prepare(it)
                }

        )
    }

    override fun isPlaying() = mediaPlayer.playWhenReady

    override fun play() {
        lastPosition = NO_VALUE
        resume()
    }

    override fun prepare(songsUrl: List<String>) {
        val diffResult = DiffUtil.calculateDiff(DiffCallback(currentSongsUrls, songsUrl))
        currentSongsUrls = songsUrl
        diffResult.dispatchUpdatesTo(DiffUpdate())
        mediaPlayer.seekTo(0, C.TIME_UNSET)
    }

    override fun stop() {
        mediaPlayer.stop()
        lastPosition = NO_VALUE
    }

    override fun pause() {
        mediaPlayer.playWhenReady = false
        stateChangePublisher.onNext(PlayerController.State.PAUSE)
    }

    override fun resume() {
        if (lastPosition == NO_VALUE) {
            seekTo(0)
        } else {
            seekTo(lastPosition)
        }

        mediaPlayer.playWhenReady = true
        stateChangePublisher.onNext(PlayerController.State.PLAY)
    }

    override fun seekTo(duration: Long) {
        mediaPlayer.seekTo(duration)
    }

    override fun onDestroy() {
        subscription.dispose()
    }

    /**
     * Listener implementation
     */
    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
        iLog("onPlaybackParametersChanged")
    }

    override fun onSeekProcessed() {
        iLog("onSeekProcessed")
    }

    override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
        playingQueue.listPosition = mediaPlayer.currentWindowIndex
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        iLog(error, "Error while playback")
        if (error.cause is HttpDataSource.InvalidResponseCodeException) {
            if ((error.cause as HttpDataSource.InvalidResponseCodeException).responseCode == 403) {
                stateChangePublisher.onNext(PlayerController.State.RECONNECT)
                ampacheConnection.reconnect(Observable.fromCallable { prepare(currentSongsUrls) }).subscribeOn(Schedulers.io()).subscribe()
                // todo unsubscribe + on complete/next/error
            }
        }
    }

    override fun onLoadingChanged(isLoading: Boolean) {
        iLog("onLoadingChanged: $isLoading")
    }

    override fun onPositionDiscontinuity(reason: Int) {
        iLog("onPositionDiscontinuity")
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        iLog("onRepeatModeChanged")
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        iLog("onShuffleModeEnabledChanged")
    }

    override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
        iLog("onTimelineChanged")
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        when (playbackState) {
            Player.STATE_ENDED -> {
                playingQueue.listPosition += 1
                lastPosition = 0
            }
            Player.STATE_BUFFERING -> {
                ampacheConnection.resetReconnectionCount()
                stateChangePublisher.onNext(PlayerController.State.BUFFER)
            }
            Player.STATE_IDLE -> stateChangePublisher.onNext(PlayerController.State.NO_MEDIA)
            Player.STATE_READY -> stateChangePublisher.onNext(if (playWhenReady) PlayerController.State.PLAY else PlayerController.State.PAUSE)
        }

        if (playWhenReady && !isReceiverRegistered) {
            context.registerReceiver(myNoisyAudioStreamReceiver, intentFilter)
        } else if (isReceiverRegistered) {
            context.unregisterReceiver(myNoisyAudioStreamReceiver)
        }
    }



    inner class DiffCallback(private val oldList: List<String>, private val newList: List<String>):  DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) = oldList[oldItemPosition] == newList[newItemPosition]

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) = areItemsTheSame(oldItemPosition, newItemPosition)
    }
    inner class DiffUpdate: ListUpdateCallback {
        override fun onChanged(position: Int, count: Int, payload: Any?) {
            onRemoved(position, count)
            onInserted(position, count)
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            audioSource.moveMediaSource(fromPosition, toPosition)
        }

        override fun onInserted(position: Int, count: Int) {
            currentSongsUrls.subList(position, position + count).forEachIndexed { index, url ->
                audioSource.addMediaSource(position + index , ExtractorMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(Uri.parse(ampacheConnection.getSongUrl(url))))
            }
        }

        override fun onRemoved(position: Int, count: Int) {
            audioSource.removeMediaSourceRange(position, position + count)
        }

    }

}