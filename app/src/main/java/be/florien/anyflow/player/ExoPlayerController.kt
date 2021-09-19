package be.florien.anyflow.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
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
import kotlin.math.max
import kotlin.math.min

class ExoPlayerController
@Inject constructor(
        private var playingQueue: PlayingQueue,
        private var ampacheConnection: AmpacheConnection,
        private val context: Context,
        okHttpClient: OkHttpClient
) : PlayerController, Player.Listener {

    inner class UrlUpdateCallback : ListUpdateCallback {
        override fun onInserted(position: Int, count: Int) {
            when {
                count == mediaPlayer.mediaItemCount -> {
                    mediaPlayer.addMediaItems(urlList.map { urlToMediaItem(it) })
                }
                position == mediaPlayer.mediaItemCount -> {
                    mediaPlayer.addMediaItems(urlList.takeLast(count).map { urlToMediaItem(it) })
                }
                position == 0 -> {
                    mediaPlayer.addMediaItems(0, urlList.subList(0, count).map { urlToMediaItem(it) })
                }
                else -> mediaPlayer.addMediaItems(position, urlList.subList(position, position + count).map { urlToMediaItem(it) })
            }
        }

        override fun onRemoved(position: Int, count: Int) {
            mediaPlayer.removeMediaItems(position, position + count)
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            mediaPlayer.moveMediaItem(fromPosition, toPosition)
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            onRemoved(position, count)
            onInserted(position, count)
        }
    }

    companion object {
        private const val NO_VALUE = -3L
        private const val MEDIA_ITEM_BEFORE_CURRENT = 4
        private const val MEDIA_ITEM_AFTER_CURRENT = 10
    }

    override val stateChangeNotifier: LiveData<PlayerController.State> = MutableLiveData()
    override val playTimeNotifier: LiveData<Long> = MutableLiveData()

    private val mediaPlayer: ExoPlayer
    private var lastPosition: Int = 0
    private var lastDuration: Long = NO_VALUE
    private var urlList: List<String> = listOf()
    private var offset: Int = 0

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
        playingQueue.stateUpdater.observeForever { state ->
            val currentUrl = urlList.getOrNull(lastPosition - offset)
            val nextOffset = max(state.position - MEDIA_ITEM_BEFORE_CURRENT, 0)
            val nextUrlList = state.urls.subList(nextOffset, min(state.position + MEDIA_ITEM_AFTER_CURRENT, state.urls.size))
            val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize() = urlList.size

                override fun getNewListSize(): Int = nextUrlList.size

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                        urlList[oldItemPosition] == nextUrlList[newItemPosition]

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = areItemsTheSame(oldItemPosition, newItemPosition)

            })
            urlList = nextUrlList
            offset = nextOffset
            lastPosition = state.position
            val nextUrl = nextUrlList[state.position - nextOffset]
            diffResult.dispatchUpdatesTo(UrlUpdateCallback())
            prepare()
            if (mediaPlayer.currentMediaItem?.mediaId == nextUrl) {
                return@observeForever
            }
            val timeToSeekTo = if (currentUrl == nextUrl) C.TIME_UNSET else 0
            mediaPlayer.seekTo(state.position - offset, timeToSeekTo)
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
                ampacheConnection.reconnect {
                    GlobalScope.launch(Dispatchers.Main) {
                        mediaPlayer.setMediaItems(urlList.map { urlToMediaItem(it) })
                    }
                    prepare()
                }
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

    private fun urlToMediaItem(url: String) = MediaItem.Builder().setUri(Uri.parse(ampacheConnection.getSongUrl(url))).setMediaId(url).build()
}