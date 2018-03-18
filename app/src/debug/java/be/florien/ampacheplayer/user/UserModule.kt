package be.florien.ampacheplayer.user

import android.content.Context
import be.florien.ampacheplayer.api.AmpacheApi
import be.florien.ampacheplayer.di.UserScope
import be.florien.ampacheplayer.player.AudioQueue
import be.florien.ampacheplayer.player.ExoPlayerController
import be.florien.ampacheplayer.player.PlayerController
import com.facebook.stetho.okhttp3.StethoInterceptor
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory

/**
 * todo
 */
@Module
class UserModule {

    @Provides
    @UserScope
    fun providePlayerController(context: Context, audioQueueManager: AudioQueue): PlayerController = ExoPlayerController(context, audioQueueManager)
}