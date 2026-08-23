import com.dao.catalog.version.shared.generateCatalogVersions

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.dao.catalog.version.shared"

    defaultConfig {
        versionCode = 1

        versionName = "1.0.0"
        applicationId = "com.dao.catalog.version.shared"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        project.generateCatalogVersions(::buildConfigField)
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(libs.android.core)
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.android.lifecycle.runtime)

    testImplementation(libs.test.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.test.compose.ui.junit4)
    androidTestImplementation(libs.test.espresso.core)
    androidTestImplementation(libs.android.test.junit)
    debugImplementation(libs.test.compose.ui.manifest)
    debugImplementation(libs.compose.ui.tooling)
}
