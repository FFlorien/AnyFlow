package be.florien.ampacheplayer.user

import be.florien.ampacheplayer.api.AmpacheApi
import be.florien.ampacheplayer.di.UserScope
import dagger.Module
import dagger.Provides
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
    fun provideAmpacheApi(url: String): AmpacheApi = Retrofit
            .Builder()
            .baseUrl(url)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(SimpleXmlConverterFactory.create())
            .build()
            .create(AmpacheApi::class.java)
}