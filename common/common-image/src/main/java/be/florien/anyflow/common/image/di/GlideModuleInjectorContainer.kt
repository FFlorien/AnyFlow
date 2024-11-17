package be.florien.anyflow.common.image.di

import be.florien.anyflow.common.image.AnyFlowAppGlideModule

interface GlideModuleInjectorContainer {
    val glideModuleInjector: GlideModuleInjector?
}

interface GlideModuleInjector {
    fun inject(anyFlowAppGlideModule: AnyFlowAppGlideModule)
}