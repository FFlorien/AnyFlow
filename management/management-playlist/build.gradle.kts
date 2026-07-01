plugins {
    alias(libs.plugins.library.plugin)
    alias(libs.plugins.ksp)
    alias(libs.plugins.library.databinding.plugin)
    id("kotlin-parcelize")
}

android {
    namespace = "be.florien.anyflow.management.playlist"
}

dependencies {
    implementation(project(":common-di"))
    implementation(project(":common-logging"))
    implementation(project(":common-management"))
    implementation(project(":common-resources"))
    implementation(project(":common-ui-domain"))
    implementation(project(":common-utils"))
    implementation(project(":data-local"))
    implementation(project(":data-server"))
    implementation(project(":management-filters-domain"))
    implementation(project(":management-urls"))
    //Room
    //todo add another layer of abstraction on top of libraryDatabase in data-local and remove these dependencies
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    //Paging
    implementation(libs.androidx.paging.runtime.ktx)

    //Workmanager
    implementation(libs.androidx.work.runtime.ktx)

    //DI
    implementation(libs.dagger)
    ksp(libs.dagger.compiler)
}