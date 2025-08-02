plugins {
    alias(libs.plugins.android.application)
    kotlin("android")
    kotlin("plugin.serialization")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.madadgarapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.madadgarapp"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Enable vector drawables
        vectorDrawables.useSupportLibrary = true
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
        debug {
            isDebuggable = true
        }
    }
    
    // Memory optimization
    packaging {
        resources {
            excludes += listOf(
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE.txt",
                "META-INF/DEPENDENCIES"
            )
        }
    }
    
    buildFeatures {
        viewBinding = true
    }
    
    lint {
        disable.add("NullSafeMutableLiveData")
        abortOnError = false
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Core library desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")
    
    // AndroidX and Core libraries
    implementation(libs.appcompat)
    implementation(libs.activity)
    implementation(libs.fragment.ktx)       // Fragment KTX for better fragment management
    
    // Lifecycle and ViewModel
    implementation(libs.lifecycle.viewmodel) // ViewModel components
    implementation(libs.lifecycle.livedata)  // LiveData components
    implementation(libs.lifecycle.runtime)   // Lifecycle runtime
    implementation(libs.savedstate)          // SavedState for ViewModel
    
    // UI Components
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation(libs.material)          // Material Design components
    implementation(libs.constraintlayout)  // ConstraintLayout for responsive UI
    
    // Third-party libraries
    implementation(libs.countrycodepicker) // Country Code Picker for phone input
    implementation(libs.circleimageview)   // CircleImageView for profile pictures
    implementation("com.github.bumptech.glide:glide:4.16.0") // Image loading library
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    
    
    // Activity and Fragment KTX for activity result API
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    
    // Credential Manager
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    
    // Security crypto for encrypted SharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Google Sign-In and Firebase Auth
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    
    // Hilt for dependency injection
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-android-compiler:2.50")
    
    // Supabase SDK
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.6.0")
    implementation("io.github.jan-tennert.supabase:gotrue-kt:2.6.0")
    implementation("io.github.jan-tennert.supabase:realtime-kt:2.6.0")
    implementation("io.github.jan-tennert.supabase:storage-kt:2.6.0")
    
    // Ktor for network requests (required by Supabase)
    implementation("io.ktor:ktor-client-android:2.3.12")
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-utils:2.3.12")
    
    // Kotlinx Serialization (required by Supabase)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Location Services
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    
    // Permissions
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.core:core-ktx:1.12.0")
    
    // Firebase Cloud Messaging for notifications
    implementation("com.google.firebase:firebase-messaging-ktx")
    
    // WorkManager for background tasks
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test:runner:1.5.2")
    
    // For Fragment testing
    debugImplementation("androidx.fragment:fragment-testing:1.6.2")
}
