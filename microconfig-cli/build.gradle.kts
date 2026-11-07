import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    application
    alias(libs.plugins.shadow)
}

application { mainClass.set("io.microconfig.MicroconfigMainKt") }

val generateVersionProperties =
    tasks.register("generateVersionProperties") {
        val projectVersion = project.version.toString()
        val versionFile =
            layout.buildDirectory.file("generated/resources/version/version.properties")
        inputs.property("version", projectVersion)
        outputs.file(versionFile)
        doLast {
            val resource = versionFile.get().asFile
            resource.parentFile.mkdirs()
            resource.writeText("version=$projectVersion\n", Charsets.UTF_8)
        }
    }

tasks.named<ProcessResources>("processResources") { from(generateVersionProperties) }

listOf("jar", "startScripts", "distZip", "distTar", "installDist").forEach { name ->
    tasks.named(name) { enabled = false }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    filesMatching("META-INF/*.kotlin_module") { duplicatesStrategy = DuplicatesStrategy.INCLUDE }
}

val javaComponent = components["java"] as AdhocComponentWithVariants

javaComponent.withVariantsFromConfiguration(configurations["apiElements"]) { skip() }

javaComponent.withVariantsFromConfiguration(configurations["runtimeElements"]) { skip() }

dependencies {
    implementation(project(":microconfig-core"))
    implementation(project(":utils"))
    testImplementation(libs.junit.jupiter.params)
}
