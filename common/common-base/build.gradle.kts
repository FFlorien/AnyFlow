plugins {
    alias(libs.plugins.library.plugin)
    alias(libs.plugins.library.databinding.plugin)
}

android {
    namespace = "be.florien.anyflow.common.base"
}

dependencies {
    api(project(":common-ui-domain"))

    api(libs.androidx.fragment)
    api(libs.androidx.lifecycle.livedata.core)
    api(libs.androidx.lifecycle.viewmodel)
    api(libs.androidx.recyclerview)
    api(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.annotation)
    implementation(libs.androidx.core)
    implementation(libs.androidx.databinding.common)
}