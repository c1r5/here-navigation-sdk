import org.jetbrains.kotlin.konan.properties.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "br.com.herenavigatesdk"
    compileSdk = 34

    val properties = Properties()
    properties.load(project.rootProject.file("local.properties").bufferedReader())

    defaultConfig {
        applicationId = "br.com.herenavigatesdk"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resValue("string", "HERE_USER_ID", properties.getProperty("here.user.id"))
        resValue("string", "HERE_CLIENT_ID", properties.getProperty("here.client.id"))
        resValue(
            "string",
            "HERE_ACCESS_KEY_ID",
            properties.getProperty("here.access.key.id")
        )
        resValue(
            "string",
            "HERE_ACCESS_KEY_SECRET",
            properties.getProperty("here.access.key.secret")
        )
        resValue(
            "string",
            "HERE_TOKEN_ENDPOINT_URL",
            properties.getProperty("here.token.endpoint.url")
        )
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    viewBinding {
        enable = true
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(project(":herenavigation"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.play.services.location)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.gson)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
