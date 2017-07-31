package be.florien.ampacheplayer.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.Reusable

/**
 * Module providing system component
 */
@Module
class AndroidModule (var context: Context){
    private val PREFERENCE_NAME = "ampache_preferences"

    @Provides
    fun provideContext() : Context = context

    @Provides
    @Reusable
    fun providePreferences() : SharedPreferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
}