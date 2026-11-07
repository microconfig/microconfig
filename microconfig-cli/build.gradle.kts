import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.api.tasks.Sync

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

tasks.test {
    inputs.file(rootProject.file(".github/scripts/set_gradle_properties.sh"))
    inputs.file(rootProject.file(".github/scripts/rotate_signing_passwords.sh"))
    inputs.file(rootProject.file(".github/scripts/native/verify-native.py"))
    inputs.file(rootProject.file("microconfig-cli/src/test/python/test_verify_native.py"))
    // NativeValidationScriptTest uses verify-native.py to read expression cases from this core test.
    // Track the source so changing those cases reruns CLI tests.
    inputs.file(rootProject.file("microconfig-core/src/test/kotlin/io/microconfig/core/properties/resolvers/expression/ExpressionResolverTest.kt"))
    val cliJar = tasks.named<ShadowJar>("shadowJar")
    dependsOn(cliJar)
    inputs.file(cliJar.flatMap { it.archiveFile })
    systemProperty("microconfig.cli.jar", cliJar.get().archiveFile.get().asFile.absolutePath)
}

listOf("jar", "startScripts", "distZip", "distTar", "installDist").forEach { name ->
    tasks.named(name) { enabled = false }
}

val nativeImageFeatureClasses = tasks.register<Sync>("nativeImageFeatureClasses") {
    from({ zipTree(configurations.runtimeClasspath.get().single { it.name.startsWith("spring-core-") }) }) {
        include("org/springframework/aot/nativex/feature/**")
    }
    into(layout.buildDirectory.dir("native-image-feature-classes"))
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    minimize()
    // Spring registers these classes by name in native-image.properties.
    sourceSetsClassesDirs.from(nativeImageFeatureClasses)
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
