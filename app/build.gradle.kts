plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.cs3700"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.cs3700"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore)
    implementation(libs.play.services)
    implementation(libs.play.services.maps)
    implementation ("com.google.android.gms:play-services-auth:20.6.0")
    implementation ("com.google.api-client:google-api-client-android:1.33.2")
    implementation ("com.google.api-client:google-api-client-gson:1.33.2")
    implementation ("com.google.http-client:google-http-client-android:1.42.3")
    implementation ("com.google.http-client:google-http-client-gson:1.42.3")
    implementation("com.google.apis:google-api-services-drive:v3-rev20230120-1.33.2")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}