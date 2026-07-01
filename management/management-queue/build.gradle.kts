plugins {
    alias(libs.plugins.library.plugin)
    id("kotlin-parcelize")
}

android {
    namespace = "be.florien.anyflow.management.queue"
}

dependencies {
    implementation(project(":common-di"))
    implementation(project(":data-local"))

    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.dagger)
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(project(":common-logging"))
    implementation(project(":management-filters-domain"))
    implementation(project(":management-tags"))
    implementation(project(":common-management"))
    implementation(project(":common-utils"))
    implementation(project(":management-podcast"))
}