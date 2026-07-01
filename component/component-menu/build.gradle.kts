plugins {
    alias(libs.plugins.library.plugin)
}

android {
    namespace = "be.florien.anyflow.component.menu"
}

dependencies {
    implementation(libs.androidx.annotation.jvm)
    implementation(libs.material)
}