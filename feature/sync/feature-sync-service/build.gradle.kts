plugins {
    alias(libs.plugins.library.plugin)
    alias(libs.plugins.ksp)
    alias(libs.plugins.library.databinding.plugin)
}

android {
    namespace = "be.florien.anyflow.feature.sync.service"
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
    implementation(project(":management-waveform"))
    implementation(libs.androidx.compose.runtime.annotation)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.retrofit)
    implementation(libs.androidx.media3.datasource.okhttp)
    implementation(project(":data-server"))
    implementation(project(":common-utils"))
    implementation(project(":common-logging"))
    ksp(libs.dagger.compiler)
    //Paging
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.recyclerview.fastscroll)
    implementation(libs.androidx.media3.session)
    implementation(libs.dagger)
    implementation(libs.androidx.media3.datasource)
    implementation(libs.androidx.media3.exoplayer)

}