plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace   = "com.academy.taskflow"
    compileSdk  = 35

    defaultConfig {
        applicationId             = "com.academy.taskflow"
        minSdk                    = 26
        targetSdk                 = 35
        versionCode               = 1
        versionName               = "1.0"
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
    kotlinOptions { jvmTarget = "11" }
    buildFeatures { compose = true }
}

dependencies {
    // Core Android — base del progetto
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose — UI dichiarativa
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.foundation)

    // Navigation Compose — grafo di navigazione
    implementation(libs.androidx.navigation.compose)

    // Hilt — Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Hilt + WorkManager — integrazione per HiltWorker
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Room — persistenza locale SQLite
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore — preferenze utente reattive
    implementation(libs.datastore.preferences)

    // WorkManager — background processing garantito
    implementation(libs.workmanager.ktx)

    // Retrofit — client HTTP dichiarativo
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // Gson converter — deserializzazione JSON automatica
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // OkHttp logging — visibilità chiamate HTTP in Logcat (solo debug)
    implementation("com.squareup.okhttp3:logging-interceptor:5.3.2")

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
