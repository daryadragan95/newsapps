plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.newswinnerapp.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.newswinnerapp.app"
        minSdk = 24
        targetSdk = 35
        versionCode = providers.gradleProperty("versionCode")
            .orElse(providers.environmentVariable("VERSION_CODE"))
            .orElse("1")
            .get()
            .toInt()
        versionName = providers.gradleProperty("versionName")
            .orElse(providers.environmentVariable("VERSION_NAME"))
            .orElse("1.0")
            .get()

        val sportsDbApiKey = providers.gradleProperty("sportsDbApiKey").orElse("123").get()
        buildConfigField("String", "SPORTS_DB_API_KEY", "\"$sportsDbApiKey\"")
    }

    signingConfigs {
        create("release") {
            val keystorePath = providers.gradleProperty("android.injected.signing.store.file")
                .orElse(providers.environmentVariable("ANDROID_KEYSTORE_PATH"))
                .orNull
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = providers.gradleProperty("android.injected.signing.store.password")
                    .orElse(providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD"))
                    .orNull
                keyAlias = providers.gradleProperty("android.injected.signing.key.alias")
                    .orElse(providers.environmentVariable("ANDROID_KEY_ALIAS"))
                    .orNull
                keyPassword = providers.gradleProperty("android.injected.signing.key.password")
                    .orElse(providers.environmentVariable("ANDROID_KEY_PASSWORD"))
                    .orNull
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-firestore")

    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

val copyGoogleServicesToAssets by tasks.registering(Copy::class) {
    val configFile = layout.projectDirectory.file("google-services.json")
    from(configFile)
    into(layout.buildDirectory.dir("generated/firebaseAssets"))
    onlyIf { configFile.asFile.exists() }
}

android.sourceSets.getByName("main").assets.srcDir(layout.buildDirectory.dir("generated/firebaseAssets"))

tasks.matching { task ->
    task.name.startsWith("merge") && task.name.endsWith("Assets")
}.configureEach {
    dependsOn(copyGoogleServicesToAssets)
}

tasks.matching { task ->
    (task.name.startsWith("generate") && task.name.endsWith("LintReportModel")) ||
        task.name.startsWith("lintAnalyze")
}.configureEach {
    dependsOn(copyGoogleServicesToAssets)
}
