// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.gradle)
        classpath(libs.kotlin.gradle.plugin)
        classpath(libs.google.services)
        classpath(libs.firebase.crashlytics.gradle)
    }
}

plugins {
    alias(libs.plugins.kapt) apply(false)
    alias(libs.plugins.ksp) apply(false)
    alias(libs.plugins.dependency.analysis)
    alias(libs.plugins.compose.compiler) apply false
}

allprojects {
    repositories {
        maven (url = "https://maven.google.com/")
        maven (url = "https://jitpack.io")
        mavenCentral()
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}