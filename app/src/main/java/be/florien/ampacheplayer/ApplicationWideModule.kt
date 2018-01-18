package be.florien.ampacheplayer

import android.app.Application
import android.content.Context
import dagger.Binds
import dagger.Module

/**
 * Created by florien on 1/01/18.
 */
@Module
abstract class ApplicationWideModule {

    @Binds
    abstract fun bindContext(application: Application): Context
}