plugins {
    alias(libs.plugins.library.plugin)
    id("kotlin-parcelize")
}

android {
    namespace = "be.florien.anyflow.management.download"
}

dependencies {
    implementation(project(":common-di"))
    implementation(project(":common-logging"))
    implementation(project(":data-local"))
    implementation(project(":management-filters"))
    implementation(project(":management-filters-domain"))
    implementation(project(":management-tags"))
    implementation(project(":management-urls"))

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.dagger)
    implementation(libs.retrofit)
}