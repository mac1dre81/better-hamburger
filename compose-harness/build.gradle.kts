plugins {
    id("com.android.test")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.dredio.textraocr.harness"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 24
        targetProjectPath = ":app"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.test.junit4)
    implementation(libs.androidx.junit)
    implementation(libs.androidx.test.core)
    implementation(libs.androidx.espresso.core)
    implementation(libs.billing.ktx)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
