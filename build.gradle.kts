import java.net.HttpURLConnection
import java.net.URI
import java.util.Base64
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.gradle.plugins.signing.SigningExtension
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.tasks.DokkaGeneratePublicationTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.dokka) apply false
}

val repositoryUser = providers.gradleProperty("ossrhUsername").orElse("")
val repositoryPassword = providers.gradleProperty("ossrhPassword").orElse("")

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "jacoco")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    group = "io.microconfig"
    version = rootProject.version

    repositories { mavenCentral() }

    extensions.configure<KotlinJvmProjectExtension> {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        withJavadocJar()
        withSourcesJar()
    }
    extensions.configure<DokkaExtension> {
        dokkaPublications.configureEach { offlineMode.set(gradle.startParameter.isOffline) }
        dokkaSourceSets.configureEach { jdkVersion.set(17) }
    }
    tasks.named<Jar>("javadocJar") {
        from(
            tasks.named<DokkaGeneratePublicationTask>("dokkaGeneratePublicationHtml").flatMap {
                it.outputDirectory
            }
        )
    }

    extensions.configure<PublishingExtension> {
        publications {
            create<MavenPublication>("nexus") {
                pom { configureMetadata() }
                from(components["java"])
            }
        }
        repositories {
            maven {
                url =
                    uri(
                        "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"
                    )
                credentials {
                    username = repositoryUser.get()
                    password = repositoryPassword.get()
                }
            }
        }
    }
    extensions.configure<SigningExtension> {
        if (!providers.gradleProperty("signing.secretKeyRingFile").orNull.isNullOrBlank()) {
            sign(extensions.getByType<PublishingExtension>().publications["nexus"])
        }
    }

    extensions.configure<JacocoPluginExtension> {
        toolVersion = rootProject.libs.versions.jacoco.get()
    }
    tasks.withType<JacocoReport>().configureEach {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        exclude("**/*IT.class")
    }
    tasks.register("sourceJar") { dependsOn(tasks.named<Jar>("sourcesJar")) }
}

subprojects {
    dependencies {
        add("testImplementation", platform(rootProject.libs.spring.boot.dependencies))
        add("testImplementation", rootProject.libs.junit.jupiter.api)
        add("testRuntimeOnly", rootProject.libs.junit.jupiter.engine)
        add("testRuntimeOnly", rootProject.libs.junit.platform.launcher)
    }
}

fun MavenPom.configureMetadata() {
    name.set("MicroConfig")
    description.set("Powerful tool for microservice configuration management")
    url.set("https://microconfig.io/")
    licenses {
        license {
            name.set("The Apache License, Version 2.0")
            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
        }
    }
    scm {
        connection.set("scm:git:https://github.com/microconfig/microconfig.git")
        developerConnection.set("scm:git:https://github.com/microconfig/microconfig.git")
        url.set("https://microconfig.io")
    }
    developers {
        developer {
            id.set("amatorin")
            name.set("Alexander Matorin")
            email.set("alx.matorin@gmail.com")
        }
        developer {
            id.set("esladkovski")
            name.set("Evgeni Sladkowski")
            email.set("kapodes@gmail.com")
        }
    }
}

tasks.register("transferToCentralPortal") {
    group = "publishing"
    description = "Transfers deployments uploaded to the staging API into Central Portal"
    val transferUrl =
        "https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/${project.group}"
    doLast {
        val credentials = "${repositoryUser.get()}:${repositoryPassword.get()}"
        val token = Base64.getEncoder().encodeToString(credentials.toByteArray(Charsets.UTF_8))
        val connection = URI.create(transferUrl).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Bearer $token")
        try {
            val status = connection.responseCode
            if (status >= 300) {
                val detail = connection.errorStream?.bufferedReader()?.use { it.readText() }
                throw GradleException("Central Portal transfer failed with HTTP $status: $detail")
            }
            logger.lifecycle(
                "Deployment transferred to Central Portal, publish it at https://central.sonatype.com/publishing/deployments"
            )
        } finally {
            connection.disconnect()
        }
    }
}
