plugins {
    alias(libs.plugins.library.plugin)
    alias(libs.plugins.library.compose.plugin)
    alias(libs.plugins.library.ksp.plugin)
}

android {
    namespace = "be.florien.anyflow.feature.library.ui"
}

dependencies {
    implementation(project(":common-di"))
    implementation(project(":common-image"))
    implementation(project(":common-navigation"))
    implementation(project(":common-base"))
    implementation(project(":common-resources"))
    implementation(project(":common-ui-domain"))
    implementation(project(":common-utils"))
    implementation(project(":common-widget"))
    implementation(project(":component-info"))
    implementation(project(":component-menu"))
    implementation(project(":component-viewholder"))
    implementation(project(":feature-library-domain"))
    implementation(project(":feature-library-podcast-domain"))
    implementation(project(":feature-library-tags-domain"))
    implementation(project(":feature-auth-domain"))
    implementation(project(":management-filters"))
    implementation(project(":management-filters-domain"))
    implementation(project(":management-queue"))
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)

    implementation(libs.collection.immutable)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.material)
    implementation(libs.dagger)
    ksp(libs.dagger.compiler)
    implementation(libs.coil)
    implementation(libs.coil.okhttp)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    //Paging
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.recyclerview.fastscroll)
    implementation(libs.telephoto)

}