plugins {
    alias(libs.plugins.library.plugin)
    alias(libs.plugins.ksp)
}

android {
    namespace = "be.florien.anyflow.feature.song.domain"
}

dependencies {
    implementation(project(":common-di"))
    implementation(project(":common-base"))
    implementation(project(":common-ui-domain"))
    implementation(project(":data-local"))
    implementation(project(":feature-songlist-base-domain"))
    implementation(project(":feature-song-base-domain"))
    implementation(project(":feature-song-base-ui"))
    implementation(project(":management-download"))
    implementation(project(":management-filters"))
    implementation(project(":management-queue"))
    implementation(project(":management-tags"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.material)
    implementation(libs.dagger)
    implementation(project(":management-filters-domain"))
    ksp(libs.dagger.compiler)

}