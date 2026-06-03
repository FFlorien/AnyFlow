plugins {
    alias(libs.plugins.library.plugin)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "be.florien.anyflow.feature.library.podcast.domain"
}

dependencies {
    implementation(project(":common-di"))
    implementation(project(":common-management"))
    implementation(project(":common-ui-domain"))
    implementation(project(":common-utils"))
    implementation(project(":management-filters"))
    implementation(project(":management-filters-domain"))
    implementation(project(":management-podcast"))
    implementation(project(":management-urls"))
    implementation(project(":feature-library-domain"))
    implementation(project(":feature-library-tags-domain"))
    implementation(project(":common-resources"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    ksp(libs.dagger.compiler)
    //Paging
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.dagger)
}