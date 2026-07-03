plugins {
    alias(libs.plugins.library.plugin)
    alias(libs.plugins.library.compose.plugin)
}

android {
    namespace = "be.florien.anyflow.feature.auth.ui"
}

dependencies {

    implementation(project(":common-base"))
    implementation(project(":common-logging"))
    implementation(project(":common-di"))
    implementation(project(":common-navigation"))
    implementation(project(":common-resources"))
    implementation(project(":common-utils"))
    implementation(project(":data-server"))
    implementation(project(":feature-auth-domain"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.navigation3.runtime)
    androidTestImplementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    //DI
    implementation(libs.dagger)
    ksp(libs.dagger.compiler)

    implementation(libs.androidx.constraintlayout)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
}