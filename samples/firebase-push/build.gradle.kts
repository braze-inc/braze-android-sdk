import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.google.gms.google-services")
}

apply(from = rootProject.file("config/buildscript/break-compile-on-deprecations.gradle"))

android {
    namespace = "com.braze.firebasepush"
    val compileSdkPreviewName =
        rootProject.findProperty("compileSdkPreviewName")?.toString()?.trim().orEmpty()
    if (compileSdkPreviewName.isNotEmpty()) {
        compileSdkPreview = compileSdkPreviewName
    } else {
        compileSdk = rootProject.extra["compileSdkVersion"] as Int
    }

    defaultConfig {
        applicationId = "com.braze.firebasepush"
        minSdk = rootProject.extra["minSdkVersion"] as Int
        if (compileSdkPreviewName.isNotEmpty()) {
            targetSdkPreview = compileSdkPreviewName
        } else {
            targetSdk = rootProject.extra["targetSdkVersion"] as Int
        }
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("debug") {
            multiDexEnabled = true
            isDebuggable = true
        }
        getByName("release") {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
            multiDexEnabled = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
        freeCompilerArgs.addAll("-Xjvm-default=all")
    }
}

dependencies {
    implementation(project(":android-sdk-ui"))
    implementation(libs.androidx.appcompat)
    implementation(libs.firebase.messaging)
    implementation(libs.kotlin.stdlib)
}
