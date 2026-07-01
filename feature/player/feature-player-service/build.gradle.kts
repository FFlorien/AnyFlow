plugins {
    alias(libs.plugins.library.plugin)
    alias(libs.plugins.library.databinding.plugin)
    alias(libs.plugins.ksp)
}

android {
    namespace = "be.florien.anyflow.feature.player.service"
}

dependencies {
    implementation(project(":common-di"))
    implementation(project(":data-local"))
    implementation(project(":management-filters"))
    implementation(project(":management-playlist"))
    implementation(project(":management-podcast"))
    implementation(project(":management-tags"))
    implementation(project(":management-alarm"))
    implementation(project(":management-queue"))
    implementation(project(":management-urls"))
    implementation(project(":management-waveform"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.retrofit)
    implementation(project(":management-filters-domain"))
    implementation(project(":common-logging"))
    ksp(libs.dagger.compiler)
    //Paging
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.recyclerview.fastscroll)
    implementation(libs.dagger)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.datasource.okhttp)
    implementation(libs.androidx.media3.datasource)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.common.ktx)

}