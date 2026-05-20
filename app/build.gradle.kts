plugins {
    alias(libs.plugins.app.plugin)
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
    implementation(project(":feature-player-ui"))
    implementation(project(":feature-playlist-ui"))
    implementation(project(":feature-playlist-selection-domain"))
    implementation(project(":feature-playlist-selection-ui"))
    implementation(project(":feature-podcast-base-domain"))
    implementation(project(":feature-podcast-base-ui"))
    implementation(project(":feature-podcast-ui"))
    implementation(project(":feature-shortcut-ui"))
    implementation(project(":feature-song-base-domain"))
    implementation(project(":feature-song-base-ui"))
    implementation(project(":feature-song-domain"))
    implementation(project(":feature-song-ui"))
    implementation(project(":feature-songlist-ui"))
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
    //Android/Kotlin
    implementation(fileTree(mapOf("include" to "*.jar", "dir" to "libs")))
    implementation(libs.androidx.appcompat)
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
    //Glide
    implementation(libs.glide)
    //ExoPlayer
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.exoplayer)
    //WorkManager
    implementation(libs.androidx.work.runtime.ktx)
}