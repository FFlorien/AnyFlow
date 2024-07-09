package be.florien.anyflow.tags.local

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Named

class PodcastPersistence @Inject constructor(@Named("podcasts") private val preferences: SharedPreferences) {

    fun savePodcastPosition(podcastId: Long, position: Long) {
        preferences.edit().putLong(podcastId.toString(), position).apply()
    }

    fun getPodcastPosition(podcastId: Long): Long = preferences.getLong(podcastId.toString(), 0)

}