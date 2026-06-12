plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.example"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.whisper.notes"
    minSdk = 26
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    manifestPlaceholders["usesCleartextTraffic"] = "false"

    // Varsayılan sunucu URL'i — release build'de ortam değişkeninden okunur
    val serverUrl = System.getenv("WHISPER_SERVER_URL") ?: "https://your-server.railway.app"
    buildConfigField("String", "DEFAULT_SERVER_URL", "\"$serverUrl\"")
  }

  signingConfigs {
    create("release") {
      val ksPath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/release.jks"
      val ksPass = System.getenv("KEYSTORE_PASSWORD") ?: ""
      val kAlias = System.getenv("KEY_ALIAS") ?: "whisper-notes"
      val kPass  = System.getenv("KEY_PASSWORD") ?: ""
      if (file(ksPath).exists() && ksPass.isNotEmpty()) {
        storeFile = file(ksPath)
        storePassword = ksPass
        keyAlias = kAlias
        keyPassword = kPass
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      // Debug için: fiziksel cihazda ADB reverse ile localhost test
      val debugUrl = System.getenv("WHISPER_SERVER_URL") ?: "http://127.0.0.1:8000"
      buildConfigField("String", "DEFAULT_SERVER_URL", "\"$debugUrl\"")
      manifestPlaceholders["usesCleartextTraffic"] = "true"
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  kotlinOptions {
    jvmTarget = "11"
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.accompanist.permissions)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.okhttp)
  testImplementation("junit:junit:4.13.2")
  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
}
