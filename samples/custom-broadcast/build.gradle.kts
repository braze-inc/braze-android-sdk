plugins {
    id("com.android.application")
}

android {
    namespace = "com.braze.custombroadcast"
    val compileSdkPreviewName =
        rootProject.findProperty("compileSdkPreviewName")?.toString()?.trim().orEmpty()
    if (compileSdkPreviewName.isNotEmpty()) {
        compileSdkPreview = compileSdkPreviewName
    } else {
        compileSdk = rootProject.extra["compileSdkVersion"] as Int
    }

    defaultConfig {
        applicationId = "com.braze.custombroadcast"
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

dependencies {
    implementation(project(":android-sdk-ui"))
    implementation(libs.androidx.appcompat)
}
