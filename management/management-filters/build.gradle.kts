plugins {
    alias(libs.plugins.library.plugin)
    id("kotlin-parcelize")
}

android {
    namespace = "be.florien.anyflow.management.filters"
}

dependencies {
    implementation(project(":common-di"))
    implementation(project(":data-local"))
    implementation(project(":management-filters-domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.dagger)
}