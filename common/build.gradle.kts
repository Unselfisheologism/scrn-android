plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.afollestad.mnmlscreenrecord.common"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        targetSdk = 35
    }
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // RxJava
    api("io.reactivex.rxjava2:rxandroid:2.1.1")

    // Google/AndroidX
    api("androidx.appcompat:appcompat:1.7.0")
    api("androidx.browser:browser:1.8.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Koin
    api("org.koin:koin-android:3.5.3")

    // afollestad
    api("com.afollestad:rxkprefs:1.2.5")

    // Debug
    api("com.jakewharton.timber:timber:5.0.1")
}