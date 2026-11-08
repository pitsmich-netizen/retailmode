plugins { id("com.android.application") }

android {
    namespace = "com.example.retailhelper"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.retailhelper"
        minSdk = 29
        targetSdk = 35
        versionCode = 19
        versionName = "0.2.13"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
