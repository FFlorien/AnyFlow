package be.florien.anyflow.injection

import android.app.Application
import android.content.Context
import be.florien.anyflow.NavigatorImpl
import be.florien.anyflow.common.ui.navigation.Navigator
import dagger.Binds
import dagger.Module

/**
 * Created by florien on 1/01/18.
 */
@Module
abstract class ApplicationWideModule {

    @Binds
    abstract fun bindContext(application: Application): Context

    @Binds
    abstract fun bindNavigator(navigatorImpl: NavigatorImpl): Navigator
}