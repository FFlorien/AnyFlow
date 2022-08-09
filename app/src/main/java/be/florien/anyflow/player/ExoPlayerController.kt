package be.florien.anyflow.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import be.florien.anyflow.data.local.model.DbSongToPlay
import be.florien.anyflow.data.server.AmpacheDataSource
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.feature.alarms.AlarmsSynchronizer
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.UnrecognizedInputFormatException
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import javax.inject.Inject

class ExoPlayerController
@Inject constructor(
    private var playingQueue: PlayingQueue,
    private var ampacheDataSource: AmpacheDataSource,
    private var filtersManager: FiltersManager,
    private var audioManager: AudioManager,
    private val alarmsSynchronizer: AlarmsSynchronizer,
    private val context: Context,
    cache: Cache,
    okHttpClient: OkHttpClient
) : PlayerController, Player.Listener {

    private val dataSourceFactory: DataSource.Factory
    override val stateChangeNotifier: LiveData<PlayerController.State> = MutableLiveData()
    override val playTimeNotifier: LiveData<Long> = MutableLiveData()

    private val mediaPlayer: ExoPlayer

    private var isReceiverRegistered: Boolean = false
    private val myNoisyAudioStreamReceiver = BecomingNoisyReceiver()

    private var shouldPlayOnFocusChange = false
    private val audioFocusRequest: AudioFocusRequest?  = getFocusRequest()

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
        dataSourceFactory = DefaultDataSourceFactory(
            context, CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(OkHttpDataSource.Factory(okHttpClient))
                .setCacheWriteDataSinkFactory(null)// Disable writing.
        )

        mediaPlayer = SimpleExoPlayer
            .Builder(
                context,
                DefaultRenderersFactory(context).apply { setEnableAudioOffload(true) })
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            .apply {
                addListener(this@ExoPlayerController)
                //todo apply experimentalSetOffloadSchedulingEnabled(true) when in background
            }
        playingQueue.stateUpdater.observeForever { state ->
            applyState(state)
        }
        GlobalScope.launch(Dispatchers.Default) {
            while (true) {
                delay(10)
                withContext(Dispatchers.Main) {
                    val contentPosition = mediaPlayer.contentPosition
                    (playTimeNotifier as MutableLiveData).value = contentPosition
                }
            }
        }
    }

    override fun isPlaying() = mediaPlayer.playWhenReady

    override fun isSeekable() = mediaPlayer.isCurrentMediaItemSeekable

    override fun playForAlarm() {
        GlobalScope.launch(Dispatchers.Default) {
            alarmsSynchronizer.syncAlarms()
        }
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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
        resume()
    }

    private fun prepare() {
        GlobalScope.launch(Dispatchers.Main) {
            mediaPlayer.prepare()
        }
    }

    override fun stop() {
        abandonAudioFocus()
        mediaPlayer.stop()
    }

    override fun pause() {
        abandonAudioFocus()
        mediaPlayer.playWhenReady = false
        (stateChangeNotifier as MutableLiveData).value = PlayerController.State.PAUSE
    }

    override fun resume() {
        if (!isPlaying()) {
            val focusResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusRequest = audioFocusRequest ?: throw IllegalStateException("AudiFocusRequest should have been initialized")
                audioManager.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    ::onAudioFocusChange,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }
            when (focusResponse) {
                AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                    shouldPlayOnFocusChange = true
                }
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                    mediaPlayer.playWhenReady = true
                    (stateChangeNotifier as MutableLiveData).value = PlayerController.State.PLAY
                }
            }
        }
    }


    private fun getFocusRequest(): AudioFocusRequest? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(::onAudioFocusChange)
                .build()
        } else {
            null
        }

    private fun onAudioFocusChange(change: Int) {
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                mediaPlayer.volume = 1f
                if (!isPlaying() && shouldPlayOnFocusChange) resume()
                shouldPlayOnFocusChange = false
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    mediaPlayer.volume = 0.2f
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pause()
                shouldPlayOnFocusChange = true
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                stop()
                shouldPlayOnFocusChange = false
            }
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = audioFocusRequest ?: throw IllegalStateException("AudioFocusRequest should have been initialized")
            audioManager.abandonAudioFocusRequest(request)
        } else {
            audioManager.abandonAudioFocus(::onAudioFocusChange)
        }
    }

    override fun seekTo(duration: Long) {
        mediaPlayer.seekTo(duration)
    }

    override fun onDestroy() {
        abandonAudioFocus()
        //todo
    }

    /**
     * Listener implementation
     */

    override fun onEvents(player: Player, events: Player.Events) {
        if (events.contains(Player.EVENT_PLAYER_ERROR)) {
            val error = player.playerError
            if (error != null) {
                eLog(error, "Unhandled error while playback")
            }
        }
    }

    override fun onPlayerError(error: PlaybackException) {

        suspend fun reconnect() {
            ampacheDataSource.reconnect {
                GlobalScope.launch(Dispatchers.Main) {
                    val firstItem =
                        dbSongToPlayToMediaItem(playingQueue.stateUpdater.value?.currentSong)
                    val secondItem =
                        dbSongToPlayToMediaItem(playingQueue.stateUpdater.value?.nextSong)
                    mediaPlayer.setMediaItems(listOfNotNull(firstItem, secondItem))
                    mediaPlayer.seekTo(0, C.TIME_UNSET)
                }
                prepare()
            }
        }

        if (error is ExoPlaybackException && error.type == ExoPlaybackException.TYPE_SOURCE && error.sourceException is UnrecognizedInputFormatException) {
            val sourceException = error.sourceException as UnrecognizedInputFormatException
            val uri = sourceException.uri
            val songId = uri.getQueryParameter("id")?.toLongOrNull()
            if (songId != null) {
                GlobalScope.launch (Dispatchers.IO) {
                    val ampacheError = ampacheDataSource.getStreamError(songId)
                    if (ampacheError.error.errorCode == 4701) {
                        reconnect()
                    }
                }
            }
        }

        if (
            (error.cause as? HttpDataSource.InvalidResponseCodeException)?.responseCode == 403
            || (error.cause as? HttpDataSource.InvalidResponseCodeException)?.responseCode == 400
        ) {
            (stateChangeNotifier as MutableLiveData).value = PlayerController.State.RECONNECT
            GlobalScope.launch {
                reconnect()
            }
        } else if (
            error is ExoPlaybackException
            && error.cause is IllegalStateException
            && error.cause?.message?.contains(
                "Playback stuck buffering and not loading",
                false
            ) == true
        ) {
            val position = mediaPlayer.currentPosition
            prepare()
            seekTo(position)
            resume()
        } else {
            eLog(error, "Error while playback")
        }
    }

    @Deprecated("Deprecated in library")
    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        when (playbackState) {
            Player.STATE_ENDED -> {
                (stateChangeNotifier as MutableLiveData).value = PlayerController.State.PAUSE
            }
            Player.STATE_BUFFERING -> {
                ampacheDataSource.resetReconnectionCount()
                (stateChangeNotifier as MutableLiveData).value = PlayerController.State.BUFFER
            }
            Player.STATE_IDLE -> (stateChangeNotifier as MutableLiveData).value =
                PlayerController.State.NO_MEDIA
            Player.STATE_READY -> (stateChangeNotifier as MutableLiveData).value =
                if (playWhenReady) PlayerController.State.PLAY else PlayerController.State.PAUSE
        }

        if (playWhenReady && !isReceiverRegistered) {
            context.registerReceiver(
                myNoisyAudioStreamReceiver,
                IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            )
            isReceiverRegistered = true
        } else if (isReceiverRegistered) {
            context.unregisterReceiver(myNoisyAudioStreamReceiver)
            isReceiverRegistered = false
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
            playingQueue.listPosition += 1
        }
    }

    /**
     * PRIVATE METHODS
     */

    private fun applyState(state: PlayingQueue.PlayingQueueState) {
        val nextSongByUser =
            mediaPlayer.mediaItemCount > 0 && mediaPlayer.getMediaItemAt(1).mediaId == state.currentSong.id.toString()
        val isNextSong = mediaPlayer.currentWindowIndex == 1 || nextSongByUser
        if (isNextSong) {
            mediaPlayer.removeMediaItem(0)
        }
        val hasCurrentItemChanged =
            mediaPlayer.currentMediaItem?.mediaId != state.currentSong.id.toString()
        val hasNextItemChanged =
            (if (mediaPlayer.mediaItemCount > 1) mediaPlayer.getMediaItemAt(1).mediaId else null) != state.nextSong?.toString()
        if (hasCurrentItemChanged) {
            mediaPlayer.clearMediaItems()
            mediaPlayer.setMediaItems(
                listOfNotNull(
                    dbSongToPlayToMediaItem(state.currentSong),
                    dbSongToPlayToMediaItem(state.nextSong)
                )
            )
            prepare()

            if (state.intent == PlayingQueue.PlayingQueueIntent.CONTINUE || state.intent == PlayingQueue.PlayingQueueIntent.START) {
                resume()
            }
        } else if (hasNextItemChanged) {
            mediaPlayer.removeMediaItem(1)
            if (state.nextSong != null) {
                mediaPlayer.addMediaItem(dbSongToPlayToMediaItem(state.nextSong)!!)
            }
        } else {
            if (state.intent == PlayingQueue.PlayingQueueIntent.START) resume()
            else if (state.intent == PlayingQueue.PlayingQueueIntent.PAUSE) pause()
        }
    }

    private fun dbSongToPlayToMediaItem(song: DbSongToPlay?): MediaItem? {
        if (song == null) return null
        if (song.local != null) {
            return MediaItem.Builder().setUri(song.local).setMediaId(song.id.toString()).build()
        }
        val songUrl = ampacheDataSource.getSongUrl(song.id)
        return MediaItem.Builder().setUri(Uri.parse(songUrl)).setMediaId(song.id.toString()).build()
    }
}