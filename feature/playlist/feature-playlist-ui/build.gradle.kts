plugins {
    alias(libs.plugins.library.plugin)
    alias(libs.plugins.ksp)
    alias(libs.plugins.library.databinding.plugin)
}

android {
    namespace = "be.florien.anyflow.feature.playlist.ui"
}

dependencies {
    implementation(project(":common-di"))
    implementation(project(":common-base"))
    implementation(project(":common-image"))
    implementation(project(":common-navigation"))
    implementation(project(":common-resources"))
    implementation(project(":common-ui-domain"))
    implementation(project(":common-widget"))
    implementation(project(":component-dialog"))
    implementation(project(":component-menu"))
    implementation(project(":management-filters"))
    implementation(project(":management-playlist"))//todo implement repo for here
    implementation(project(":management-tags"))
    implementation(project(":management-urls"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.dagger)
    implementation(project(":management-filters-domain"))
    ksp(libs.dagger.compiler)
    //Paging
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.recyclerview.fastscroll)

}