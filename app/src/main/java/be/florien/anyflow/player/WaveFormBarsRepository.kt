package be.florien.anyflow.player

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import be.florien.anyflow.data.local.LibraryDatabase
import be.florien.anyflow.data.server.AmpacheDataSource
import be.florien.anyflow.extension.eLog
import kotlinx.coroutines.*

class WaveFormBarsRepository(
    private val libraryDatabase: LibraryDatabase,
    private val ampacheDataSource: AmpacheDataSource,
    private val context: Context
) {

    //todo clean this after a while ?
    private val dataMap: HashMap<Long, WaveFormBars> = hashMapOf()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        eLog(throwable, "Received an exception in WaveFormBarsRepository's scope")
    }
    private val coroutineScope = CoroutineScope(Dispatchers.Main + exceptionHandler)

    fun getComputedBars(songId: Long): LiveData<DoubleArray> =
        getBars(songId).computedLiveData

    private fun getBars(songId: Long): WaveFormBars {
        val dataFromMap = dataMap[songId]
        return if (dataFromMap == null) {
            val data = WaveFormBars(songId)
            dataMap[songId] = data
            data
        } else {
            dataFromMap
        }
    }

    inner class WaveFormBars(val songId: Long) {
        private lateinit var computedBarList: DoubleArray
        val computedLiveData: LiveData<DoubleArray> = MutableLiveData()

        init {
            coroutineScope.launch {
                fetchDataLocally()

                if (computedBarList.isEmpty()) {
                    getWaveFormFromAmpache()
                }
            }
        }

        private suspend fun fetchDataLocally() = withContext(Dispatchers.IO) {
            computedBarList = libraryDatabase.getBars(songId)
            updateLiveData()
        }

        private suspend fun getWaveFormFromAmpache() {
            val bitmap = getBitmap()
            val array = getValuesFromBitmap(bitmap)
            val ratioArray = ratioValues(array)
            computedBarList = enhanceBarsHeight(ratioArray)
            save()
            updateLiveData()
        }

        private suspend fun getBitmap() = withContext(Dispatchers.IO) {
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

        private suspend fun ratioValues(array: DoubleArray) = withContext(Dispatchers.Default) {
            val duration = libraryDatabase.getSongDuration(songId)
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

        private suspend fun enhanceBarsHeight(ratioArray: DoubleArray)  = withContext(Dispatchers.Default) {
            val maxRatio = ratioArray.max()
            val multiplier = 1 / maxRatio
            ratioArray.map { it * multiplier }.toDoubleArray()
        }

        suspend fun save() {
            withContext(Dispatchers.IO) {
                libraryDatabase.updateBars(songId, computedBarList)
            }
        }

        private suspend fun updateLiveData() {
            withContext(Dispatchers.Main) {
                (computedLiveData as MutableLiveData).value = computedBarList
            }
        }
    }

    companion object {
        const val BAR_DURATION_MS = 500
        private const val ALPHA_MASK = 0x1000000
        private const val ALPHA_THRESHOLD = 0x40
    }
}