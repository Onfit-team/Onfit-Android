plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {

    buildFeatures {
        viewBinding = true
    }

    namespace = "com.example.onfit"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.onfit"
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
}

dependencies {
    //바텀 네비게이션
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    //리사이클러뷰
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    // flexbox
    implementation ("com.google.android.flexbox:flexbox:3.0.0")

    // 이미지 크롭 기능
    implementation("com.vanniktech:android-image-cropper:4.5.0")

    // Retrofit 라이브러리
    implementation("com.squareup.retrofit2:retrofit:2.6.4")

    // Gson 변환기 라이브러리
    implementation("com.squareup.retrofit2:converter-gson:2.6.4")

    // okhttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}