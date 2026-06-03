plugins {
    alias(libs.plugins.library.plugin)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "be.florien.anyflow.feature.library.tags.domain"
}

dependencies {
    implementation(project(":common-di"))
    implementation(project(":common-management"))
    implementation(project(":common-resources"))
    implementation(project(":common-ui-domain"))
    implementation(project(":common-utils"))
    implementation(project(":feature-library-domain"))
    implementation(project(":management-filters"))
    implementation(project(":management-playlist"))
    implementation(project(":management-tags"))
    implementation(project(":management-urls"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(project(":management-filters-domain"))
    ksp(libs.dagger.compiler)
    //Paging
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.dagger)
}