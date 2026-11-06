plugins { `java-library` }

dependencies {
    api(project(":microconfig-api"))
    implementation(project(":utils"))
    implementation(libs.jmustache)
    implementation(libs.spring.expression)
    implementation(libs.snakeyaml)
}
