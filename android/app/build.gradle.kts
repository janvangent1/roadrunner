plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.roadrunner.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.roadrunner.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    flavorDimensions += "brand"
    productFlavors {
        create("motorcycle") {
            dimension = "brand"
            applicationId = "com.roadrunner.app"
            versionNameSuffix = "-motorcycle"
        }
        create("sportscar") {
            dimension = "brand"
            applicationId = "com.roadrunner.sportscar"
            versionNameSuffix = "-sportscar"
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:3000\"")
            buildConfigField("String", "TINK_KEYSET_B64", "\"PLACEHOLDER_SERVER_KEYSET_B64\"")
            buildConfigField("String", "CERT_PIN_SHA256", "\"PLACEHOLDER_PIN_SHA256\"")
            isDebuggable = true
        }
        release {
            buildConfigField("String", "BASE_URL", "\"https://api.roadrunner.app\"")
            buildConfigField("String", "CERT_PIN_SHA256", "\"PLACEHOLDER_PIN_SHA256\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Activity + Navigation
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")

    // Network
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Map
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    implementation("androidx.preference:preference-ktx:1.2.1")

    // Google Credentials / Sign-In
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // JWT storage — Google Tink (NOT security-crypto which is deprecated Jul 2025)
    implementation("com.google.crypto.tink:tink-android:1.15.0")

    // GPX parsing — used after in-memory decryption of encrypted GPX files
    implementation("com.github.ticofab:android-gpx-parser:2.3.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // GPS — FusedLocationProviderClient
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // WorkManager for tile caching (Plan 03)
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Play Integrity API
    implementation("com.google.android.play:integrity:1.4.0")

    // Core library desugaring for java.time on API 24+
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
