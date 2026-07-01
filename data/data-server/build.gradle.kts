plugins {
    alias(libs.plugins.library.plugin)
    alias(libs.plugins.ksp)
}

android {
    namespace = "be.florien.anyflow.data.server"
}

dependencies {
    implementation(project(":common-di"))
    implementation(project(":common-logging"))
    implementation(project(":common-utils"))

    implementation(libs.dagger)
    ksp(libs.dagger.compiler)

    //internet
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.jackson)
    //WorkManager
    implementation(libs.androidx.work.runtime.ktx)
}