plugins {
    alias(libs.plugins.library.plugin)
    alias(libs.plugins.library.databinding.plugin)
    alias(libs.plugins.library.compose.plugin)
    alias(libs.plugins.library.ksp.plugin)
    id("kotlin-kapt")
}

android {
    namespace = "be.florien.anyflow.feature.mediaList.ui"

    buildFeatures {
        dataBinding = true
    }
}

dependencies {
    implementation(project(":common-di"))
    implementation(project(":common-image"))
    implementation(project(":common-navigation"))
    implementation(project(":common-base"))
    implementation(project(":common-logging"))
    implementation(project(":common-resources"))
    implementation(project(":common-ui-domain"))
    implementation(project(":common-utils"))
    implementation(project(":common-widget"))
    implementation(project(":component-menu"))
    implementation(project(":component-viewholder"))
    implementation(project(":data-local"))
    implementation(project(":feature-auth-domain"))
    implementation(project(":feature-library-ui"))
    implementation(project(":feature-song-base-ui"))
    implementation(project(":feature-player-service"))
    implementation(project(":feature-songlist-base-domain"))
    implementation(project(":feature-song-base-domain"))
    implementation(project(":feature-song-domain"))
    implementation(project(":management-download"))
    implementation(project(":management-filters"))
    implementation(project(":management-filters-domain"))
    implementation(project(":management-podcast"))
    implementation(project(":management-queue"))
    implementation(project(":management-tags"))
    implementation(project(":management-urls"))

    //Paging
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.coil)
    implementation(libs.coil.okhttp)
    implementation(libs.collection.immutable)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.recyclerview.fastscroll)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.material)
    implementation(libs.dagger)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.navigation.common.ktx)
    ksp(libs.dagger.compiler)

}