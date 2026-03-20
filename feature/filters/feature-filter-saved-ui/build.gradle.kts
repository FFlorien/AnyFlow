plugins {
    alias(libs.plugins.library.plugin)
    alias(libs.plugins.library.compose.plugin)
    alias(libs.plugins.library.ksp.plugin)
}

android {
    namespace = "be.florien.anyflow.feature.filter.saved.ui"
}

dependencies {
    implementation(project(":common-di"))
    implementation(project(":common-base"))
    implementation(project(":common-resources"))
    implementation(project(":common-ui-domain"))
    implementation(project(":common-utils"))
    implementation(project(":management-filters"))
    implementation(project(":management-filters-domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.dagger)
    implementation(project(":feature-library-ui"))
    implementation(project(":management-tags"))
    implementation(project(":common-navigation"))
    implementation(libs.androidx.compose.material3)
    ksp(libs.dagger.compiler)
    //Paging
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.recyclerview.fastscroll)
    implementation(libs.glide)
    implementation(libs.collection.immutable)
}