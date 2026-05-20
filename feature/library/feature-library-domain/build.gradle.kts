plugins {
    alias(libs.plugins.library.plugin)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "be.florien.anyflow.feature.library.domain"
}

dependencies {
    implementation(project(":common-management"))
    implementation(project(":common-ui-domain"))
    implementation(project(":management-filters"))
    implementation(project(":management-filters-domain"))
    implementation(project(":management-playlist"))
    implementation(project(":management-podcast"))
    implementation(project(":management-tags"))
    implementation(project(":common-resources"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(project(":common-di"))
    ksp(libs.dagger.compiler)
    //Paging
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.dagger)
}