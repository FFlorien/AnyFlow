plugins {
    alias(libs.plugins.library.plugin)
    alias(libs.plugins.ksp)
}

android {
    namespace = "be.florien.anyflow.feature.song.base.domain"
}

dependencies {

    implementation(project(":feature-songlist-base-domain"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.material)
    implementation(libs.dagger)
    implementation(project(":data-local"))
    implementation(project(":common-resources"))
    ksp(libs.dagger.compiler)

}