plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    // JUnit 5 & Coroutines Testing para Reglas de Negocio en Kotlin Puro
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    useJUnitPlatform()
}
