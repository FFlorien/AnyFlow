plugins {
    alias(libs.plugins.library.plugin)
    alias(libs.plugins.library.databinding.plugin)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kapt)
    id("kotlin-parcelize")
}

android {
    namespace = "be.florien.anyflow.component.viewholder"
}
//todo clean up resources

dependencies {
    //Modules
    implementation(project(":common-base"))
    implementation(project(":common-image"))
    implementation(project(":common-ui-domain"))
    implementation(project(":common-widget"))
    implementation(project(":common-resources") )//todo useful ?
    implementation(project(":feature-songlist-base-domain"))
    implementation(project(":feature-song-base-domain"))
    implementation(project(":management-queue"))
    //Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    //DI
    implementation(libs.dagger)
    ksp(libs.dagger.compiler)
    //Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel)
}