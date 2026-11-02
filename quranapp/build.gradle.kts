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
        versionCode = 3
        versionName = "1.2.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}
