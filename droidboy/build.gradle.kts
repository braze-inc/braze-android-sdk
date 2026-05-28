import java.util.Date
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.appboy.sample"

    defaultConfig {
        compileSdk = rootProject.extra["compileSdkVersion"] as Int
        minSdk = rootProject.extra["minSdkVersion"] as Int
        targetSdk = rootProject.extra["targetSdkVersion"] as Int
        applicationId = "com.appboy.sample"
        versionName = rootProject.extra["appVersionName"] as String
        versionCode = 1
        resValue("string", "google_maps_key", (project.findProperty("GOOGLE_MAPS_API_KEY") as? String) ?: "")

        buildConfigField("String", "BUILD_TIME", "\"${Date()}\"")
        buildConfigField("boolean", "IS_DROIDBOY_RELEASE_BUILD", "false")
        buildConfigField("boolean", "STRICTMODE_ENABLED", "false")

        multiDexEnabled = true
    }

    lint {
        disable += "MissingTranslation"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        compose = true
    }

}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjvm-default=all")
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

dependencies {
    implementation(project(":android-sdk-ui"))
    implementation(project(":android-sdk-location"))
    implementation(project(":android-sdk-jetpack-compose"))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.google.material)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.constraintlayout)

    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)

    implementation(libs.glide)

    implementation(libs.firebase.core)
    implementation(libs.firebase.messaging)
    implementation(libs.play.services.mlkit.barcode)

    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.coroutines.android)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose.android)
    implementation(libs.androidx.multidex)

    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(libs.java.jwt)
    implementation(libs.zxing.core)
}

// Uncomment to use Firebase Messaging
//apply(plugin = "com.google.gms.google-services")
