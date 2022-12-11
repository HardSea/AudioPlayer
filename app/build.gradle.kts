plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.hillywave.audioplayer"
    compileSdk = 32

    version = 1
    // versionName = "1.0"

    defaultConfig {
        minSdk = 22
        targetSdk = 32

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation("com.android.support:appcompat-v7:26.1.0")
    implementation("com.android.support:design:26.1.0")
    implementation("com.android.support.constraint:constraint-layout:1.1.3")
    implementation("com.android.support:recyclerview-v7:26.1.0")
    implementation("com.android.support:cardview-v7:26.1.0")
    implementation("com.android.support:support-media-compat:26.1.0")
    implementation("com.android.support:support-v4:26.1.0")
    implementation("com.sothree.slidinguppanel:library:3.4.0")
    testImplementation("junit:junit:4.12")
    androidTestImplementation("com.android.support.test:runner:1.0.2")
    androidTestImplementation("com.android.support.test.espresso:espresso-core:3.0.2")

    implementation("com.google.code.gson:gson:2.8.2")
}
