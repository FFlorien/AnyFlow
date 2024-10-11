package be.florien.anyflow.injection

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import be.florien.anyflow.R
import be.florien.anyflow.architecture.di.ServerScope
import be.florien.anyflow.common.navigation.MainScreenSection
import be.florien.anyflow.feature.auth.domain.net.AuthenticationInterceptor
import be.florien.anyflow.feature.filter.current.ui.CurrentFilterFragment
import be.florien.anyflow.feature.library.podcast.ui.info.LibraryPodcastInfoFragment
import be.florien.anyflow.feature.library.podcast.ui.info.LibraryPodcastInfoViewModel
import be.florien.anyflow.feature.library.tags.ui.info.LibraryTagsInfoFragment
import be.florien.anyflow.feature.player.ui.PlayerActivity
import be.florien.anyflow.feature.songlist.ui.SongListFragment
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Named

/**
 * Module for dependencies available only when a user is logged in.
 */
@Module
class ConnectedModule {

    @ServerScope
    @Provides
    @Named("authenticated")
    fun provideDataOkHttp(authenticationInterceptor: AuthenticationInterceptor): OkHttpClient =
        OkHttpClient
            .Builder()
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .connectTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(authenticationInterceptor)
            .build()

    @ServerScope
    @Provides
    @Named("glide")
    fun provideGlideOkHttp(authenticationInterceptor: AuthenticationInterceptor): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(authenticationInterceptor)
            .callTimeout(60, TimeUnit.SECONDS)//it may need some time to generate the waveform image
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

    @Provides
    @Named("playerActivity")
    @ServerScope
    fun providePlayerActivityIntent(context: Context) = Intent(context, PlayerActivity::class.java)

    @Provides
    @ServerScope
    fun provideMainScreenSection(): List<MainScreenSection> = listOf(
        object: MainScreenSection {
            override val isFirstSection: Boolean = false
            override val menuId: Int = R.id.menu_library
            override val tag: String = LibraryTagsInfoFragment::class.java.simpleName

            override fun createFragment(): Fragment = LibraryTagsInfoFragment()
        },
        object: MainScreenSection {
            override val isFirstSection: Boolean = false
            override val menuId: Int = R.id.menu_podcast
            override val tag: String = LibraryPodcastInfoFragment::class.java.simpleName

            override fun createFragment(): Fragment = LibraryPodcastInfoFragment()
        },
        object: MainScreenSection {
            override val isFirstSection: Boolean = true
            override val menuId: Int = R.id.menu_song_list
            override val tag: String = SongListFragment::class.java.simpleName

            override fun createFragment(): Fragment = SongListFragment()
        },
        object: MainScreenSection {
            override val isFirstSection: Boolean = false
            override val menuId: Int = R.id.menu_filters
            override val tag: String = CurrentFilterFragment::class.java.simpleName

            override fun createFragment(): Fragment = CurrentFilterFragment()
        },
    )
}