package be.florien.anyflow.management.podcast

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Named
import androidx.core.content.edit

class PodcastPersistence @Inject constructor(@param:Named("podcasts") private val preferences: SharedPreferences) {

    fun savePodcastPosition(podcastId: Long, position: Long) {
        preferences.edit { putLong(podcastId.toString(), position) }
    }

    fun getPodcastPosition(podcastId: Long): Long = preferences.getLong(podcastId.toString(), 0)

}