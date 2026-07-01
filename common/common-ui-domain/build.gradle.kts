plugins {
    alias(libs.plugins.library.plugin)
    id("kotlin-parcelize")
}

android {
    namespace = "be.florien.anyflow.common.ui.domain"
}
dependencies {
    implementation(libs.androidx.annotation)
    implementation(libs.kotlin.parcelize.runtime)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)

    implementation(project(":common-utils"))
}