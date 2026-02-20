plugins {
    id("com.android.application")
}

android {
    namespace = "com.braze.helloworld"

    defaultConfig {
        compileSdk = rootProject.extra["compileSdkVersion"] as Int
        minSdk = rootProject.extra["minSdkVersion"] as Int
        targetSdk = rootProject.extra["targetSdkVersion"] as Int
        versionName = "1.0"
    }

    buildTypes {
        getByName("debug") {
        }
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
    implementation(libs.androidx.concurrent.futures)
}
