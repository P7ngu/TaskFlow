// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.android) apply false

    //Hilt - dependecy injection - apply false = non applicato qui, ma solo
    // nel modulo app
    alias(libs.plugins.hilt) apply false

    // KSP (Kotlin Symbol Processing) - genera codice per room e hilt a compile time
    alias(libs.plugins.ksp) apply false
}