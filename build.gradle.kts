buildscript {
    dependencies {
        // AGP 9 has a runtime dependency on KGP 2.2.10. Raising it here is the
        // documented way to build with a newer Kotlin under built-in Kotlin.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
}
