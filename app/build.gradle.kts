import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val vibeVersionName = (findProperty("vibeVersionName") as String?) ?: "1.2.0"
val vibeVersionCode = ((findProperty("vibeVersionCode") as String?) ?: "3").toInt()
val googleWebClientId = (findProperty("vibeGoogleWebClientId") as String?)
    ?: System.getenv("VIBE_GOOGLE_WEB_CLIENT_ID")
    ?: ""

// YouTube Data API v3 key. It is injected from a GitHub Actions secret, an
// environment variable or the git-ignored local.properties, never committed.
fun localProperty(name: String): String? {
    val file = rootProject.file("local.properties")
    if (!file.exists()) return null
    val properties = Properties()
    file.inputStream().use { properties.load(it) }
    return properties.getProperty(name)
}

val youtubeApiKey = (findProperty("vibeYouTubeApiKey") as String?)
    ?: System.getenv("VIBE_YOUTUBE_API_KEY")
    ?: localProperty("VIBE_YOUTUBE_API_KEY")
    ?: ""

// Optional release signing. The CI workflow decodes a keystore secret into
// VIBE_KEYSTORE_PATH so consecutive releases keep the same Android signature.
val releaseKeystorePath = System.getenv("VIBE_KEYSTORE_PATH")
val hasReleaseKeystore = !releaseKeystorePath.isNullOrBlank() && file(releaseKeystorePath).exists()

android {
    namespace = "com.vibemusic.app"
    compileSdk = 35
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.vibemusic.app"
        minSdk = 24
        targetSdk = 35
        versionCode = vibeVersionCode
        versionName = vibeVersionName

        vectorDrawables.useSupportLibrary = true

        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"${googleWebClientId.replace("\"", "\\\"")}\""
        )
        buildConfigField(
            "String",
            "YOUTUBE_API_KEY",
            "\"${youtubeApiKey.replace("\"", "\\\"")}\""
        )
        buildConfigField("String", "RELEASES_REPO", "\"DMorenoH0/VibeMusic\"")
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = System.getenv("VIBE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("VIBE_KEY_ALIAS") ?: "vibemusic"
                keyPassword = System.getenv("VIBE_KEY_PASSWORD")
                    ?: System.getenv("VIBE_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseKeystore) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-opt-in=kotlin.RequiresOptIn")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "/META-INF/versions/9/previous-compilation-data.bin"
        )
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
}
