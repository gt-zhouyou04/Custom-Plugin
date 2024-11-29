import java.util.*

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.23"
    id("org.jetbrains.intellij") version "1.17.2"
    id("org.openjfx.javafxplugin").version("0.0.9")
}

group = "com.gaotu.plugin"
version = "1.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
//    implementation("org.openjfx:javafx-controls:17.0.2:${getOS()}-${getArchVersion()}")
//    implementation("org.openjfx:javafx-fxml:17.0.2:${getOS()}-${getArchVersion()}")
//    implementation("org.openjfx:javafx-web:17.0.2:${getOS()}-${getArchVersion()}")
//    implementation("org.openjfx:javafx-swing:17.0.2:${getOS()}-${getArchVersion()}")
    implementation("org.bytedeco:opencv:4.5.5-1.5.7")
    implementation("org.bytedeco:opencv-platform:4.5.5-1.5.7")

    implementation("org.bytedeco:javacv:1.5.6")
}

javafx {
    version = "17.0.2"
    modules("javafx.controls", "javafx.fxml", "javafx.web", "javafx.swing")
}

buildscript {
    repositories {
        maven {
            setUrl("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("org.openjfx:javafx-plugin:0.1.0")
    }
}
apply(plugin = "org.openjfx.javafxplugin")

intellij {
    version.set("2023.2.5")
//    type.set("AI") // Target IDE Platform
//    plugins.set(listOf("java"))
    plugins.set(listOf("com.intellij.java", "org.jetbrains.kotlin"))
    pluginName.set("Animation Preview")
}

tasks.withType<JavaCompile> {
    sourceCompatibility = JavaVersion.VERSION_17.toString()
    targetCompatibility = JavaVersion.VERSION_17.toString()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}

tasks.runIde {
    ideDir.set(file("/Applications/Android Studio.app/Contents"))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"

        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("231")
        untilBuild.set("242.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}

fun getOS(): String {
    val osName = System.getProperty("os.name").lowercase(Locale.getDefault())
    return when {
        osName.contains("win") -> "win"
        osName.contains("mac") -> "mac"
        osName.contains("nux") -> "linux"
        else -> throw IllegalArgumentException("Unsupported OS: $osName")
    }
}

fun getArchVersion(): String {
    val arch = System.getProperty("os.arch").lowercase(Locale.getDefault())
    return when {
        arch.contains("aarch64") -> "aarch64"
        arch.contains("amd64") || arch.contains("x86_64") -> "x86_64"
        else -> throw IllegalArgumentException("Unsupported architecture: $arch")
    }
}
