plugins {
    alias(libs.plugins.library.plugin)
    alias(libs.plugins.ksp)
    alias(libs.plugins.library.databinding.plugin)
}

android {
    namespace = "be.florien.anyflow.feature.shortcut.ui"
}

dependencies {
    implementation(project(":common-di"))
    implementation(project(":common-base"))
    implementation(project(":common-ui-domain"))
    implementation(project(":data-local"))
    implementation(project(":feature-song-base-ui"))
    implementation(project(":management-download"))
    implementation(project(":management-filters-domain"))
    implementation(project(":management-queue"))
    implementation(project(":management-tags"))
    implementation(project(":common-navigation"))
    implementation(project(":feature-songlist-base-domain"))
    implementation(project(":feature-song-base-domain"))
    implementation(project(":component-info"))
    implementation(project(":component-viewholder"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.dagger)
    implementation(libs.androidx.lifecycle.viewmodel)
    ksp(libs.dagger.compiler)

}