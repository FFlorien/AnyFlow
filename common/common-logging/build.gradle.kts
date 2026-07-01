plugins {
    alias(libs.plugins.library.plugin)
}

android {
    namespace = "be.florien.anyflow.common.logging"
}

dependencies {
    //debug
    implementation(libs.treessence)
    api(libs.timber)
    implementation(libs.firebase.core)
    implementation(libs.firebase.crashlytics)
}