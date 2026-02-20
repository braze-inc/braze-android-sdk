import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    namespace = "com.braze.hms_sample"

    defaultConfig {
        applicationId = "com.braze.hms_sample"
        compileSdk = rootProject.extra["compileSdkVersion"] as Int
        minSdk = rootProject.extra["minSdkVersion"] as Int
        // Huawei has not provided an SDK ready for
        // API 31 so this target version is on 30
        @Suppress("ExpiredTargetSdkVersion")
        targetSdk = 30
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("hms_sample_keystore.keystore")
            storePassword = "Z3SXw5ZyU!Up"
            keyAlias = "easter_egg"
            keyPassword = "Z3SXw5ZyU!Up"
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = true
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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
    }
}

dependencies {
    implementation(project(":android-sdk-ui")) {
        exclude(group = "com.google.firebase")
        exclude(group = "com.google.android.gms")
    }
    implementation(libs.kotlin.stdlib)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.huawei.hms.push)
}

repositories {
    // Note that for security reasons, this repository should
    // not be added at a higher level than this sample app's
    // gradle file.
    maven {
        url = uri("https://developer.huawei.com/repo/")
    }
}
