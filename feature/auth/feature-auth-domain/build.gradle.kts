plugins {
    alias(libs.plugins.library.plugin)
    alias(libs.plugins.library.ksp.plugin)
}

android {
    namespace = "be.florien.anyflow.feature.auth.domain"
}

dependencies {
    implementation(project(":common-di"))
    implementation(project(":data-server"))
    implementation(project(":common-logging"))
    implementation(project(":common-utils"))

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.livedata.core.ktx)
    // DI
    implementation(libs.dagger)
    ksp(libs.dagger.compiler)
    // Internet
    implementation(libs.okhttp)
    implementation(libs.retrofit.converter.jackson )//todo use standalone jackson library
}