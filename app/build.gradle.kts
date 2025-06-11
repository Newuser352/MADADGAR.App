plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.madadgarapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.madadgarapp"
        minSdk = 23
        targetSdk = 34
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
            applicationIdSuffix = ".debug"
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
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
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
    implementation(libs.material)          // Material Design components
    implementation(libs.constraintlayout)  // ConstraintLayout for responsive UI
    
    // Third-party libraries
    implementation(libs.countrycodepicker) // Country Code Picker for phone input
    implementation(libs.circleimageview)   // CircleImageView for profile pictures
    
    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test:runner:1.5.2")
    
    // For Fragment testing
    debugImplementation("androidx.fragment:fragment-testing:1.6.2")
}
