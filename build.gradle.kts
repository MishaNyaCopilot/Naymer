// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("vkid.manifest.placeholders") version "1.1.0" apply true
}

buildscript {

    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath(libs.gradle)
        classpath(libs.kotlin.gradle.plugin)
        classpath("com.google.android.libraries.mapsplatform.secrets-gradle-plugin:secrets-gradle-plugin:2.0.1")
    }
}

vkidManifestPlaceholders {
    init(
        clientId = "client-id-here",
        clientSecret = "client-secret-here",
    )
    vkidRedirectHost = "vk.com"
    vkidRedirectScheme = "vkidredirect"
    vkidClientId = "vkid-client-id-here"
    vkidClientSecret = "vkid-client-secret-here"
}