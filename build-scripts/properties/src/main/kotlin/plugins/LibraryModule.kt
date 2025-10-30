package plugins

import com.android.build.api.dsl.LibraryExtension
import extension.getLibsFromVersionCatalog
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.kotlin.dsl.configure

class BaseLibraryModule : Plugin<Project> {

    override fun apply(project: Project) = with(project) {
        val libs = getLibsFromVersionCatalog()
        plugins.apply(libs.findPlugin("android-library").get().get().pluginId)
        plugins.apply(libs.findPlugin("kotlin-android").get().get().pluginId)
        plugins.apply(libs.findPlugin("dependency-analysis").get().get().pluginId)

        extensions.configure<LibraryExtension> {
            addAndroidConfiguration(libs)
        }
    }

    private fun LibraryExtension.addAndroidConfiguration(libs: VersionCatalog) {
        compileSdk = libs.findVersion("android.compileSdk").get().requiredVersion.toInt()
        defaultConfig {
            minSdk = libs.findVersion("android.minSdk").get().requiredVersion.toInt()
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
        }
    }
}

class UiComposeModule : Plugin<Project> {

    override fun apply(project: Project) = with(project) {
        val libs = getLibsFromVersionCatalog()
        plugins.apply(libs.findPlugin("compose-compiler").get().get().pluginId)
        plugins.apply(libs.findPlugin("ksp").get().get().pluginId)

        extensions.configure<LibraryExtension> {
            buildFeatures {
                compose = true
            }
        }
    }
}

class UiDataBindingModule : Plugin<Project> {

    override fun apply(project: Project) = with(project) {
        val libs = getLibsFromVersionCatalog()
        plugins.apply(libs.findPlugin("kapt").get().get().pluginId)

        extensions.configure<LibraryExtension> {
            buildFeatures {
                dataBinding = true
            }
        }
    }
}

class DomainKspModule : Plugin<Project> {

    override fun apply(project: Project) = with(project) {
        val libs = getLibsFromVersionCatalog()
        plugins.apply(libs.findPlugin("ksp").get().get().pluginId)
        Unit
    }
}