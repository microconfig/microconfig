plugins { `java-library` }

dependencies {
    api(project(":microconfig-api"))
    api(project(":utils"))
    implementation(libs.jmustache)
    implementation(libs.spring.expression)
    implementation(libs.snakeyaml)
}
