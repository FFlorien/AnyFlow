plugins {
    alias(libs.plugins.library.plugin)
    alias(libs.plugins.library.compose.plugin)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "be.florien.anyflow.common.navigation"
}

dependencies {
    api(libs.androidx.fragment)
    api(project(":common-ui-domain"))
    api(project(":common-resources"))
    api(project(":management-filters-domain"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
}