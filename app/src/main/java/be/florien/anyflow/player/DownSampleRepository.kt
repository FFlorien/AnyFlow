package be.florien.anyflow.player

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import be.florien.anyflow.data.local.LibraryDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext

class DownSampleRepository(private val libraryDatabase: LibraryDatabase) {

    private val dataMap: HashMap<Long, DownSampleData> = hashMapOf()
    private var currentSongId: Long = -1L

    private val coroutineScope = CoroutineScope(EmptyCoroutineContext)

    fun getComputedDownSamples(songId: Long): LiveData<IntArray> =
        getDownSamples(songId).computedLiveData

    fun addDownSample(songId: Long, sample: Double, positionUs: Long) {
        if (songId != currentSongId) {
            dataMap[currentSongId]?.saveDownSamples()
        }

        getDownSamples(songId).addDownSample(sample, positionUs)
    }

    //todo method to let exoplayer know if it should downsample

    private fun getDownSamples(songId: Long): DownSampleData {
        val dataFromMap = dataMap[songId]
        return if (dataFromMap == null) {
            val data = DownSampleData(songId)
            dataMap[songId] = data
            data
        } else {
            dataFromMap
        }
    }

    inner class DownSampleData(val songId: Long) {
        private var currentPosition: Long = -1
        private val rawSampleList: MutableList<Double> = mutableListOf()
        private var computedSampleList: IntArray = IntArray(100) { 0 }
        val computedLiveData: LiveData<IntArray> = MutableLiveData()

        init {
            coroutineScope.launch { initIntArray() }
        }

        //todo get image from <ampacheserver>/waveform?songId=123456, get the higher non-alpha pixel at each row and put the data in dirtySamples
        //todo get clean if we have that, or dirty if not, from libraryDatabase

        fun addDownSample(sample: Double, positionUs: Long) {
            val inDownSampleDuration = positionUs.fromUsToMs.inDownSampleDuration
            if (inDownSampleDuration != currentPosition) {
                setDownSample()
                currentPosition = inDownSampleDuration
                rawSampleList.clear()
            }

            rawSampleList.add(sample)
        }

        fun saveDownSamples() {
            setDownSample()
            coroutineScope.launch {
                libraryDatabase.updateDownSamples(songId, computedSampleList)
            }
        }

        private fun setDownSample() {
            val element = (rawSampleList.average().times(10000)).toInt()
            val position = (currentPosition / DOWN_SAMPLE_DURATION_MS).toInt()
            computedSampleList[position] = element
            coroutineScope.launch(Dispatchers.Main) {
                (computedLiveData as MutableLiveData).value = computedSampleList
            }
        }

        private suspend fun initIntArray() {
            val duration = libraryDatabase.getSongDuration(songId)
            val previousList = computedSampleList
            val size = ((duration * 1000) / DOWN_SAMPLE_DURATION_MS) + 2
            computedSampleList = IntArray(size) { 0 }
            previousList.forEachIndexed { index, value ->
                if (index < size) computedSampleList[index] = value
            }
        }

        private val Long.inDownSampleDuration: Long
            get() = this - (this % DOWN_SAMPLE_DURATION_MS)

        private val Long.fromUsToMs: Long
            get() = this / 1000
    }

    companion object {
        const val DOWN_SAMPLE_DURATION_MS = 500
    }
}