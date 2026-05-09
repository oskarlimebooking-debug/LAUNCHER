plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "com.oskar.retrolauncher"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.oskar.retrolauncher"
        minSdk = 23
        targetSdk = 28
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // OpenWeather API key from local.properties — never commit
        val owmKey = project.findProperty("OWM_API_KEY")?.toString().orEmpty()
        buildConfigField("String", "OWM_API_KEY", "\"$owmKey\"")

        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        create("platform") {
            // Placeholder: real keystore lands in T1.44 (system-build embedding).
            // Lazy-attach so a missing file doesn't fail config of other variants.
            val keystoreFile = rootProject.file("platform.keystore")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = (project.findProperty("PLATFORM_STORE_PASSWORD") as String?) ?: "android"
                keyAlias = (project.findProperty("PLATFORM_KEY_ALIAS") as String?) ?: "platform"
                keyPassword = (project.findProperty("PLATFORM_KEY_PASSWORD") as String?) ?: "android"
            }
        }
    }

    flavorDimensions += "build"
    productFlavors {
        create("standard") {
            dimension = "build"
            // No system uid, no embedding. v0.1 ships this flavor.
            buildConfigField("boolean", "ENABLE_EMBEDDING", "false")
        }
        create("system") {
            dimension = "build"
            // Reserved for v0.3. Manifest merges sharedUserId placeholder.
            buildConfigField("boolean", "ENABLE_EMBEDDING", "true")
            signingConfig = signingConfigs.getByName("platform")
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    lint {
        baseline = file("lint-baseline.xml")
        abortOnError = false
        warningsAsErrors = false
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    packaging {
        resources.excludes += listOf("META-INF/AL2.0", "META-INF/LGPL2.1", "META-INF/LICENSE.md", "META-INF/LICENSE-notice.md")
    }

    testOptions {
        // Required for Robolectric to inflate layouts/themes via the merged resource set.
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.coroutines.android)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.work)
    implementation(libs.androidx.palette)
    implementation(libs.androidx.preference)
    implementation(libs.material)

    implementation(libs.okhttp)
    implementation(libs.moshi)
    kapt(libs.moshi.codegen)
    implementation(libs.glide)
    kapt(libs.glide.compiler)

    implementation(libs.timber)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso)
}
