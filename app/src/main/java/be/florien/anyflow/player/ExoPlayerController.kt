package be.florien.anyflow.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import be.florien.anyflow.data.server.AmpacheConnection
import be.florien.anyflow.extension.iLog
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.HttpDataSource
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import javax.inject.Inject
import kotlin.math.min

class ExoPlayerController
@Inject constructor(
        private var playingQueue: PlayingQueue,
        private var ampacheConnection: AmpacheConnection,
        private val context: Context,
        okHttpClient: OkHttpClient
) : PlayerController, Player.Listener {

    companion object {
        private const val NO_VALUE = -3L
    }

    override val stateChangeNotifier: LiveData<PlayerController.State> = MutableLiveData()

    override val playTimeNotifier: LiveData<Long> = MutableLiveData()

    private val mediaPlayer: ExoPlayer
    private var lastPosition: Int = 0
    private var lastDuration: Long = NO_VALUE

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

    /**
     * Constructor
     */

    init {
        val dataSourceFactory = DefaultDataSourceFactory(
                context,
                DefaultBandwidthMeter.Builder(context).build(),
                OkHttpDataSource.Factory(okHttpClient)
        )
        mediaPlayer = SimpleExoPlayer
                .Builder(context, DefaultRenderersFactory(context).apply { setEnableAudioOffload(true) })
                .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
                .build()
                .apply {
                    addListener(this@ExoPlayerController)
                    //todo apply experimentalSetOffloadSchedulingEnabled(true) when in background
                }
        playingQueue.songUrlListUpdater.observeForever { urls ->
            setItemsToMediaPlayer(urls)
            prepare()
        }
        playingQueue.positionUpdater.observeForever {
            val shouldPlay = mediaPlayer.playWhenReady
            val oldPosition = lastPosition
            lastPosition = it
            if (lastPosition != oldPosition) {
                if (lastPosition == oldPosition + 1) {
                    mediaPlayer.removeMediaItem(0)

                    val url = playingQueue.songUrlListUpdater.value?.get(lastPosition + 1)
                    if (url != null) {
                        mediaPlayer.addMediaItem(urlToMediaItem(url))
                    }
                } else {
                    val urls = playingQueue.songUrlListUpdater.value
                    if (urls != null) {
                        setItemsToMediaPlayer(urls)
                        prepare()
                    }
                }
            }
            mediaPlayer.playWhenReady = shouldPlay
        }
        GlobalScope.launch(Dispatchers.Default) {
            while (true) {
                delay(10)
                withContext(Dispatchers.Main) {
                    val contentPosition = mediaPlayer.contentPosition
                    (playTimeNotifier as MutableLiveData).value = contentPosition
                    lastDuration = contentPosition
                }
            }
        }
    }

    override fun isPlaying() = mediaPlayer.playWhenReady

    override fun play() {
        lastDuration = NO_VALUE
        resume()
    }

    private fun prepare() {
        GlobalScope.launch(Dispatchers.Main) {
            mediaPlayer.prepare()
        }
    }

    override fun stop() {
        mediaPlayer.stop()
        lastDuration = NO_VALUE
    }

    override fun pause() {
        mediaPlayer.playWhenReady = false
        (stateChangeNotifier as MutableLiveData).value = PlayerController.State.PAUSE
    }

    override fun resume() {
        if (lastDuration == NO_VALUE) {
            seekTo(0)
        } else {
            seekTo(lastDuration)
        }

        mediaPlayer.playWhenReady = true
        (stateChangeNotifier as MutableLiveData).value = PlayerController.State.PLAY
    }

    override fun seekTo(duration: Long) {
        mediaPlayer.seekTo(duration)
    }

    override fun onDestroy() {
        //todo
    }

    /**
     * Listener implementation
     */

    override fun onEvents(player: Player, events: Player.Events) {
        if (events.contains(Player.EVENT_PLAYER_ERROR)) {
            val error = player.playerError
            if (error != null) {
//todo
            }
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        iLog(error, "Error while playback")
        if ((error.cause as? HttpDataSource.InvalidResponseCodeException)?.responseCode == 403) {
            (stateChangeNotifier as MutableLiveData).value = PlayerController.State.RECONNECT
            GlobalScope.launch {
                ampacheConnection.reconnect { prepare() }
            }
        }
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        when (playbackState) {
            Player.STATE_ENDED -> {
                lastDuration = 0
            }
            Player.STATE_BUFFERING -> {
                ampacheConnection.resetReconnectionCount()
                (stateChangeNotifier as MutableLiveData).value = PlayerController.State.BUFFER
            }
            Player.STATE_IDLE -> (stateChangeNotifier as MutableLiveData).value = PlayerController.State.NO_MEDIA
            Player.STATE_READY -> (stateChangeNotifier as MutableLiveData).value = if (playWhenReady) PlayerController.State.PLAY else PlayerController.State.PAUSE
        }

        if (playWhenReady && !isReceiverRegistered) {
            context.registerReceiver(myNoisyAudioStreamReceiver, intentFilter)
        } else if (isReceiverRegistered) {
            context.unregisterReceiver(myNoisyAudioStreamReceiver)
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
            playingQueue.listPosition += 1
            lastDuration = 0
        }
    }

    /**
     * PRIVATE METHODS
     */

    private fun setItemsToMediaPlayer(urls: List<String>) {
        if (playingQueue.listPosition >= playingQueue.queueSize) {
            return
        }
        mediaPlayer.clearMediaItems()
        val toIndex = min(playingQueue.listPosition + 1, playingQueue.queueSize - 1)
        mediaPlayer.addMediaItems(urls.subList(playingQueue.listPosition, toIndex + 1).map { url -> urlToMediaItem(url) })
    }

    private fun urlToMediaItem(url: String) = MediaItem.fromUri(Uri.parse(ampacheConnection.getSongUrl(url)))
}