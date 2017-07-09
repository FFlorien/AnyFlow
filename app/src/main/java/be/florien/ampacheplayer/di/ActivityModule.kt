package be.florien.ampacheplayer.di

import android.app.Activity
import be.florien.ampacheplayer.manager.AmpacheApi
import be.florien.ampacheplayer.manager.AmpacheConnection
import dagger.Module
import dagger.Provides

/**
 * Module providing component related to activities
 */
@Module
class ActivityModule (val activity : Activity) {
    @ActivityScope
    @Provides
    fun providesActivity() : Activity = activity
}