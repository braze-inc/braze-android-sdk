plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
}

extra["compileSdkVersion"] = 37
extra["minSdkVersion"] = 21
extra["targetSdkVersion"] = 37
extra["appVersionName"] = "42.3.0"

subprojects {
    repositories {
        maven { url = uri("https://braze-inc.github.io/braze-android-sdk/sdk") }
        mavenLocal()
        google()
        mavenCentral()
    }

    group = "com.braze"
    version = "42.3.0"
}
