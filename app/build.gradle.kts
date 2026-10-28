import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// La clave de firma nunca vive en el repo. Se lee de variables de entorno (CI)
// o de ~/.sencillo/keystore.properties (PC de Augusto).
val firma: Map<String, String>? = run {
    val env = System.getenv()
    if (env["SENCILLO_KEYSTORE"] != null) {
        mapOf(
            "archivo" to env.getValue("SENCILLO_KEYSTORE"),
            "clave" to env.getValue("SENCILLO_KEYSTORE_CLAVE"),
            "alias" to env.getValue("SENCILLO_KEY_ALIAS"),
            "claveAlias" to env.getValue("SENCILLO_KEY_CLAVE"),
        )
    } else {
        val f = File(System.getProperty("user.home"), ".sencillo/keystore.properties")
        if (f.exists()) {
            val p = Properties().apply { f.inputStream().use { load(it) } }
            mapOf(
                "archivo" to p.getProperty("archivo"),
                "clave" to p.getProperty("clave"),
                "alias" to p.getProperty("alias"),
                "claveAlias" to p.getProperty("claveAlias"),
            )
        } else null
    }
}

android {
    namespace = "pe.sencillo"
    compileSdk = 34

    defaultConfig {
        applicationId = "pe.sencillo.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "0.1.1"
    }

    signingConfigs {
        if (firma != null) {
            create("release") {
                storeFile = file(firma.getValue("archivo"))
                storePassword = firma.getValue("clave")
                keyAlias = firma.getValue("alias")
                keyPassword = firma.getValue("claveAlias")
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
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
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "DebugProbesKt.bin")
    }
    dependenciesInfo {
        // No incrustar el bloque de dependencias cifrado con la clave de Google en el APK.
        includeInApk = false
        includeInBundle = false
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.biometric)

    testImplementation(libs.junit)
}
