package be.florien.anyflow.feature.player.services.controller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.UnrecognizedInputFormatException
import be.florien.anyflow.data.local.model.DbSongToPlay
import be.florien.anyflow.data.server.AmpacheDataSource
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.extension.postValueIfChanged
import be.florien.anyflow.feature.alarms.AlarmsSynchronizer
import be.florien.anyflow.feature.download.DownloadManager
import be.florien.anyflow.feature.player.services.queue.FiltersManager
import be.florien.anyflow.feature.player.services.queue.PlayingQueue
import be.florien.anyflow.injection.ServerScope
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Named

@androidx.media3.common.util.UnstableApi
@ServerScope
class ExoPlayerController
@Inject constructor(
    private val playingQueue: PlayingQueue,
    private val ampacheDataSource: AmpacheDataSource,
    private val filtersManager: FiltersManager,
    private val audioManager: AudioManager,
    private val downloadManager: DownloadManager,
    private val alarmsSynchronizer: AlarmsSynchronizer,
    private val context: Context,
    cache: Cache,
    @Named("authenticated") okHttpClient: OkHttpClient
) : PlayerController, Player.Listener {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        eLog(throwable, "Received an exception in ExoPlayerController's scope")
    }
    private val exoplayerScope = CoroutineScope(Dispatchers.Main + exceptionHandler)

    override val stateChangeNotifier: LiveData<PlayerController.State> = MutableLiveData()
    override val internetChangeNotifier: LiveData<Boolean> = MutableLiveData()
    override val playTimeNotifier: LiveData<Long> = MutableLiveData()

    private val mediaPlayer: ExoPlayer

    private var isReceiverRegistered: Boolean = false
    private val myNoisyAudioStreamReceiver = BecomingNoisyReceiver()

    private var shouldPlayOnFocusChange = false
    private var currentSongId = -1L
    private val audioFocusRequest: AudioFocusRequest? = getFocusRequest()

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
        val dataSourceFactory = DefaultDataSource.Factory(
            context, CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(OkHttpDataSource.Factory(okHttpClient))
                .setCacheWriteDataSinkFactory(null)// Disable writing.
        )

        mediaPlayer = ExoPlayer
            .Builder(
                context,
                DefaultRenderersFactory(context).apply { setEnableAudioOffload(true) }
            )
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            .apply {
                addListener(this@ExoPlayerController)
                //todo apply experimentalSetOffloadSchedulingEnabled(true) when in background
            }
        playingQueue.stateUpdater.observeForever { state ->
            applyState(state)
            currentSongId = state.currentSong.id
        }
        exoplayerScope.launch(Dispatchers.Default) { // todo softer update (pause ? only if different ? )
            while (true) {
                delay(10)
                withContext(Dispatchers.Main) {
                    (playTimeNotifier as MutableLiveData).value = mediaPlayer.contentPosition
                }
            }
        }
        observeNetwork()
    }

    private fun observeNetwork() {
        val networkRequest = NetworkRequest
            .Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val hasInternet =
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                (internetChangeNotifier as MutableLiveData).postValueIfChanged(hasInternet)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                (internetChangeNotifier as MutableLiveData).postValueIfChanged(false)
            }

        }

        val connectivityManager =
            context.getSystemService(ConnectivityManager::class.java) as ConnectivityManager
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val hasInternet =
            networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
        val isWifi =
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
        val isCellular =
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ?: false
        (internetChangeNotifier as MutableLiveData).postValueIfChanged(hasInternet && (isWifi || isCellular))
    }

    override fun isPlaying() = mediaPlayer.playWhenReady

    override fun isSeekable() = mediaPlayer.isCurrentMediaItemSeekable

    override fun playForAlarm() {
        exoplayerScope.launch(Dispatchers.Default) {
            alarmsSynchronizer.syncAlarms()
        }
        val connectivityManager =
            context.getSystemService(ConnectivityManager::class.java) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        if (networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true) {
            filtersManager.clearFilters()//todo download Filter as a parent (here could a good place)
            filtersManager.addFilter(
                Filter(
                    Filter.FilterType.DOWNLOADED_STATUS_IS,
                    true,
                    "",
                    emptyList()
                )
            )
            exoplayerScope.launch(Dispatchers.Default) {
                filtersManager.commitChanges()
            }
        }
        val streamMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, streamMaxVolume.div(3), 0)
        prepare()
        resume()
    }

    private fun prepare() {
        exoplayerScope.launch(Dispatchers.Main) {
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
                val focusRequest = audioFocusRequest
                    ?: throw IllegalStateException("AudiFocusRequest should have been initialized")
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
                    mediaPlayer.volume = 0.1f
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
            val request = audioFocusRequest
                ?: throw IllegalStateException("AudioFocusRequest should have been initialized")
            audioManager.abandonAudioFocusRequest(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(::onAudioFocusChange)
        }
    }

    override fun seekTo(duration: Long) {
        mediaPlayer.seekTo(duration)
    }

    // todo here is a problem: service.onDestroy is call on configuration changes
    override fun onDestroy() {
        abandonAudioFocus()
        exoplayerScope.cancel()
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

        fun resetItems() {
            exoplayerScope.launch(Dispatchers.Main) {
                val firstItem = dbSongToMediaItem(playingQueue.stateUpdater.value?.currentSong)
                val secondItem = dbSongToMediaItem(playingQueue.stateUpdater.value?.nextSong)
                mediaPlayer.setMediaItems(listOfNotNull(firstItem, secondItem))
                mediaPlayer.seekTo(0, C.TIME_UNSET)
                prepare()
            }
        }

        if (error is ExoPlaybackException && error.type == ExoPlaybackException.TYPE_SOURCE && error.sourceException is UnrecognizedInputFormatException) {
            val sourceException = error.sourceException as UnrecognizedInputFormatException
            val uri = sourceException.uri
            val songId = uri.getQueryParameter("id")?.toLongOrNull()
            if (songId != null) {
                exoplayerScope.launch(Dispatchers.IO) {
                    //todo wow, this is ugly. Intercept response ?
                    val ampacheError = ampacheDataSource.getStreamError(songId)
                    if (ampacheError.error.errorCode == 4701) {
                        resetItems()
                    }
                }
            } else if (uri.scheme?.equals("content") == true) {
                mediaPlayer.clearMediaItems()
                exoplayerScope.launch { //todo mark as faulty download / try again
                    downloadManager.removeDownload(playingQueue.stateUpdater.value?.currentSong?.id)
                }
            }
        }

        if (
            (error.cause as? HttpDataSource.InvalidResponseCodeException)?.responseCode == 403
            || (error.cause as? HttpDataSource.InvalidResponseCodeException)?.responseCode == 400
        ) {
            (stateChangeNotifier as MutableLiveData).value = PlayerController.State.RECONNECT
            exoplayerScope.launch {
                resetItems()
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
                //todo should I care ? ampacheAuthSource.resetReconnectionCount()
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
        val isNextSong = mediaPlayer.currentMediaItemIndex == 1 || nextSongByUser
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
                    dbSongToMediaItem(state.currentSong),
                    dbSongToMediaItem(state.nextSong)
                )
            )
            prepare()

            if (state.intent == PlayingQueue.PlayingQueueIntent.CONTINUE || state.intent == PlayingQueue.PlayingQueueIntent.START) {
                resume()
            }
        } else if (hasNextItemChanged) {
            mediaPlayer.removeMediaItem(1)
            if (state.nextSong != null) {
                mediaPlayer.addMediaItem(dbSongToMediaItem(state.nextSong)!!)
            }
        } else {
            if (state.intent == PlayingQueue.PlayingQueueIntent.START) resume()
            else if (state.intent == PlayingQueue.PlayingQueueIntent.PAUSE) pause()
        }
    }

    private fun dbSongToMediaItem(song: DbSongToPlay?): MediaItem? {
        if (song == null) return null
        if (!song.local.isNullOrBlank()) {
            return MediaItem.Builder().setUri(song.local).setMediaId(song.id.toString()).build()
        }
        return dbSongToPlayToUrlMediaItem(song)
    }

    private fun dbSongToPlayToUrlMediaItem(song: DbSongToPlay): MediaItem {
        val songUrl = ampacheDataSource.getSongUrl(song.id)
        return MediaItem.Builder().setUri(Uri.parse(songUrl)).setMediaId(song.id.toString()).build()
    }
}