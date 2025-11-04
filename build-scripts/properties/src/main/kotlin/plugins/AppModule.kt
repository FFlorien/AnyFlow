package plugins

import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import extension.getLibsFromVersionCatalog
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.kotlin.dsl.configure

class AppLevelPlugin : Plugin<Project> {

    override fun apply(project: Project) = with(project) {
        val libs = getLibsFromVersionCatalog()
        configurePlugins(libs)

        configurations.configureEach {
            resolutionStrategy.force("com.google.code.findbugs:jsr305:3.0.2")
        }

        extensions.configure<BaseAppModuleExtension> {
            addSdkAndVersion(libs)
            addCompileOptions()
            addDataBinding()
            addSourceSets()
            addUseLibrary()
        }
    }

    private fun Project.configurePlugins(libs: VersionCatalog) {
        plugins.apply(libs.findPlugin("android-application").get().get().pluginId)
        plugins.apply(libs.findPlugin("crashlytics").get().get().pluginId)
        plugins.apply(libs.findPlugin("dependency-analysis").get().get().pluginId)
        plugins.apply(libs.findPlugin("google-services").get().get().pluginId)
        plugins.apply(libs.findPlugin("kotlin-android").get().get().pluginId)
        plugins.apply(libs.findPlugin("kotlin-parcelize").get().get().pluginId)
        plugins.apply(libs.findPlugin("ksp").get().get().pluginId)
    }

    private fun BaseAppModuleExtension.addSdkAndVersion(libs: VersionCatalog) {
        compileSdk = libs.findVersion("android.compileSdk").get().requiredVersion.toInt()
        defaultConfig {
            multiDexEnabled = true
            applicationId = "be.florien.anyflow"
            minSdk = libs.findVersion("android.minSdk").get().requiredVersion.toInt()
            targetSdk = libs.findVersion("android.targetSdk").get().requiredVersion.toInt()
            versionCode = libs.findVersion("app.version.code").get().requiredVersion.toInt()
            versionName = libs.findVersion("app.version.name").get().requiredVersion
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            vectorDrawables.useSupportLibrary = true
        }
    }

    private fun BaseAppModuleExtension.addCompileOptions() {
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
        }
    }

    private fun BaseAppModuleExtension.addDataBinding() {
        buildFeatures {
            dataBinding = true
        }
    }

    private fun BaseAppModuleExtension.addSourceSets() {
        sourceSets {
            getByName("main").java.srcDirs("src/main/kotlin")
            getByName("androidTest").java.srcDirs("src/sharedTest/java")
            getByName("test").java.srcDirs("src/sharedTest/java")
        }
    }

    private fun BaseAppModuleExtension.addUseLibrary() {
        useLibrary("android.test.runner")
        useLibrary("android.test.base")
        useLibrary("android.test.mock")
    }
}