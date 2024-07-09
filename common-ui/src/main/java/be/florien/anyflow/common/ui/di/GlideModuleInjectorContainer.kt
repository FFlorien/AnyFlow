package be.florien.anyflow.common.ui.di

import be.florien.anyflow.common.ui.MyAppGlideModule

interface GlideModuleInjectorContainer {
    val glideModuleInjector: GlideModuleInjector?
}

interface GlideModuleInjector {

    fun inject(myAppGlideModule: MyAppGlideModule)
}