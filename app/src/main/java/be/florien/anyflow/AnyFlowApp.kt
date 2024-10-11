package be.florien.anyflow

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.multidex.MultiDexApplication
import be.florien.anyflow.common.navigation.UnauthenticatedNavigation
import be.florien.anyflow.common.ui.di.GlideModuleInjector
import be.florien.anyflow.common.ui.di.GlideModuleInjectorContainer
import be.florien.anyflow.feature.alarm.ui.di.AlarmActivityComponent
import be.florien.anyflow.feature.alarm.ui.di.AlarmActivityComponentCreator
import be.florien.anyflow.feature.auth.domain.persistence.AuthPersistence
import be.florien.anyflow.feature.auth.domain.repository.AuthRepository
import be.florien.anyflow.feature.player.service.di.PlayerServiceComponent
import be.florien.anyflow.feature.player.service.di.PlayerServiceComponentCreator
import be.florien.anyflow.feature.player.ui.di.PlayerActivityComponent
import be.florien.anyflow.feature.player.ui.di.PlayerActivityComponentCreator
import be.florien.anyflow.feature.playlist.di.PlaylistActivityComponentCreator
import be.florien.anyflow.feature.playlist.di.PlaylistComponent
import be.florien.anyflow.feature.shortcut.ui.di.ShortcutActivityComponent
import be.florien.anyflow.feature.shortcut.ui.di.ShortcutActivityComponentCreator
import be.florien.anyflow.feature.sync.service.SyncService
import be.florien.anyflow.feature.sync.service.di.SyncServiceComponent
import be.florien.anyflow.feature.sync.service.di.SyncServiceComponentCreator
import be.florien.anyflow.injection.ApplicationComponent
import be.florien.anyflow.injection.DaggerApplicationComponent
import be.florien.anyflow.injection.ServerComponent
import be.florien.anyflow.logging.eLog
import be.florien.anyflow.logging.plantTimber
import be.florien.anyflow.ui.di.ServerVmInjector
import be.florien.anyflow.ui.di.ServerVmInjectorContainer
import be.florien.anyflow.ui.di.UserVmInjector
import be.florien.anyflow.ui.di.UserVmInjectorContainer
import be.florien.anyflow.ui.server.ServerActivity
import be.florien.anyflow.utils.startActivity
import javax.inject.Inject


/**
 * Application class used for initialization of many libraries
 */
@SuppressLint("Registered")
open class AnyFlowApp : MultiDexApplication(),
    PlayerServiceComponentCreator,
    UserVmInjectorContainer,
    ServerVmInjectorContainer,
    UnauthenticatedNavigation,
    PlayerActivityComponentCreator,
    PlaylistActivityComponentCreator,
    GlideModuleInjectorContainer,
    ShortcutActivityComponentCreator,
    AlarmActivityComponentCreator,
    SyncServiceComponentCreator {
    lateinit var applicationComponent: ApplicationComponent
        protected set
    override val serverVmInjector: ServerVmInjector
        get() = applicationComponent
    var serverComponent: ServerComponent? = null
    override val userVmInjector: UserVmInjector?
        get() = serverComponent

    override val glideModuleInjector: GlideModuleInjector?
        get() = serverComponent

    @Inject
    lateinit var authPersistence: AuthPersistence

    override fun onCreate() {
        super.onCreate()
        plantTimber()
        initApplicationComponent()
        initServerComponentIfReady()
        createNotificationChannels()
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            this@AnyFlowApp.eLog(e, "Unexpected error")
        }
    }

    protected open fun initApplicationComponent() {
        applicationComponent = DaggerApplicationComponent
            .builder()
            .application(this)
            .build()
        applicationComponent.inject(this)
    }

    private fun initServerComponentIfReady() {
        val serverUrl = authPersistence.serverUrl
        if (serverUrl.hasSecret()) {
            createServerComponent(serverUrl.secret)
        }
    }

    override suspend fun createServerComponentIfServerValid(serverUrl: String): Boolean {
        createServerComponent(serverUrl)

        val validator = ServerValidator()
        serverComponent?.inject(validator)

        return if (validator.isServerValid()) {
            true
        } else {
            serverComponent = null
            false
        }
    }

    private fun createServerComponent(serverUrl: String) {
        serverComponent = applicationComponent
            .serverComponentBuilder()
            .ampacheUrl(serverUrl)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val updateChannel = getUpdateChannel()
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(updateChannel)
        }
    }

    private fun getUpdateChannel(): NotificationChannel {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SyncService.UPDATE_SESSION_NAME,
                "Update",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "It update your music database"
            return channel
        } else {
            throw UnsupportedOperationException("This method shouldn't be called from this api")
        }
    }

    override fun createPlaylistComponent(): PlaylistComponent? =
        serverComponent?.playlistComponentBuilder()?.build()

    override fun createPlayerServiceComponent(): PlayerServiceComponent? =
        serverComponent?.playerServiceComponentBuilder()?.build()

    class ServerValidator {
        @Inject
        lateinit var authRepository: AuthRepository

        suspend fun isServerValid() = authRepository.ping()
    }

    override fun createShortcutActivityComponent(): ShortcutActivityComponent? =
        serverComponent?.shortcutsComponentBuilder()?.build()

    override fun createAlarmActivityComponent(): AlarmActivityComponent? =
        serverComponent?.alarmComponentBuilder()?.build()

    override fun createSyncServiceComponent(): SyncServiceComponent? =
        serverComponent?.syncComponentBuilder()?.build()

    override fun createPlayerActivityComponent(): PlayerActivityComponent? =
        serverComponent?.playerComponentBuilder()?.build()

    override fun isUserConnected(): Boolean =
        serverComponent != null

    override fun goToAuthentication(activity: Activity) {
        activity.startActivity(ServerActivity::class)
        activity.finish()
    }
}