package be.florien.anyflow.management.waveform

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.media3.common.MediaMetadata
import be.florien.anyflow.common.di.ServerScope
import be.florien.anyflow.common.ui.GlideApp
import be.florien.anyflow.common.logging.eLog
import be.florien.anyflow.common.logging.iLog
import be.florien.anyflow.tags.local.LibraryDatabase
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.FutureTarget
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

@ServerScope
class WaveFormRepository @Inject constructor(
    private val libraryDatabase: LibraryDatabase,
    @Named("serverUrl") private val serverUrl: String,
    private val context: Context
) {

    private val currentDownloads = mutableSetOf<Long>()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        eLog(throwable, "Received an exception in WaveFormRepository's scope")
    }
    private val coroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main + exceptionHandler)

    fun getComputedWaveForm(mediaId: Long, mediaType: Int): LiveData<DoubleArray> =
        if (mediaType == MediaMetadata.MEDIA_TYPE_MUSIC) {
            libraryDatabase.getSongDao().getWaveFormUpdatable(mediaId).map { it.downSamplesArray }
        } else {
            libraryDatabase.getPodcastEpisodeDao().getWaveFormUpdatable(mediaId).map { it?.downSamplesArray ?: DoubleArray(0) }
        }

    fun checkWaveForm(mediaId: Long, mediaType: Int) {
        coroutineScope.launch(Dispatchers.IO) {
            val waveFormLocal = if (mediaType == MediaMetadata.MEDIA_TYPE_MUSIC) {
                libraryDatabase
                    .getSongDao()
                    .getWaveForm(mediaId)
            } else {
                libraryDatabase
                    .getPodcastEpisodeDao()
                    .getWaveForm(mediaId)
            }
            val isWaveFormMissing =
                waveFormLocal == null || waveFormLocal.downSamplesArray.isEmpty()
            if (isWaveFormMissing && currentDownloads.add(mediaId)) {
                getWaveFormFromAmpache(mediaId, mediaType)
                currentDownloads.remove(mediaId)
            }
        }
    }

    private suspend fun getWaveFormFromAmpache(mediaId: Long, mediaType: Int) {
        val bitmap = getWaveFormImage(mediaId, mediaType)

        if (bitmap != null) {
            val array = getValuesFromBitmap(bitmap)
            val ratioArray = ratioValues(mediaId, mediaType, array)
            val computedBarList = enhanceBarsHeight(ratioArray)
            save(mediaId, mediaType, computedBarList)
        }
    }

    private suspend fun getWaveFormImage(mediaId: Long, mediaType: Int) =
        withContext(Dispatchers.IO) {
            val url = if (mediaType == MediaMetadata.MEDIA_TYPE_MUSIC) {
                "$serverUrl/waveform.php?song_id=$mediaId"
            } else {
                return@withContext null
// todo               "$serverUrl/waveform.php?podcast_episode=$mediaId"
            }
            iLog("url for waveform is $url")
            val futureTarget: FutureTarget<Bitmap> = GlideApp.with(context)
                .asBitmap()
                .load(url)
                .submit()

            try {
                val bitmap: Bitmap = futureTarget.get()
                GlideApp.with(context).clear(futureTarget)
                bitmap
            } catch (exception: GlideException) {
                exception.logRootCauses("WaveFormRepository")
                null
            }
        }

    private suspend fun getValuesFromBitmap(bitmap: Bitmap) = withContext(Dispatchers.Default) {
        val width = bitmap.width
        val height = bitmap.height
        val array = DoubleArray(width)
        val waveFormHeight = height / 2
        abscissa@ for (x in 0 until width) {
            for (y in 0 until waveFormHeight) {
                val pixel = bitmap.getPixel(x, y)
                val alpha = pixel / ALPHA_MASK
                if (alpha > ALPHA_THRESHOLD) {
                    array[x] = (waveFormHeight - y).toDouble() / waveFormHeight
                    continue@abscissa
                }
            }
        }
        array
    }

    private suspend fun ratioValues(mediaId: Long, mediaType: Int, array: DoubleArray) =
        withContext(Dispatchers.Default) {
            val duration = if (mediaType == MediaMetadata.MEDIA_TYPE_MUSIC) {
                libraryDatabase.getSongDao().getSongDuration(mediaId)
            } else {
                libraryDatabase.getPodcastEpisodeDao().getPodcastDuration(mediaId)
            }
            val durationMs = duration * 1000
            val numberOfBars = durationMs / BAR_DURATION_MS
            val ratio = array.size.toDouble() / numberOfBars
            val barList = DoubleArray(numberOfBars)
            for (index in 0 until numberOfBars) {
                val firstIndex = (index * ratio).toInt()
                val lastIndex = ((index + 1) * ratio).toInt().coerceAtMost(array.size - 1)
                barList[index] = array.slice(firstIndex..lastIndex).average()
            }
            barList
        }

    private suspend fun enhanceBarsHeight(ratioArray: DoubleArray) =
        withContext(Dispatchers.Default) {
            val maxRatio = ratioArray.maxOrNull() ?: return@withContext DoubleArray(0)
            val multiplier = 1 / maxRatio
            ratioArray.map { it * multiplier }.toDoubleArray()
        }

    private suspend fun save(mediaId: Long, mediaType: Int, computedBarList: DoubleArray) {
        withContext(Dispatchers.IO) {
            val stringify = computedBarList
                .takeIf { it.isNotEmpty() }
                ?.joinToString(separator = "|") {
                    "%.3f".format(it)
                }
            if (stringify != null) {
                if (mediaType == MediaMetadata.MEDIA_TYPE_MUSIC) {
                    libraryDatabase.getSongDao().updateWithNewWaveForm(mediaId, stringify)
                } else {
                    libraryDatabase.getPodcastEpisodeDao().updateWithNewWaveForm(mediaId, stringify)
                }
            }
        }
    }

    companion object {
        const val BAR_DURATION_MS = 500
        private const val ALPHA_MASK = 0x1000000
        private const val ALPHA_THRESHOLD = 0x1E
    }
}