import java.util.*

plugins {
    id("com.android.library")
    kotlin("multiplatform")
    id("org.jetbrains.compose") version "1.5.1"
}

group = "chat.sphinx"
version = "1.0"

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
    maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots")
}

// Load secrets from local.properties (for local dev)
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

// Task to generate ApiConfig
val generateApiConfig by tasks.registering {
    val outputDir = file("src/commonMain/kotlin/chat/sphinx/generated")

    doLast {
        val workflowIdStr = findProperty("WORKFLOW_ID")?.toString()
            ?: localProperties.getProperty("WORKFLOW_ID") ?: "0"
        val chaptersToken = findProperty("CHAPTERS_TOKEN")?.toString()
            ?: localProperties.getProperty("CHAPTERS_TOKEN") ?: ""

        val safeWorkflowId = workflowIdStr.toIntOrNull() ?: 0

        val safeToken = chaptersToken
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")

        val configContent = """
            package chat.sphinx.generated

            object ApiConfig {
                const val WORKFLOW_ID = $safeWorkflowId
                const val CHAPTERS_TOKEN = "$safeToken"
            }
        """.trimIndent()

        outputDir.mkdirs()
        file("${outputDir.path}/ApiConfig.kt").writeText(configContent)
    }
}

// Ensure ApiConfig is generated before compilation
tasks.matching { it.name.startsWith("compileKotlin") }.configureEach {
    dependsOn(generateApiConfig)
}


kotlin {
    androidTarget()
    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
        }
    }
    sourceSets["commonMain"].kotlin.srcDir("src/commonMain/kotlin/chat/sphinx/generated")

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
    }

    val javaFxVersion = "21.0.1"
    val arch = System.getProperty("os.arch")

    val platform = when {
        org.gradle.internal.os.OperatingSystem.current().isWindows -> "win"
        org.gradle.internal.os.OperatingSystem.current().isLinux -> "linux"
        org.gradle.internal.os.OperatingSystem.current().isMacOsX && arch == "aarch64" -> "mac-aarch64"
        org.gradle.internal.os.OperatingSystem.current().isMacOsX -> "mac"
        else -> throw GradleException("Unsupported OS")
    }

    sourceSets {
        val klockVersion = "2.5.1"
        val korauVersion = "3.2.0"
        val korioVersion = "3.2.0"

        val commonMain by getting {
            dependencies {
                implementation(compose.ui)
                implementation(compose.runtime)
                implementation(compose.foundation)
                api(compose.material)
                implementation(compose.materialIconsExtended)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.material3)
                api(project(":sphinx-kotlin-core"))
                implementation("com.soywiz.korlibs.klock:klock:$klockVersion")
                implementation("media.kamel:kamel-image:0.7.3")
                implementation("com.google.zxing:core:3.5.0")
                implementation("io.ktor:ktor-client-core:2.3.7")
                implementation("io.ktor:ktor-client-cio:2.3.7")
                implementation("com.russhwolf:multiplatform-settings:0.8.1")
                implementation("org.cryptonode.jncryptor:jncryptor:1.2.0")
                implementation("com.soywiz.korlibs.korio:korio:$korioVersion")
                implementation("com.soywiz.korlibs.korau:korau:$korauVersion")
                implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
                implementation("uk.co.caprica:vlcj:4.7.1")
                api("io.github.kevinnzou:compose-webview-multiplatform:1.8.0")
                implementation("org.openjfx:javafx-base:$javaFxVersion:$platform")
                implementation("org.openjfx:javafx-controls:$javaFxVersion:$platform")
                implementation("org.openjfx:javafx-graphics:$javaFxVersion:$platform")
                implementation("org.openjfx:javafx-media:$javaFxVersion:$platform")
                implementation("org.openjfx:javafx-swing:$javaFxVersion:$platform")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val androidMain by getting {
            dependencies {
                api("androidx.appcompat:appcompat:1.6.1")
                api("androidx.core:core-ktx:1.12.0")
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation("junit:junit:4.13")
            }
        }

        val desktopMain by getting {
            dependencies {
                api(compose.preview)
                api(compose.desktop.common)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                api(compose.desktop.components.splitPane)
            }
        }

        val desktopTest by getting
    }
}

android {
    compileSdk = 31
    sourceSets["main"].apply {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
        res.srcDirs("src/androidMain/res", "src/commonMain/resources")
    }
    defaultConfig {
        minSdk = 24
        targetSdk = 31
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.4")
    implementation("androidx.compose.ui:ui-text:1.5.4")
    implementation("com.google.android.material:material:1.10.0")
    implementation("io.coil-kt:coil-compose:1.4.0")
    implementation("androidx.compose.material:material:1.6.0-alpha08")
}
