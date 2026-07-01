plugins {
    alias(libs.plugins.library.plugin)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "be.florien.anyflow.common.resources"
}

dependencies {
    implementation(project(":feature-auth-domain"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation("androidx.compose.foundation:foundation:1.6.0-alpha01")
    implementation(libs.androidx.paging.compose)
    implementation(libs.coil)
    implementation(libs.coil.okhttp)
}