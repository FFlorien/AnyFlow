plugins {
    alias(libs.plugins.library.plugin)
    id("kotlin-parcelize")
}

android {
    namespace = "be.florien.anyflow.management.waveform"
}

dependencies {
    implementation(project(":common-di"))
    implementation(project(":common-image"))
    implementation(project(":common-base"))
    implementation(project(":data-local"))

    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.dagger)
    implementation(project(":common-logging"))
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.glide)
}