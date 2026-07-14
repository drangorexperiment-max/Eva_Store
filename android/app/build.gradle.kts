plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.evastore.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.evastore.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables { useSupportLibrary = true }

        // Встроенный ключ VirusTotal: берётся из GitHub-секрета VT_API_KEY
        // при сборке в CI. Если секрет не задан — пустая строка, и приложение
        // предлагает ввести ключ вручную либо открывает отчёт в браузере.
        buildConfigField(
            "String",
            "VT_API_KEY",
            "\"${System.getenv("VT_API_KEY") ?: ""}\""
        )
    }

    buildTypes {
        release {
            // Минификация отключена: R8 в release-сборке вызывал краш при запуске.
            isMinifyEnabled = false
            isShrinkResources = false
            // Debug-подпись, чтобы APK из CI сразу устанавливался.
            // Для публикации замените на собственный keystore.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coil.compose)
    implementation(libs.okhttp)
    // DNS-over-HTTPS: обходит DNS-подмену провайдеров, чтобы источники
    // работали без VPN. Фолбэк на системный DNS при недоступности.
    implementation(libs.okhttp.dnsoverhttps)
    implementation(libs.kotlinx.serialization.json)
}
