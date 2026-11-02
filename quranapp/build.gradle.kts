plugins {
    id("com.android.application") version "9.4.0"
}

android {
    namespace = "com.minshawi.murattal.lost"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.minshawi.murattal.lost"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}
