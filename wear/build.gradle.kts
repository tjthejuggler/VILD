plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.vild.wear"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.vild.wear"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // Shared constants (Data Layer paths & keys)
    implementation(project(":shared"))
    // Wearable Data Layer API
    implementation(libs.play.services.wearable)
    // Coroutines support for GMS Tasks (enables .await() on Task<T>)
    implementation(libs.kotlinx.coroutines.play.services)
    // Wear OS Compose UI
    implementation(libs.wear.compose.material)
    implementation(libs.wear.compose.foundation)
}
