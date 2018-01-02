package be.florien.ampacheplayer.view

import android.app.Activity
import android.view.View
import dagger.Module
import dagger.Provides
import dagger.Reusable

/**
 * Created by florien on 1/01/18.
 */
@Module
class ActivityModule {

    @Provides
    @Reusable
    fun provideNavigator(activity: Activity): Navigator = Navigator(activity)

    @Provides
    @Reusable
    fun provideDisplayHelper(view: View): DisplayHelper = DisplayHelper(view)


}