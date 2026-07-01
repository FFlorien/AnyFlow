plugins {
    alias(libs.plugins.library.plugin)
    alias(libs.plugins.library.databinding.plugin)
}

android {
    namespace = "be.florien.anyflow.component.dialog"
}

dependencies {
    //Modules
    implementation(project(":common-image"))
    implementation(project(":common-resources"))
    implementation(project(":common-base"))
    //Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
}