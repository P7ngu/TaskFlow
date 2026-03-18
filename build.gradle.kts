// Top-level build file — configurazione comune a tutti i moduli
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android)      apply false
    alias(libs.plugins.kotlin.compose)      apply false
    // Hilt: dependency injection — apply false = non applicato qui, solo nel modulo app
    alias(libs.plugins.hilt)                apply false
    // KSP: Kotlin Symbol Processing — genera codice per Room e Hilt a compile-time
    alias(libs.plugins.ksp)                 apply false
}
