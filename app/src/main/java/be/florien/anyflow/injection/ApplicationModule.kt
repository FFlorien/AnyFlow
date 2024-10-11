package be.florien.anyflow.injection

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Environment
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import be.florien.anyflow.feature.auth.domain.persistence.AuthPersistence
import be.florien.anyflow.feature.auth.domain.persistence.AuthPersistenceKeystore
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import javax.inject.Named
import javax.inject.Singleton

/**
 * Provide elements used through all the application state
 */
@Module
class ApplicationModule {

    companion object {
        private const val PREFERENCE_NAME = "anyflow_preferences"
        private const val PODCASTS_NAME = "anyflow_podcasts"
    }

    @Provides
    @Named("preferences")
    fun providePreferences(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

    @Provides
    @Named("podcasts")
    fun providePodcastsData(context: Context): SharedPreferences =
        context.getSharedPreferences(PODCASTS_NAME, Context.MODE_PRIVATE)

    @Singleton
    @Provides
    fun provideAuthPersistence(
        @Named("preferences") preferences: SharedPreferences,
        context: Context
    ): AuthPersistence =
        AuthPersistenceKeystore(preferences, context)

    @SuppressLint("UnsafeOptInUsageError")
    @Singleton
    @Provides
    fun provideStandaloneDatabaseProvider(context: Context): StandaloneDatabaseProvider =
        StandaloneDatabaseProvider(context)

    @SuppressLint("UnsafeOptInUsageError")
    @Singleton
    @Provides
    fun provideCache(context: Context, dbProvider: StandaloneDatabaseProvider): Cache = SimpleCache(
        context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.noBackupFilesDir,
        NoOpCacheEvictor(),
        dbProvider
    )

    @Provides
    fun provideAlarmManager(context: Context): AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @Provides
    @Named("player")
    fun providePlayerPendingIntent(context: Context): PendingIntent {
        val alarmIntent = Intent(context, be.florien.anyflow.feature.alarm.ui.AlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            0,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @Provides
    @Named("alarm")
    fun provideAlarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, be.florien.anyflow.feature.alarm.ui.AlarmActivity::class.java)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    @Provides
    fun provideAudioManager(context: Context) =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @Provides
    @Named("nonAuthenticated")
    fun provideAuthOkHttp(): OkHttpClient =
        OkHttpClient.Builder().build()
}