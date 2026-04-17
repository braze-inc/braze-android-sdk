plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.braze.googletagmanager"
    val compileSdkPreviewName =
        rootProject.findProperty("compileSdkPreviewName")?.toString()?.trim().orEmpty()
    if (compileSdkPreviewName.isNotEmpty()) {
        compileSdkPreview = compileSdkPreviewName
    } else {
        compileSdk = rootProject.extra["compileSdkVersion"] as Int
    }

    defaultConfig {
        applicationId = "com.braze.googletagmanager"
        minSdk = rootProject.extra["minSdkVersion"] as Int
        if (compileSdkPreviewName.isNotEmpty()) {
            targetSdkPreview = compileSdkPreviewName
        } else {
            targetSdk = rootProject.extra["targetSdkVersion"] as Int
        }
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
        }
    }

    lint {
        disable += "Instantiatable"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(project(":android-sdk-ui")) {
        exclude(group = "com.google.firebase")
        exclude(group = "com.google.android.gms")
    }
    implementation(libs.androidx.appcompat)
    implementation(libs.play.services.tagmanager)
    implementation(libs.androidx.multidex)
}
