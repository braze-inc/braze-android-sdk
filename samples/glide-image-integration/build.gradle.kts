import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    namespace = "com.braze.glideimageintegration"

    defaultConfig {
        applicationId = "com.braze.glideimageintegration"
        compileSdk = rootProject.extra["compileSdkVersion"] as Int
        minSdk = rootProject.extra["minSdkVersion"] as Int
        targetSdk = rootProject.extra["targetSdkVersion"] as Int
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
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
    implementation(libs.glide)
    implementation(libs.kotlin.stdlib)
}
