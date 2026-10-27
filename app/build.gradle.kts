import java.util.Properties
import java.net.URI

plugins {
    id("com.android.application")
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.isFile) {
        localPropertiesFile.inputStream().use(::load)
    }
}

// Client keys are extractable from an APK. Use a dedicated, restricted Android key,
// never the backend geocoding key. No credential fallback in versioned source.
val mapTilerApiKey = providers.environmentVariable("MAPTILER_ANDROID_API_KEY")
    .orElse(providers.provider { localProperties.getProperty("mapTilerApiKey", "") })
    .get().trim()
require(mapTilerApiKey.isEmpty() || mapTilerApiKey.matches(Regex("[A-Za-z0-9_-]+"))) {
    "MapTiler Android key must contain only letters, digits, underscore or hyphen."
}

val developmentApiBaseUrl = providers.gradleProperty("agenticApiBaseUrl")
    .orElse(providers.provider { localProperties.getProperty("agenticApiBaseUrl") })
    .orElse("http://10.0.2.2:8000/")
    .get()
val releaseApiBaseUrl = providers.gradleProperty("agenticReleaseApiBaseUrl")
    .orElse(providers.provider { localProperties.getProperty("agenticReleaseApiBaseUrl") })
    .orElse("")
    .get()

fun validateApiBaseUrl(value: String, property: String, httpsOnly: Boolean) {
    val uri = runCatching { URI(value) }.getOrNull()
    require(uri != null && !uri.host.isNullOrBlank() &&
        uri.scheme in (if (httpsOnly) setOf("https") else setOf("http", "https")) &&
        uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null &&
        value.endsWith("/")) {
        "$property must be an absolute ${if (httpsOnly) "HTTPS" else "HTTP(S)"} URL ending in /, without credentials, query, or fragment."
    }
}

validateApiBaseUrl(developmentApiBaseUrl, "agenticApiBaseUrl", httpsOnly = false)
val validateReleaseApiBaseUrl = tasks.register("validateReleaseApiBaseUrl") {
    doLast { validateApiBaseUrl(releaseApiBaseUrl, "agenticReleaseApiBaseUrl", httpsOnly = true) }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    dependsOn(validateReleaseApiBaseUrl)
}

android {
    namespace = "com.example.vedaahar"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.vedamrit"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "MAPTILER_API_KEY", "\"$mapTilerApiKey\"")
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "AGENTIC_DRAFT_PREVIEW", "true")
            buildConfigField("String", "AGENTIC_API_BASE_URL", "\"$developmentApiBaseUrl\"")
        }

        release {
            buildConfigField("boolean", "AGENTIC_DRAFT_PREVIEW", "false")
            buildConfigField("String", "AGENTIC_API_BASE_URL", "\"$releaseApiBaseUrl\"")
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    implementation("org.maplibre.gl:android-sdk:13.7.1")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.json)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
