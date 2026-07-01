plugins {
    alias(libs.plugins.library.plugin)
    alias(libs.plugins.ksp)
}

android {
    namespace = "be.florien.anyflow.component.image.display"
}

dependencies {
    //Modules
    implementation(project(":common-image"))
    implementation(project(":common-base"))
    implementation(project(":common-ui-domain"))
    implementation(project(":common-resources"))
    //Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    //glide
    implementation(libs.glide)
    ksp(libs.glide.ksp)
    implementation(libs.glide.okhttp3.integration)
    implementation(libs.stfalconimageviewer)
    //Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel)
}