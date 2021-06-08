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
import be.florien.anyflow.data.view.Song
import be.florien.anyflow.extension.iLog
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.HttpDataSource
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import javax.inject.Inject

class ExoPlayerController
@Inject constructor(
    private var playingQueue: PlayingQueue,
    private var ampacheConnection: AmpacheConnection,
    private val context: Context,
    okHttpClient: OkHttpClient
) : PlayerController, Player.Listener {

    override val stateChangeNotifier: LiveData<PlayerController.State> = MutableLiveData()

    companion object {
        private const val NO_VALUE = -3L
    }

    override val playTimeNotifier: LiveData<Long> = MutableLiveData()

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

    /**
     * Constructor
     */

    init {
        mediaPlayer = SimpleExoPlayer.Builder(context).build().apply {
            addListener(this@ExoPlayerController)
        }
        val bandwidthMeter = DefaultBandwidthMeter.Builder(context).build()
        dataSourceFactory = DefaultDataSourceFactory(
            context,
            bandwidthMeter,
            OkHttpDataSource.Factory(okHttpClient)
        )
        playingQueue.currentSong.observeForever { song ->
            this@ExoPlayerController.iLog("New song is $song")
            song?.let {
                prepare(it)
                resume()
            }
        }
        GlobalScope.launch(Dispatchers.Default) {
            while (true) {
                delay(10)
                withContext(Dispatchers.Main) {
                    val contentPosition = mediaPlayer.contentPosition
                    (playTimeNotifier as MutableLiveData).value = contentPosition
                    lastPosition = contentPosition
                }
            }
        }
    }

    override fun isPlaying() = mediaPlayer.playWhenReady

    override fun play() {
        lastPosition = NO_VALUE
        resume()
    }

    private fun prepare(song: Song) {
        GlobalScope.launch(Dispatchers.Main) {
            mediaPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(ampacheConnection.getSongUrl(song.url))))
            mediaPlayer.prepare()
        }
    }

    override fun stop() {
        mediaPlayer.stop()
        lastPosition = NO_VALUE
    }

    override fun pause() {
        mediaPlayer.playWhenReady = false
        (stateChangeNotifier as MutableLiveData).value = PlayerController.State.PAUSE
    }

    override fun resume() {
        if (lastPosition == NO_VALUE) {
            seekTo(0)
        } else {
            seekTo(lastPosition)
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

    override fun onSeekProcessed() {
        iLog("onSeekProcessed")
    }

    override fun onEvents(player: Player, events: Player.Events) {
        if (events.contains(Player.EVENT_PLAYER_ERROR)) {
            val error = player.playerError
            if (error != null) {
//todo
            }
        }
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        iLog(error, "Error while playback")
        if ((error.cause as? HttpDataSource.InvalidResponseCodeException)?.responseCode == 403) {
            (stateChangeNotifier as MutableLiveData).value = PlayerController.State.RECONNECT
            GlobalScope.launch {
                ampacheConnection.reconnect { playingQueue.currentSong.value?.let { prepare(it) } }
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

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        when (playbackState) {
            Player.STATE_ENDED -> {
                playingQueue.listPosition += 1
                lastPosition = 0
            }
            Player.STATE_BUFFERING -> {
                ampacheConnection.resetReconnectionCount()
                (stateChangeNotifier as MutableLiveData).value = PlayerController.State.BUFFER
            }
            Player.STATE_IDLE -> (stateChangeNotifier as MutableLiveData).value =
                PlayerController.State.NO_MEDIA
            Player.STATE_READY -> (stateChangeNotifier as MutableLiveData).value =
                if (playWhenReady) PlayerController.State.PLAY else PlayerController.State.PAUSE
        }

        if (playWhenReady && !isReceiverRegistered) {
            context.registerReceiver(myNoisyAudioStreamReceiver, intentFilter)
        } else if (isReceiverRegistered) {
            context.unregisterReceiver(myNoisyAudioStreamReceiver)
        }
    }
}