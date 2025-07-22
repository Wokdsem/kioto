plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidApplication)
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())

    androidTarget()
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "app"
        }
    }

}

android {
    namespace = "com.wokdsem.kioto.example"
    compileSdk = libs.versions.androidTargetSdk.get().toInt()

    defaultConfig {
        applicationId = "com.wokdsem.kioto.example"
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
}

private fun DependencyHandler.androidMainImplementation(dependencyNotation: Any) = add("androidMainImplementation", dependencyNotation)
dependencies {
    commonMainApi(project(":kioto"))
    commonMainImplementation(compose.runtime)
    commonMainImplementation(compose.foundation)
    commonMainImplementation(compose.material)
    commonMainImplementation(compose.material3)
    commonMainImplementation(compose.materialIconsExtended)
    commonMainImplementation(compose.components.uiToolingPreview)

    androidMainImplementation(libs.androidAppCompat)
    androidMainImplementation(libs.androidActivityCompose)
    androidMainImplementation(libs.androidMaterial)

    debugImplementation(compose.uiTooling)
}
