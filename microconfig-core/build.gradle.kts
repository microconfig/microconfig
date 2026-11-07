plugins { `java-library` }

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    api(project(":microconfig-api"))
    api(project(":utils"))
    implementation(libs.jmustache)
    implementation(libs.spring.expression)
    implementation(libs.snakeyaml)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.mockito.core)
}
