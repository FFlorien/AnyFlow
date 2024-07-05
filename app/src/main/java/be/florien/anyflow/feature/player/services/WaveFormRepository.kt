package be.florien.anyflow.feature.player.services

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import be.florien.anyflow.data.local.LibraryDatabase
import be.florien.anyflow.data.server.di.ServerScope
import be.florien.anyflow.extension.GlideApp
import be.florien.anyflow.logging.eLog
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

    fun getComputedWaveForm(songId: Long): LiveData<DoubleArray> =
        libraryDatabase.getSongDao().getWaveFormUpdatable(songId).map { it.downSamplesArray }

    fun checkWaveForm(songId: Long) {
        coroutineScope.launch(Dispatchers.IO) {
            val waveFormLocal = libraryDatabase
                .getSongDao()
                .getWaveForm(songId)
            val isWaveFormMissing =
                waveFormLocal == null || waveFormLocal.downSamplesArray.isEmpty()
            if (isWaveFormMissing && currentDownloads.add(songId)) {
                getWaveFormFromAmpache(songId)
                currentDownloads.remove(songId)
            }
        }
    }

    private suspend fun getWaveFormFromAmpache(songId: Long) {
        val bitmap = getWaveFormImage(songId)

        if (bitmap != null) {
            val array = getValuesFromBitmap(bitmap)
            val ratioArray = ratioValues(songId, array)
            val computedBarList = enhanceBarsHeight(ratioArray)
            save(songId, computedBarList)
        }
    }

    private suspend fun getWaveFormImage(songId: Long) = withContext(Dispatchers.IO) {
        val url = "$serverUrl/waveform.php?song_id=$songId"
        val futureTarget: FutureTarget<Bitmap> = GlideApp.with(context)
            .asBitmap()
            .load(url)
            .submit()

        val bitmap: Bitmap = futureTarget.get()
        GlideApp.with(context).clear(futureTarget)
        bitmap
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

    private suspend fun ratioValues(songId: Long, array: DoubleArray) =
        withContext(Dispatchers.Default) {
            val duration = libraryDatabase.getSongDao().getSongDuration(songId)
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

    private suspend fun save(songId: Long, computedBarList: DoubleArray) {
        withContext(Dispatchers.IO) {
            val stringify = computedBarList
                .takeIf { it.isNotEmpty() }
                ?.joinToString(separator = "|") {
                    "%.3f".format(it)
                }
            if (stringify != null) {
                libraryDatabase.getSongDao().updateWithNewWaveForm(songId, stringify)
            }
        }
    }

    companion object {
        const val BAR_DURATION_MS = 500
        private const val ALPHA_MASK = 0x1000000
        private const val ALPHA_THRESHOLD = 0x1E
    }
}