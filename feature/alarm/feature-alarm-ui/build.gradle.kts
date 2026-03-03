plugins {
    alias(libs.plugins.library.plugin)
    alias(libs.plugins.library.compose.plugin)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kapt)
}

android {
    namespace = "be.florien.anyflow.feature.alarm.ui"
}

dependencies {
    implementation(project(":common-base"))
    implementation(project(":common-di"))
    implementation(project(":common-image"))
    implementation(project(":common-navigation"))
    implementation(project(":common-resources"))
    implementation(project(":common-ui-domain"))
    implementation(project(":component-menu"))
    implementation(project(":data-local"))
    implementation(project(":feature-player-service"))
    implementation(project(":management-alarm"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.collection.immutable)
    implementation(libs.material)
    implementation(libs.dagger)
    implementation(libs.androidx.media3.ui)
    ksp(libs.dagger.compiler)
}