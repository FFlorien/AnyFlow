plugins {
    alias(libs.plugins.library.plugin)
    alias(libs.plugins.ksp)
    alias(libs.plugins.library.databinding.plugin)
}

android {
    namespace = "be.florien.anyflow.feature.playlist.selection.domain"
}

dependencies {
    implementation(project(":common-di"))
    implementation(project(":common-base"))
    implementation(project(":management-playlist"))//todo implement repo for here
    implementation(project(":common-resources"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.dagger)
    implementation(project(":common-ui-domain"))
    implementation(project(":management-filters-domain"))
    implementation(project(":management-tags"))
    ksp(libs.dagger.compiler)
    //Paging
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.recyclerview.fastscroll)

}