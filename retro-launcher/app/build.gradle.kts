plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    jacoco
}

jacoco {
    toolVersion = "0.8.11"
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

        // T1.36 — custom runner installs a TestServiceLocator before App.onCreate
        // so instrumented fragment tests never touch real network or sensors.
        testInstrumentationRunner = "com.oskar.retrolauncher.test.RetroTestRunner"

        // OpenWeather API key. Defaults to the owner's personal key so unsigned
        // builds work out of the box; can still be overridden via local.properties.
        val owmKey = project.findProperty("OWM_API_KEY")?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: "75306edb5e7e9293ed55f94f578db406"
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
            enableUnitTestCoverage = true
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

// Robolectric + Jacoco compatibility: include classes that lack source location
// (Robolectric's SandboxClassLoader synthesizes some at runtime) and skip
// internal JDK packages that the agent cannot instrument on Java 11+.
tasks.withType<Test>().configureEach {
    extensions.configure(JacocoTaskExtension::class) {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
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
    implementation(libs.osmdroid)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotest.property)
    testImplementation(libs.androidx.room.testing)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso)
    androidTestImplementation(libs.androidx.test.espresso.contrib)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.room.testing)
    // fragment-testing ships an empty debug-only test activity used by FragmentScenario.
    debugImplementation(libs.androidx.fragment.testing)
}

// T1.35: jacoco coverage for unit tests.
// Scope = StandardDebug variant (the one shipped in v0.1). Reports the line/branch
// coverage of the production sources covered by `:app:testStandardDebugUnitTest`.
tasks.register<JacocoReport>("jacocoTestReport") {
    group = "verification"
    description = "Coverage for :app:testStandardDebugUnitTest (line/branch)."
    dependsOn("testStandardDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    // T1.35 scope: repos + recorders + util extensions. Anything outside the AC
    // list (UI surfaces, system services, PackageManager-backed AppList, the
    // ~600 LoC SettingsStore wrapper) is exercised via instrumentation / QC and
    // is not counted here.
    val excludes = listOf(
        "**/R.class",
        "**/R\$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "**/*\$*\$*.class",
        "**/databinding/**/*.*",
        "**/android/databinding/**/*.*",
        "**/androidx/databinding/**/*.*",
        "**/*Binding.*",
        "**/*_Impl*.*",
        "**/Glide*.*",
        "**/*GlideModule*.*",
        // Out-of-scope production code (covered by instrumentation/QC, not unit tests).
        "**/ui/**",
        "**/MainActivity.*",
        "**/App.*",
        "**/ServiceLocator.*",
        "**/service/**",
        "**/data/apps/**",
        "**/data/prefs/**",
        "**/util/Permissions*",
        "**/util/UnitsFormatExt*",
        // Generated Moshi adapters.
        "**/*JsonAdapter.*",
    )

    val javaClasses = fileTree("${layout.buildDirectory.get()}/intermediates/javac/standardDebug/classes") {
        exclude(excludes)
    }
    val kotlinClasses = fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/standardDebug") {
        exclude(excludes)
    }
    classDirectories.setFrom(files(javaClasses, kotlinClasses))

    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    executionData.setFrom(
        fileTree(layout.buildDirectory.get()) {
            include("jacoco/testStandardDebugUnitTest.exec", "outputs/unit_test_code_coverage/**/*.exec")
        },
    )
}

// 80 % minimum line coverage on the same scope. Fails the build below the floor.
tasks.register<JacocoCoverageVerification>("jacocoCoverageVerification") {
    group = "verification"
    description = "Fails build when line coverage drops below 80 % on StandardDebug."
    dependsOn("jacocoTestReport")

    val excludes = listOf(
        "**/R.class",
        "**/R\$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "**/*\$*\$*.class",
        "**/databinding/**/*.*",
        "**/*Binding.*",
        "**/*_Impl*.*",
        "**/Glide*.*",
        "**/*GlideModule*.*",
        "**/ui/**",
        "**/MainActivity.*",
        "**/App.*",
        "**/ServiceLocator.*",
        "**/service/**",
        "**/*JsonAdapter.*",
    )

    val javaClasses = fileTree("${layout.buildDirectory.get()}/intermediates/javac/standardDebug/classes") {
        exclude(excludes)
    }
    val kotlinClasses = fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/standardDebug") {
        exclude(excludes)
    }
    classDirectories.setFrom(files(javaClasses, kotlinClasses))
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    executionData.setFrom(
        fileTree(layout.buildDirectory.get()) {
            include("jacoco/testStandardDebugUnitTest.exec", "outputs/unit_test_code_coverage/**/*.exec")
        },
    )

    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}
