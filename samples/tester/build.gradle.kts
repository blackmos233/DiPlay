plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.diplay.tester"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.diplay.tester"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
