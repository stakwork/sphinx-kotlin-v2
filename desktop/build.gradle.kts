import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.FileInputStream
import java.util.*

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose") version "1.5.11"
}

group = "chat.sphinx"
version = "1.0"

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
    maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots")
    maven(url = "https://jogamp.org/deployment/maven")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
        }
        withJava()
    }

    sourceSets {
        val jvmMain by getting {
            val kmpTorBinaryVersion = "0.4.7.8"
            val korauVersion = "3.2.0"
            val korioVersion = "3.2.0"

            val osName = System.getProperty("os.name").toLowerCase()
            val osArch = System.getProperty("os.arch").toLowerCase()

            // Normalize architecture names
            val normalizedArch = when (osArch) {
                "amd64" -> "x86_64"
                else -> osArch
            }

            // Set the resources srcDir based on the OS and architecture
            val nativeResourceDir = when {
                "mac" in osName && normalizedArch == "aarch64" -> "src/jvmMain/resources/natives/macos/aarch64"
                "mac" in osName && normalizedArch == "x86_64" -> "src/jvmMain/resources/natives/macos/x86_64"
                "win" in osName && normalizedArch == "x86_64" -> "src/jvmMain/resources/natives/windows/x86_64"
                "win" in osName && normalizedArch == "i686" -> "src/jvmMain/resources/natives/windows/i686"
                "linux" in osName && normalizedArch == "x86_64" -> "src/jvmMain/resources/natives/linux/x86_64"
                "linux" in osName && normalizedArch == "x86" -> "src/jvmMain/resources/natives/linux/x86"
                "linux" in osName && normalizedArch == "arm64-v8a" -> "src/jvmMain/resources/natives/linux/arm64-v8a"
                "linux" in osName && normalizedArch == "armeabi-v7a" -> "src/jvmMain/resources/natives/linux/armeabi-v7a"
                else -> throw IllegalArgumentException("Unsupported OS/architecture combination: $osName, $osArch")
            }
            resources.srcDir(nativeResourceDir)

            dependencies {
                implementation(project(":common"))
                implementation(compose.desktop.currentOs)
                api(compose.preview)

                implementation("dev.datlag:kcef:2023.10.13")
                implementation("dev.datlag.tooling:tooling:1.1.0")
                implementation("io.matthewnelson.kotlin-components:kmp-tor-binary-linuxx64:$kmpTorBinaryVersion")
                implementation("io.matthewnelson.kotlin-components:kmp-tor-binary-macosx64:$kmpTorBinaryVersion")
                implementation("io.matthewnelson.kotlin-components:kmp-tor-binary-mingwx64:$kmpTorBinaryVersion")
                implementation("com.soywiz.korlibs.korio:korio:$korioVersion")
                implementation("com.soywiz.korlibs.korau:korau:$korauVersion")
                implementation("org.jetbrains.compose.ui:ui-graphics:1.5.1")
                implementation("uk.co.caprica:vlcj:4.7.1")
                implementation("net.java.dev.jna:jna:5.13.0")

//                implementation ("com.github.skydoves:landscapist-glide:1.3.6")
//                implementation ("io.coil-kt:coil-compose:1.4.0")
            }
        }
        val jvmTest by getting
    }
}

compose.desktop {

    application {
        mainClass = "MainKt"

        jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.desktop/sun.java2d=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED")

        if (System.getProperty("os.name").contains("Mac")) {
            jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
        }

        buildTypes {
            release {
                proguard {
                    configurationFiles.from(file("compose-desktop.pro"))
                }
            }
        }

        nativeDistributions {
            // Modules suggested by suggestRuntimeModules (avoids the ClassNotFoundException)
            modules("java.instrument", "java.management", "java.prefs", "java.sql", "jdk.unsupported")
            includeAllModules = true

            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Sphinx"
            packageVersion = "1.0.3"

            val sphinxProperties = Properties().apply {
                val localPropertiesFile = project.file("../local.properties")
                if (localPropertiesFile.exists()) {
                    load(FileInputStream(localPropertiesFile))
                }
            }

            val macOsBundleID = sphinxProperties.getProperty("macOs.bundleID")
            val macOsSigningIdentity = sphinxProperties.getProperty("macOs.signing.identity")

            macOS {
                if (macOsBundleID?.isNotEmpty() == true) {
                    bundleID = macOsBundleID
                }

                signing {
                    if (macOsSigningIdentity?.isNotEmpty() == true) {
                        sign.set(true)
                        identity.set(macOsSigningIdentity)
                    }
                }
                iconFile.set(project.file("sphinx-logo.icns"))
            }
            windows {
                iconFile.set(project.file("sphinx-logo-64.ico"))
                dirChooser = true
            }
            linux {
                iconFile.set(project.file("sphinx-logo.png"))
            }
        }
    }
}
