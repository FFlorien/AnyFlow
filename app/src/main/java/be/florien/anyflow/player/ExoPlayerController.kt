package be.florien.anyflow.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import be.florien.anyflow.data.AmpacheDownloadService
import be.florien.anyflow.data.server.AmpacheConnection
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.extension.iLog
import be.florien.anyflow.feature.alarms.AlarmsSynchronizer
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource
import com.google.android.exoplayer2.offline.DownloadHelper
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

class ExoPlayerController
@Inject constructor(
    private var playingQueue: PlayingQueue,
    private var ampacheConnection: AmpacheConnection,
    private var filtersManager: FiltersManager,
    private var audioManager: AudioManager,
    private val alarmsSynchronizer: AlarmsSynchronizer,
    private val context: Context,
    cache: Cache,
    okHttpClient: OkHttpClient
) : PlayerController, Player.Listener {

    inner class UrlUpdateCallback : ListUpdateCallback {
        override fun onInserted(position: Int, count: Int) {
            when {
                count == mediaPlayer.mediaItemCount -> {
                    mediaPlayer.addMediaItems(idList.map { idToMediaItem(it) })
                }
                position == mediaPlayer.mediaItemCount -> {
                    mediaPlayer.addMediaItems(idList.takeLast(count).map { idToMediaItem(it) })
                }
                position == 0 -> {
                    mediaPlayer.addMediaItems(0, idList.subList(0, count).map { idToMediaItem(it) })
                }
                else -> mediaPlayer.addMediaItems(position, idList.subList(position, position + count).map { idToMediaItem(it) })
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
        private const val MEDIA_ITEM_BEFORE_CURRENT = 1
        private const val MEDIA_ITEM_AFTER_CURRENT = 2
    }

    private val dataSourceFactory: DataSource.Factory
    override val stateChangeNotifier: LiveData<PlayerController.State> = MutableLiveData()
    override val playTimeNotifier: LiveData<Long> = MutableLiveData()

    private val mediaPlayer: ExoPlayer
    private var lastPosition: Int = 0
    private var lastDuration: Long = NO_VALUE
    private var idList: List<Long> = listOf()
    private var lastOffset: Int = 0

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
        dataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(OkHttpDataSource.Factory(okHttpClient))
            .setCacheWriteDataSinkFactory(null) // Disable writing.

        mediaPlayer = SimpleExoPlayer
            .Builder(context, DefaultRenderersFactory(context).apply { setEnableAudioOffload(true) })
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            .apply {
                addListener(this@ExoPlayerController)
                //todo apply experimentalSetOffloadSchedulingEnabled(true) when in background
            }
        playingQueue.stateUpdater.observeForever { state ->
            val currentId = idList.getOrNull(lastPosition - lastOffset)
            val nextOffset = max(state.position - MEDIA_ITEM_BEFORE_CURRENT, 0)
            val nextIdList = state.ids.slice(nextOffset until min(state.position + MEDIA_ITEM_AFTER_CURRENT + 1, state.ids.size))
            val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize() = idList.size

                override fun getNewListSize(): Int = nextIdList.size

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                    idList[oldItemPosition] == nextIdList[newItemPosition]

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = areItemsTheSame(oldItemPosition, newItemPosition)

            })
            idList = nextIdList
            lastOffset = nextOffset
            lastPosition = state.position
            diffResult.dispatchUpdatesTo(UrlUpdateCallback())
            if (nextIdList.isNotEmpty()) { //todo UI Handling of this
                prepare()
                val nextId = nextIdList[state.position - nextOffset]
                if (mediaPlayer.currentMediaItem?.mediaId == nextId.toString()) {
                    return@observeForever
                }
                val timeToSeekTo = if (currentId == nextId) C.TIME_UNSET else 0
                mediaPlayer.seekTo(state.position - offset, timeToSeekTo)
            }
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

    override fun playForAlarm() {
        GlobalScope.launch(Dispatchers.Default) {
            alarmsSynchronizer.syncAlarms()
        }
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        if (activeNetworkInfo == null || !activeNetworkInfo.isConnected) {
            filtersManager.clearFilters()
            filtersManager.addFilter(Filter.DownloadedStatusIs(true))
            GlobalScope.launch(Dispatchers.Default) {
                filtersManager.commitChanges()
            }
        }
        val streamMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, streamMaxVolume.div(3), 0)
        prepare()
        play()
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

    override fun download(id: Long) {
        downloadMedia(idToMediaItem(id))
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
        if ((error.cause as? HttpDataSource.InvalidResponseCodeException)?.responseCode == 403 || (error.cause as? HttpDataSource.InvalidResponseCodeException)?.responseCode == 400) {
            (stateChangeNotifier as MutableLiveData).value = PlayerController.State.RECONNECT
            GlobalScope.launch {
                ampacheConnection.reconnect {
                    GlobalScope.launch(Dispatchers.Main) {
                        mediaPlayer.setMediaItems(idList.map { idToMediaItem(it) })
                        mediaPlayer.seekTo(lastPosition - lastOffset, C.TIME_UNSET)
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

    private fun idToMediaItem(id: Long): MediaItem {
        val songUrl = ampacheConnection.getSongUrl(id)
        return MediaItem.Builder().setUri(Uri.parse(songUrl)).setMediaId(id.toString()).build()
    }

    private fun downloadMedia(media: MediaItem) {
        val helper = DownloadHelper.forMediaItem(context, media)
        helper.prepare(object : DownloadHelper.Callback {
            override fun onPrepared(helper: DownloadHelper) {
                val json = JSONObject()
                json.put("title", media.mediaMetadata.title)
                json.put("artist", media.mediaMetadata.artist)
                val download = helper.getDownloadRequest(media.mediaId, Util.getUtf8Bytes(json.toString()))
                //sending the request to the download service
                DownloadService.sendAddDownload(context, AmpacheDownloadService::class.java, download, true)
            }

            override fun onPrepareError(helper: DownloadHelper, e: IOException) {
                Timber.e(e)
            }
        })

    }
}