package be.florien.anyflow.injection

import android.app.Application
import android.content.Context
import be.florien.anyflow.NavigatorImpl
import be.florien.anyflow.common.navigation.Navigator
import dagger.Binds
import dagger.Module

@Module
abstract class ApplicationBindsModule {

    @Binds
    abstract fun bindNavigator(navigatorImpl: NavigatorImpl): Navigator

    @Binds
    abstract fun bindContext(application: Application): Context
}