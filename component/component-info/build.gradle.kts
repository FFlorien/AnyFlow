plugins {
    alias(libs.plugins.library.plugin)
    alias(libs.plugins.library.databinding.plugin)
    alias(libs.plugins.ksp)
    id("kotlin-parcelize")
}

android {
    namespace = "be.florien.anyflow.component.info"
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
    //DI
    implementation(libs.dagger)
    ksp(libs.dagger.compiler)
    //Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel)
}