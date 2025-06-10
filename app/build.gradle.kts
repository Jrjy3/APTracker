plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "2.0.0-1.0.22"
}

android {
    namespace = "com.wavehitech.aptracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.wavehitech.aptracker"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    dependenciesInfo {
        includeInApk = true
        includeInBundle = true
    }
    buildToolsVersion = "36.0.0"
}

dependencies {
    // Define consistent versions
    val composeVersion = "1.8.2"
    val composeBomVersion = "2025.05.01"

    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM - this manages all Compose library versions
    implementation(platform(libs.androidx.compose.bom))

    // Compose UI dependencies (versions managed by BOM)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")

    // Material Design
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.material)

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.9.0")

    // Paging
    implementation("androidx.paging:paging-compose:3.3.6")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Room database
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    ksp("androidx.room:room-compiler:2.7.1")

    // JSON parsing
    implementation("com.google.code.gson:gson:2.13.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Compose Runtime
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")

    // EXIF data
    implementation("androidx.exifinterface:exifinterface:1.4.1")

    // Media3 (if needed)
    implementation(libs.androidx.media3.common.ktx)

    // Testing dependencies
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.13.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("androidx.room:room-testing:2.7.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.compose.ui.ui.test.junit4)

    // Debug dependencies
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}