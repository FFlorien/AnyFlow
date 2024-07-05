package be.florien.anyflow.data

import be.florien.anyflow.utils.TimeOperations
import java.util.*

class TestingTimeUpdater: TimeOperations.CurrentTimeUpdater {

    private val recordedChanges: MutableMap<Int, Int> = mutableMapOf()

    override fun getCurrentTimeUpdated(current: Calendar): Calendar {
        for (entry in recordedChanges.entries) {
            current.add(entry.key, entry.value)
        }
        return current
    }

    fun addIncrement(key: Int, increment: Int) {
        recordedChanges[key] = increment + (recordedChanges[key] ?: 0)
    }

    fun clearIncrement(key: Int) {
        recordedChanges.remove(key)
    }

    fun clearAll() {
        recordedChanges.clear()
    }
}