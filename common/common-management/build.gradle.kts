plugins {
    alias(libs.plugins.library.plugin)
}

android {
    namespace = "be.florien.anyflow.common.management"
}

dependencies {
    //Paging
    implementation(libs.androidx.paging.runtime.ktx)
}