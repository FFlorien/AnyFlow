plugins {
    alias(libs.plugins.library.plugin)
    alias(libs.plugins.library.compose.plugin)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "be.florien.anyflow.feature.library.tags.ui"
}

dependencies {
    implementation(project(":common-di"))
    implementation(project(":common-base"))
    implementation(project(":common-ui-domain"))
    implementation(project(":common-utils"))
    implementation(project(":common-resources"))
    implementation(project(":feature-library-domain"))
    implementation(project(":feature-library-tags-domain"))
    implementation(project(":feature-library-ui"))
    implementation(project(":feature-auth-domain"))
    implementation(project(":management-filters"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.dagger)
    implementation(libs.coil)
    implementation(project(":common-navigation"))
    implementation(project(":component-info"))
    implementation(project(":management-filters-domain"))
    implementation(libs.collection.immutable)
    ksp(libs.dagger.compiler)
    //Paging
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.recyclerview.fastscroll)

}