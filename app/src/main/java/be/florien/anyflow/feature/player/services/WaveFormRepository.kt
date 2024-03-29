package be.florien.anyflow.feature.player.services

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import be.florien.anyflow.data.local.LibraryDatabase
import be.florien.anyflow.data.server.AmpacheDataSource
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.injection.ServerScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ServerScope
class WaveFormRepository @Inject constructor(
    private val libraryDatabase: LibraryDatabase,
    private val ampacheDataSource: AmpacheDataSource,
    private val context: Context
) {

    private val currentDownloads = mutableSetOf<Long>()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        eLog(throwable, "Received an exception in WaveFormRepository's scope")
    }
    private val coroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main + exceptionHandler)

    fun getComputedWaveForm(songId: Long): LiveData<DoubleArray> =
        libraryDatabase.getSongDao().getWaveForm(songId).map { it.downSamplesArray }

    fun checkWaveForm(songId: Long) {
        coroutineScope.launch(Dispatchers.IO) {
            val waveFormSync = libraryDatabase
                .getSongDao()
                .getWaveFormSync(songId)
            val isWaveFormMissing = waveFormSync == null || waveFormSync.downSamplesArray.isEmpty()
            if (isWaveFormMissing && currentDownloads.add(songId)) {
                getWaveFormFromAmpache(songId)
                currentDownloads.remove(songId)
            }
        }
    }

    private suspend fun getWaveFormFromAmpache(songId: Long) {
        val bitmap = getBitmap(songId)

        if (bitmap != null) {
            val array = getValuesFromBitmap(bitmap)
            val ratioArray = ratioValues(songId, array)
            val computedBarList = enhanceBarsHeight(ratioArray)
            save(songId, computedBarList)
        }
    }

    private suspend fun getBitmap(songId: Long) = withContext(Dispatchers.IO) {
        ampacheDataSource.getWaveFormImage(songId, context)
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
            val maxRatio = ratioArray.max()
            val multiplier = 1 / maxRatio
            ratioArray.map { it * multiplier }.toDoubleArray()
        }

    private suspend fun save(songId: Long, computedBarList: DoubleArray) {
        withContext(Dispatchers.IO) {
            val stringify =
                computedBarList.takeIf { it.isNotEmpty() }
                    ?.joinToString(separator = "|") { "%.3f".format(it) }
            libraryDatabase.getSongDao().updateWithNewWaveForm(songId, stringify)
        }
    }

    companion object {
        const val BAR_DURATION_MS = 500
        private const val ALPHA_MASK = 0x1000000
        private const val ALPHA_THRESHOLD = 0x1E
    }
}