plugins {
    alias(libs.plugins.library.plugin)
    alias(libs.plugins.library.compose.plugin)
    alias(libs.plugins.library.ksp.plugin)
}

android {
    namespace = "be.florien.anyflow.feature.filter.current.ui"
}

dependencies {
    implementation(project(":common-di"))
    implementation(project(":common-base"))
    implementation(project(":common-logging"))
    implementation(project(":common-navigation"))
    implementation(project(":common-image"))
    implementation(project(":common-resources"))
    implementation(project(":common-ui-domain"))
    implementation(project(":component-menu"))
    implementation(project(":feature-library-ui"))
    implementation(project(":feature-auth-domain"))
    implementation(project(":management-filters"))
    implementation(project(":management-filters-domain"))
    implementation(project(":management-tags"))
    implementation(project(":management-urls"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.navigation3.runtime)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.coil)
    implementation(libs.coil.okhttp)
    implementation(libs.collection.immutable)
    implementation(libs.dagger)
    ksp(libs.dagger.compiler)
    //Paging
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.recyclerview.fastscroll)
    implementation(libs.glide)
}