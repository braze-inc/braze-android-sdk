plugins {
    id("com.android.application")
}

android {
    namespace = "com.braze.manualsessionintegration"

    defaultConfig {
        applicationId = "com.braze.manualsessionintegration"
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

dependencies {
    implementation(project(":android-sdk-ui"))
    implementation(libs.androidx.appcompat)
}
