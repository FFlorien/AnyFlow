plugins {
    alias(libs.plugins.library.plugin)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "be.florien.anyflow.data.local"
}

dependencies {
    implementation(project(":common-logging"))
    implementation(project(":common-utils"))
    implementation(project(":management-filters-domain"))
    implementation(libs.dagger)
    ksp(libs.dagger.compiler)
    //LiveData
    implementation(libs.androidx.lifecycle.livedata.ktx)
    //Paging
    implementation(libs.androidx.paging.runtime.ktx)
    //Room
//    implementation("androidx.room:room-common:$room_version")
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    // Required -- JUnit 5 framework
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.jupiter.junit.jupiter.api)
    testRuntimeOnly(libs.jupiter.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}