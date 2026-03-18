plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp) //Kotlin Symbol Processing

}

android {
    namespace = "com.academy.taskflow"
    compileSdk = 35 // versione Android 15

    defaultConfig {
        applicationId = "com.academy.taskflow"
        minSdk = 26 // versione Android 8.0 Oreo
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
    kotlinOptions { jvmTarget = "11" }
    buildFeatures { compose = true }
}


dependencies {
    // Core Android - base per il nostro progetto
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose - UI dichiarativa
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.foundation)

    // Navigation compose - grafo di navigazione
    implementation(libs.androidx.navigation.compose)

    // Hilt - Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Hilt + WorkManager - integrazioni per HiltWorker
    ksp(libs.hilt.work.compiler)
    implementation(libs.hilt.work)

    // Room - persistenza locale SQLite, serve per l'integrità e la struttura dei dati
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Datastore - preferenze reattive utente, serve per la fluidità e la personalizzazione dell'UX
    // darkmode --> chiave dark_mode_enabled -> valore: true
    implementation(libs.datastore.preferences)

    // Workmanager - task in background
    implementation(libs.workmanager.ktx)

    // Retrofit - client HTTP dichiarativo
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // Gson converter - converte JSON in oggetti Kotlin automaticamente
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // OkHttp logging - visibilità delle chiamate HTTP in Logcat
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Test - librerie di test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    /** Modalità meno compatta per scrivere le librerie
     * Hilt dependency injection
    implementation("com.google.dagger:hilt-android:2.51")
    kapt("com.google.dagger:hilt-android-compiler:2.51")
    implementation("androidx.hilt:hilt-navigation-compose_1.2.0")
    WorkManager - task in background
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    Datastore - preferenze utente
    implementation("androidx-datastore:datastore-preferences:1.1.1")
    Lifecycle - collectAsStateWithLifecycle
    implementation("androidx.lifecycle:lifecyle-runtime-compose:2.6.1")*/
}