plugins {
    alias(libs.plugins.library.plugin)
}

android {
    namespace = "be.florien.anyflow.feature.songlist.base.domain"
}

dependencies {
    implementation(libs.androidx.annotation.jvm)
}