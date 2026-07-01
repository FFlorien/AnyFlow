plugins {
    alias(libs.plugins.library.plugin)
}

android {
    namespace = "be.florien.anyflow.common.di"
}

dependencies {
    api(libs.dagger)
    api(libs.androidx.lifecycle.viewmodel)
}