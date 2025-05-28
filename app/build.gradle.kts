plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.de2"
    compileSdk = 35 // Đã có

    defaultConfig {
        applicationId = "com.example.de2"
        minSdk = 24
        targetSdk = 35 // Đã có
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.swipeRefreshLayout)

    // Thêm dòng này để implement Glide:
    implementation(libs.glide)
    // Dòng này tùy chọn, thường không cần thiết cho Java với các phiên bản Glide mới cho tính năng cơ bản
    // annotationProcessor(libs.glide.compiler)
}