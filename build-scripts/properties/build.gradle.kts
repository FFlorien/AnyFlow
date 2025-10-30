plugins {
    `kotlin-dsl`
}
gradlePlugin {
    plugins {
        register("app-plugin") {
            id = libs.plugins.app.plugin.get().pluginId
            implementationClass = "plugins.AppLevelPlugin"
        }
        register("library-plugin") {
            id = libs.plugins.library.plugin.get().pluginId
            implementationClass = "plugins.BaseLibraryModule"
        }
        register("compose-plugin") {
            id = libs.plugins.library.compose.plugin.get().pluginId
            implementationClass = "plugins.UiComposeModule"
        }
        register("databinding-plugin") {
            id = libs.plugins.library.databinding.plugin.get().pluginId
            implementationClass = "plugins.UiDataBindingModule"
        }
        register("ksp-plugin") {
            id = libs.plugins.library.ksp.plugin.get().pluginId
            implementationClass = "plugins.DomainKspModule"
        }
    }
}

dependencies {
    compileOnly(libs.gradle)
    compileOnly(libs.kotlin.gradle.plugin)
}