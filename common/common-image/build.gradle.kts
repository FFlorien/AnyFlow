plugins {
    alias(libs.plugins.library.plugin)
    alias(libs.plugins.ksp)
    alias(libs.plugins.library.databinding.plugin)
    id("kotlin-parcelize")
}

android {
    namespace = "be.florien.anyflow.common.image"
}

dependencies {
    //Modules
    api(project(":common-ui-domain"))
    implementation(project(":common-resources"))
    //Android
    api(libs.androidx.fragment)
    api(libs.okhttp)
    api(libs.javax.inject)
    implementation(libs.androidx.annotation)
    implementation(libs.glide.annotations)
    //glide
    api(libs.glide)
    ksp(libs.glide.ksp)
    implementation(libs.glide.okhttp3.integration)
    //DI
    api(libs.dagger)
    ksp(libs.dagger.compiler)
}