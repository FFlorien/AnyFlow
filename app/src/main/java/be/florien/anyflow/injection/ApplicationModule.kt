package be.florien.anyflow.injection

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Build
import android.os.Environment
import be.florien.anyflow.data.local.LibraryDatabase
import be.florien.anyflow.data.user.AuthPersistence
import be.florien.anyflow.data.user.AuthPersistenceKeystore
import be.florien.anyflow.feature.alarms.AlarmActivity
import be.florien.anyflow.player.PlayerService
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

/**
 * Provide elements used through all the application state
 */
@Module
class ApplicationModule {

    companion object {
        private const val PREFERENCE_NAME = "anyflow_preferences"
    }

    @Provides
    fun providePreferences(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

    @Singleton
    @Provides
    fun provideAuthPersistence(preferences: SharedPreferences, context: Context): AuthPersistence =
        AuthPersistenceKeystore(preferences, context)

    @Singleton
    @Provides
    fun provideLibrary(context: Context): LibraryDatabase = LibraryDatabase.getInstance(context)

    @Singleton
    @Provides
    fun provideStandaloneDatabaseProvider(context: Context): StandaloneDatabaseProvider =
        StandaloneDatabaseProvider(context)

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
        val intent = Intent(context, PlayerService::class.java)
        intent.action = "ALARM"
        if (Build.VERSION.SDK_INT >= 26) {
            return PendingIntent.getForegroundService(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        return PendingIntent.getService(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @Provides
    @Named("alarm")
    fun provideAlarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmActivity::class.java)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    @Provides
    fun provideAudioManager(context: Context) =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
}