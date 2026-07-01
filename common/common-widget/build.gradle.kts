plugins {
    alias(libs.plugins.library.plugin)
    id("kotlin-parcelize")
}

android {
    namespace = "be.florien.anyflow.common.widget"
}

dependencies {
    api(libs.androidx.constraintlayout)
}