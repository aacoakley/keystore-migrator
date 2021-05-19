plugins {
    kotlin("multiplatform") version "1.5.0"
}

group = "me.acoakley"
version = "1.0"

repositories {
    mavenCentral()
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val KeystoreTarget = when {
        hostOs == "Mac OS X" -> macosX64("Keystore")
        hostOs == "Linux" -> linuxX64("Keystore") {
            compilations["main"].enableEndorsedLibs = true
        }
        isMingwX64 -> mingwX64("Keystore")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")

    }
    
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.2")
            }
        }
    }

    KeystoreTarget.apply {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }
    sourceSets {
        val KeystoreMain by getting
        val KeystoreTest by getting
    }
}
