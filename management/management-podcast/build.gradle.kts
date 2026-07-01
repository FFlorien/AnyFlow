plugins {
    alias(libs.plugins.library.plugin)
}

android {
    namespace = "be.florien.anyflow.management.podcast"
}

dependencies {
    implementation(project(":common-di"))
    implementation(project(":common-management"))
    implementation(project(":common-ui-domain"))
    implementation(project(":data-local"))
    implementation(project(":data-server"))
    implementation(project(":management-filters-domain"))
    implementation(project(":management-tags"))//todo remove when urlrepo is in its own module

    implementation(libs.androidx.core.ktx)
    //Paging
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.dagger)
    //Room
    //todo add another layer of abstraction on top of libraryDatabase in data-local and remove these dependencies
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
}