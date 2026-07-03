plugins {
    alias(libs.plugins.app.plugin)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "be.florien.anyflow"

    defaultConfig {
        applicationId = "be.florien.anyflow"
    }
    testOptions.unitTests {
        isIncludeAndroidResources = true
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
            isShrinkResources = false
        }
        release {
            isMinifyEnabled = false
            isDebuggable = false
        }
    }
}

dependencies {
    //Modules
    implementation(project(":common-base"))
    implementation(project(":common-di"))
    implementation(project(":common-image"))
    implementation(project(":common-management"))
    implementation(project(":common-navigation"))
    implementation(project(":common-resources"))
    implementation(project(":common-utils"))
    implementation(project(":common-ui-domain"))
    implementation(project(":component-dialog"))
    implementation(project(":component-info"))
    implementation(project(":component-menu"))
    implementation(project(":component-player-controls"))
    implementation(project(":component-viewholder"))
    implementation(project(":data-local"))
    implementation(project(":data-server"))
    implementation(project(":feature-alarm-ui"))
    implementation(project(":feature-auth-domain"))
    implementation(project(":feature-auth-ui"))
    implementation(project(":feature-filter-current-ui"))
    implementation(project(":feature-filter-saved-ui"))
    implementation(project(":feature-library-domain"))
    implementation(project(":feature-library-ui"))
    implementation(project(":feature-library-tags-domain"))
    implementation(project(":feature-library-podcast-domain"))
    implementation(project(":feature-player-service"))
    implementation(project(":feature-playlist-ui"))
    implementation(project(":feature-playlist-selection-domain"))
    implementation(project(":feature-playlist-selection-ui"))
    implementation(project(":feature-shortcut-ui"))
    implementation(project(":feature-song-base-domain"))
    implementation(project(":feature-song-base-ui"))
    implementation(project(":feature-song-domain"))
    implementation(project(":feature-medialist-ui"))
    implementation(project(":feature-sync-service"))
    implementation(project(":common-logging"))
    implementation(project(":management-alarm"))
    implementation(project(":management-queue"))
    implementation(project(":management-waveform"))
    implementation(project(":management-download"))
    implementation(project(":management-tags"))
    implementation(project(":management-urls"))
    implementation(project(":management-filters"))
    implementation(project(":management-filters-domain"))
    implementation(project(":management-playlist"))
    implementation(project(":management-podcast"))
    implementation(project(":common-base"))
    implementation(project(":common-di"))
    implementation(project(":common-image"))
    implementation(project(":common-logging"))
    implementation(project(":common-navigation"))
    implementation(project(":common-resources"))
    implementation(project(":component-menu"))
    implementation(project(":component-player-controls"))
    implementation(project(":data-local"))
    implementation(project(":feature-auth-domain"))
    implementation(project(":feature-player-service"))
    implementation(project(":feature-sync-service"))
    implementation(project(":management-alarm"))
    implementation(project(":management-filters"))
    implementation(project(":management-playlist"))
    implementation(project(":management-podcast"))
    implementation(project(":management-queue"))
    implementation(project(":management-tags"))
    implementation(project(":management-waveform"))
    //Android/Kotlin
    implementation(fileTree(mapOf("include" to "*.jar", "dir" to "libs")))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.runtime.saveable)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.multidex)
    implementation(libs.material)
    implementation(libs.kotlin.reflect)
    //DI
    implementation(libs.dagger)
    ksp(libs.dagger.compiler)
    implementation(libs.javax.annotation)
    //Internet
    implementation(libs.okhttp)
    implementation(libs.retrofit.converter.jackson)
    implementation(libs.coil)
    implementation(libs.coil.okhttp)
    //Glide
    implementation(libs.glide)
    //ExoPlayer
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.exoplayer)
    //WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.androidx.compose.material.icons.core)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.retrofit)
    implementation(libs.androidx.media3.datasource.okhttp)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.collection.immutable)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.paging.runtime.ktx)
    //Paging
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.recyclerview.fastscroll)
    implementation(libs.androidx.media3.datasource)
}