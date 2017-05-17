package be.florien.ampacheplayer.di

import android.app.Activity
import dagger.Module
import dagger.Provides

/**
 * Created by florien on 17/05/17.
 */
@Module
class ActivityModule (val activity : Activity) {

    @Provides
    fun providesActivity() : Activity = activity

}