
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.operit.hohyaiimage"
    compileSdk = 34

    val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
    val keystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
    val keyAlias = System.getenv("ANDROID_KEY_ALIAS")
    val keyPassword = System.getenv("ANDROID_KEY_PASSWORD")

    val bundledKeystore = file("keystore/universal-image-studio-release.jks")
    val bundledKeystorePassword = "UniversalImageStudio2026"
    val bundledKeyAlias = "universal-image-studio"
    val bundledKeyPassword = "UniversalImageStudio2026"

    val hasSigningEnv =
        !keystorePath.isNullOrBlank() &&
        !keystorePassword.isNullOrBlank() &&
        !keyAlias.isNullOrBlank() &&
        !keyPassword.isNullOrBlank()
    val hasBundledSigning = bundledKeystore.exists()
    val hasReleaseSigning = hasSigningEnv || hasBundledSigning

    if (hasReleaseSigning) {
        signingConfigs {
            create("sharedRelease") {
                if (hasSigningEnv) {
                    storeFile = file(keystorePath!!)
                    storePassword = keystorePassword
                    this.keyAlias = keyAlias
                    this.keyPassword = keyPassword
                } else {
                    storeFile = bundledKeystore
                    storePassword = bundledKeystorePassword
                    this.keyAlias = bundledKeyAlias
                    this.keyPassword = bundledKeyPassword
                }
            }
        }
    }

    defaultConfig {
        applicationId = "com.operit.hohyaiimage"
        minSdk = 24
        targetSdk = 34
        versionCode = 6
        versionName = "1.5"
    }

    buildTypes {
        debug {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("sharedRelease")
            }
        }
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("sharedRelease")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get() }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)
}
