import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization) // Wajib untuk Supabase Data Mapping
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.primaraya.inspectra"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.primaraya.inspectra"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Membaca token dari local.properties
        val localProperties = Properties().apply {
            val file = rootProject.file("local.properties")
            if (file.exists()) {
                file.inputStream().use { load(it) }
            }
        }
        buildConfigField("String", "SUPABASE_URL", localProperties.getProperty("SUPABASE_URL") ?: "\"\"")
        buildConfigField("String", "SUPABASE_KEY", localProperties.getProperty("SUPABASE_KEY") ?: "\"\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // AndroidX & Core Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose & Material 3 Platform
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3) // Material 3 UI Kit
    implementation(libs.androidx.compose.material.icons.extended)

    // Supabase Ecosystem
    implementation(libs.supabase.postgrest) // Database CRUD
    implementation(libs.supabase.gotrue)    // Auth Login/Register
    implementation(libs.ktor.client.android) // Network Engine Ktor

    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)

    // Image Loader untuk Jetpack Compose
    implementation("io.coil-kt:coil-compose:2.6.0")
}
