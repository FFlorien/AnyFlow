package be.florien.anyflow.injection

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Build
import android.os.Environment
import androidx.lifecycle.LiveData
import be.florien.anyflow.AnyFlowApp
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.local.LibraryDatabase
import be.florien.anyflow.data.server.AmpacheDataSource
import be.florien.anyflow.data.user.AuthPersistence
import be.florien.anyflow.data.user.AuthPersistenceKeystore
import be.florien.anyflow.feature.alarms.AlarmActivity
import be.florien.anyflow.feature.alarms.AlarmsSynchronizer
import be.florien.anyflow.feature.player.library.LibraryActions
import be.florien.anyflow.feature.player.library.LibraryActionsHelper
import be.florien.anyflow.player.FiltersManager
import be.florien.anyflow.player.PlayerService
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import java.util.concurrent.Executor
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

    @Singleton
    @Provides
    fun providePreferences(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

    @Singleton
    @Provides
    fun provideOkHttp(): OkHttpClient =
        OkHttpClient.Builder().addNetworkInterceptor(StethoInterceptor()).build()

    @Singleton
    @Provides
    fun provideAuthPersistence(preferences: SharedPreferences, context: Context): AuthPersistence =
        AuthPersistenceKeystore(preferences, context)

    @Singleton
    @Provides
    fun provideAmpacheConnection(
        authPersistence: AuthPersistence,
        context: Context,
        sharedPreferences: SharedPreferences
    ): AmpacheDataSource =
        AmpacheDataSource(
            authPersistence,
            (context.applicationContext as AnyFlowApp),
            sharedPreferences
        )

    @Provides
    fun provideAmpacheConnectionStatus(connection: AmpacheDataSource): LiveData<AmpacheDataSource.ConnectionStatus> =
        connection.connectionStatusUpdater

    @Singleton
    @Provides
    fun provideLibrary(context: Context): LibraryDatabase = LibraryDatabase.getInstance(context)

    @Singleton
    @Provides
    fun provideCacheDataBaseProvider(context: Context): ExoDatabaseProvider =
        ExoDatabaseProvider(context)

    @Singleton
    @Provides
    fun provideCache(context: Context, dbProvider: ExoDatabaseProvider): Cache = SimpleCache(
        context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.noBackupFilesDir,
        NoOpCacheEvictor(),
        dbProvider
    )

    @Singleton
    @Provides
    fun provideDownloadManager(
        context: Context,
        databaseProvider: ExoDatabaseProvider,
        cache: Cache
    ): DownloadManager {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
        val downloadExecutor = Executor { obj: Runnable -> obj.run() }
        return DownloadManager(
            context,
            databaseProvider,
            cache,
            dataSourceFactory,
            downloadExecutor
        )
    }

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
    fun provideAlarmsSynchronizer(
        alarmManager: AlarmManager,
        dataRepository: DataRepository,
        @Named("player") playerIntent: PendingIntent,
        @Named("alarm") alarmIntent: PendingIntent
    ): AlarmsSynchronizer =
        AlarmsSynchronizer(alarmManager, dataRepository, alarmIntent, playerIntent)

    @Provides
    fun provideAudioManager(context: Context) =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @Provides
    fun provideFiltersScreen(filtersManager: FiltersManager): LibraryActions = LibraryActionsHelper(filtersManager)
}