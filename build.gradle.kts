plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) version libs.versions.kotlin.get() apply false
    alias(libs.plugins.google.gms.google.services) apply false
}