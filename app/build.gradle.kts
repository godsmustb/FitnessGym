import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

// Release signing: CI passes FG_* env vars (from GitHub secrets); locally we read
// ~/.fitnessgym/keystore.properties. The keystore never lives in the repo.
val signingProps = Properties().apply {
    val f = File(System.getProperty("user.home"), ".fitnessgym/keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signing(env: String, prop: String): String? = System.getenv(env) ?: signingProps.getProperty(prop)

// Every CI build gets a higher versionCode so Obtainium sees it as an update.
val buildNumber = (System.getenv("FG_BUILD_NUMBER") ?: "0").toInt()

android {
    namespace = "com.nunna.fitnessgym"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nunna.fitnessgym"
        minSdk = 28
        targetSdk = 36
        versionCode = 100 + buildNumber
        versionName = "0.2.$buildNumber"
    }

    // Exercises and motions live in the repo-root content/ folder, shared with the
    // desktop preview tool and the JVM tests.
    sourceSets["main"].assets.srcDirs(rootProject.file("content"))

    signingConfigs {
        create("release") {
            val store = signing("FG_KEYSTORE_FILE", "storeFile")
            if (store != null) {
                storeFile = file(store)
                storePassword = signing("FG_KEYSTORE_PASSWORD", "storePassword")
                keyAlias = signing("FG_KEY_ALIAS", "keyAlias")
                keyPassword = signing("FG_KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release").takeIf { it.storeFile != null }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    // Our gate is the JVM content/engine tests; lintVital is slow on release builds.
    lint { checkReleaseBuilds = false }
    // End-to-end UI + database tests run on the JVM with Robolectric (no emulator needed).
    testOptions { unitTests { isIncludeAndroidResources = true; all { it.maxHeapSize = "3g" } } }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(project(":anim"))
    implementation(project(":core"))
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.icons)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
