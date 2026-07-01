plugins {
    alias(libs.plugins.library.plugin)
}

android {
    namespace = "be.florien.anyflow.component.player.controls"
}

dependencies {
    implementation(project(":common-resources"))
    implementation(project(":common-utils"))
    implementation(libs.material)
}