import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform

group = "com.wokdsem.kioto"

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.mavenPublish)
}

kotlin {
    explicitApi()

    androidTarget()
    iosArm64()
    iosX64()
    iosSimulatorArm64()

    applyDefaultHierarchyTemplate()
}

android {
    namespace = "com.wokdsem.kioto.kioto"
    compileSdk = libs.versions.androidTargetSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
    }
}

private fun DependencyHandler.androidMainImplementation(dependencyNotation: Any) = add("androidMainImplementation", dependencyNotation)

dependencies {
    commonMainImplementation(compose.runtime)
    commonMainImplementation(compose.foundation)
    commonMainImplementation(libs.kotlinCoroutines)
    androidMainImplementation(libs.androidAppCompat)
    androidMainImplementation(libs.androidActivityCompose)
}

mavenPublishing {
    configure(
        KotlinMultiplatform(
            javadocJar = JavadocJar.None(),
            sourcesJar = false,
            androidVariantsToPublish = listOf("release"),
        )
    )
}
