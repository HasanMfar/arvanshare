plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.arvanshare.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.arvanshare.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0-beta.1"
    }

    // ── Signing ──────────────────────────────────────────────────────────────
    // Reads from environment variables so the keystore is never committed.
    // In GitHub Actions, KEYSTORE_BASE64 is decoded to a temp file first;
    // locally, point KEYSTORE_PATH at keystore/release.jks.
    signingConfigs {
        create("release") {
            val ksPath = System.getenv("KEYSTORE_PATH") ?: rootProject.file("keystore/release.jks").absolutePath
            storeFile = file(ksPath)
            storePassword = System.getenv("STORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: "arvanshare"
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
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
    }

    // The AWS Android SDK still uses the legacy Apache HTTP client.
    useLibrary("org.apache.http.legacy")
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.05.00"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.9")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    ksp("androidx.room:room-compiler:2.7.1")

    implementation("com.amazonaws:aws-android-sdk-s3:2.78.0")

    implementation("io.coil-kt:coil-compose:2.7.0")
}
