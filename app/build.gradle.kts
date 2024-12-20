plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)


    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.juliancbagendacontactos"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.juliancbagendacontactos"
        minSdk = 24
        targetSdk = 34
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

    viewBinding{
        enable = true
    }
}

dependencies {

    implementation(libs.firebase.bom)
    implementation(libs.firebase.realtime)
    implementation(platform(libs.firebase.bom))
    implementation (libs.glide)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.androidx.core.splashscreen)
    annotationProcessor (libs.compiler)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}