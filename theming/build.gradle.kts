plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.afollestad.mnmlscreenrecord.theming"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        targetSdk = 35
    }
}

dependencies {
    implementation(project(":common"))

    // Needed for md_ attributes in styles.xml
    implementation("com.afollestad.material-dialogs:core:3.3.0")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.0")

    // Google/AndroidX
    api("com.google.android.material:material:1.11.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.google.truth:truth:1.2.0")
}