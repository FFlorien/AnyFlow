plugins {
    alias(libs.plugins.library.plugin)
    id("kotlin-parcelize")
}

android {
    namespace = "be.florien.anyflow.management.alarm"
}

dependencies {
    implementation(project(":common-di"))
    implementation(project(":data-local"))

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.dagger)
}