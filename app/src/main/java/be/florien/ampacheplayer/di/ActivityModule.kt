package be.florien.ampacheplayer.di

import android.app.Activity
import be.florien.ampacheplayer.manager.AmpacheApi
import be.florien.ampacheplayer.manager.AmpacheConnection
import dagger.Module
import dagger.Provides

/**
 * Created by florien on 17/05/17.
 */
@Module
class ActivityModule (val activity : Activity) {

    @ActivityScope
    @Provides
    fun providesActivity() : Activity = activity

    @ActivityScope
    @Provides
    fun provideAmpacheConnection(ampacheApi: AmpacheApi, activity: Activity): AmpacheConnection = AmpacheConnection(ampacheApi, activity)

}