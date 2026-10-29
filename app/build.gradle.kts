plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.playtank.databasemigration"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.playtank.databasemigration"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // -------------------------------------------------------------------------
    // Product flavors for the two-run emulator demo
    //
    //  crashDemo  – AppDatabase version = 1, entity already has `email`.
    //               Room detects an identity-hash mismatch and throws
    //               IllegalStateException on the first DB access.
    //
    //  fixedDemo  – AppDatabase version = 2, MIGRATION_1_2_FIXED registered.
    //               Install this AFTER crashDemo (same DB file name) to watch
    //               the migration run and data survive.
    // -------------------------------------------------------------------------
    flavorDimensions += "demo"
    productFlavors {
        create("crashDemo") {
            dimension = "demo"
            // No applicationIdSuffix – same package name as fixedDemo so both
            // flavors share the same app data sandbox on the emulator.
            // The crashDemo install writes v1 data; fixedDemo install migrates it.
            versionNameSuffix = "-crash"
        }
        create("fixedDemo") {
            dimension = "demo"
            versionNameSuffix = "-fixed"
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }

    // Export Room schema JSON for the fixedDemo flavor only.
    // crashDemo uses exportSchema = false so no path is needed for it.
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    // Make schemas available to MigrationTestHelper as test assets.
    sourceSets {
        getByName("androidTest").assets.srcDirs("$projectDir/schemas")
    }
}

dependencies {
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Compose + lifecycle
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Unit tests (JVM)
    testImplementation(libs.junit)

    // Instrumented tests (device/emulator)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}