plugins {
    alias(libs.plugins.library.plugin)
    alias(libs.plugins.library.compose.plugin)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "be.florien.anyflow.feature.library.podcast.ui"
}

dependencies {
    implementation(project(":common-base"))
    implementation(project(":common-di"))
    implementation(project(":common-navigation"))
    implementation(project(":common-resources"))
    implementation(project(":common-ui-domain"))
    implementation(project(":component-info"))
    implementation(project(":feature-auth-domain"))
    implementation(project(":feature-library-domain"))
    implementation(project(":feature-library-ui"))
    implementation(project(":feature-library-podcast-domain"))
    implementation(project(":feature-library-tags-domain"))
    implementation(project(":management-filters"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.dagger)
    implementation(project(":management-filters-domain"))
    ksp(libs.dagger.compiler)
    implementation(libs.collection.immutable)
    //Paging
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.recyclerview.fastscroll)

}