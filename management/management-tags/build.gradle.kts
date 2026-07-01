plugins {
    alias(libs.plugins.library.plugin)
    id("kotlin-parcelize")
}

android {
    namespace = "be.florien.anyflow.management.tags"
}

dependencies {
    implementation(project(":common-di"))
    implementation(project(":common-management"))
    implementation(project(":common-utils"))
    implementation(project(":data-local"))
    implementation(project(":management-filters-domain"))

    implementation(libs.androidx.core.ktx)
    //Paging
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.dagger)
    //Room
    //todo add another layer of abstraction on top of libraryDatabase in data-local and remove these dependencies
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
}