plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        freeCompilerArgs.add("-Xskip-metadata-version-check")
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "dev.cluvex.zedsecure.shared"
        compileSdk = 37
        minSdk = 24
    }
    jvm("desktop")

    sourceSets {
        val jvmMain = create("jvmMain") {
            dependsOn(getByName("commonMain"))
            dependencies {
                implementation(libs.bouncycastle)

                api(libs.jsch)

                implementation(libs.zxing.core)
            }
        }
        getByName("commonMain") {
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.ui)
                implementation(libs.compose.animation)
                implementation(libs.compose.components.resources)

                implementation(libs.compose.material3.mp)
                implementation(libs.material.kolor)
                implementation(libs.lifecycle.viewmodel.compose.mp)
                implementation(libs.lifecycle.runtime.compose.mp)
                implementation(libs.coil.compose)
                implementation(libs.coil.svg)
                implementation(libs.androidx.graphics.shapes)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.reorderable)
            }
        }
        getByName("androidMain") {
            dependsOn(jvmMain)
            dependencies {
                implementation(libs.kotlinx.coroutines.android)
                implementation(libs.androidx.compose.material3)

                implementation(libs.androidx.activity.compose)
            }
        }
        getByName("desktopMain") {
            dependsOn(jvmMain)
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}

configurations.configureEach {
    resolutionStrategy {
        force(
            "org.jetbrains.kotlin:kotlin-stdlib:${libs.versions.kotlin.get()}",
            "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${libs.versions.kotlin.get()}",
            "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${libs.versions.kotlin.get()}",
        )
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "dev.cluvex.zedsecure.shared.resources"
    generateResClass = always
}

tasks.register<Copy>("copyComposeResForAndroid") {
    dependsOn("prepareComposeResourcesTaskForCommonMain")
    from(layout.buildDirectory.dir("generated/compose/resourceGenerator/preparedResources/commonMain/composeResources"))
    into(layout.buildDirectory.dir("composeResForAndroid/composeResources/dev.cluvex.zedsecure.shared.resources"))
}

val zxComposeArtwork = mapOf(
        "zx_distressed" to "https://gcdn.picsart.com/picsart-mcp/cb632ab8-5804-4525-8dda-2f63e1e2509a.png",
        "zx_white" to "https://gcdn.picsart.com/picsart-mcp/0392c8f8-2bfb-44de-a1a8-6d4c912d4917.png",
        "zx_blade" to "https://gcdn.picsart.com/picsart-mcp/30e5a57e-c421-4944-a3b9-d3f4f00570f4.png",
        "zx_steel" to "https://gcdn.picsart.com/picsart-mcp/2991afb5-d4d6-4a8b-af0f-de8f2034dfe8.png",
        "zx_silver" to "https://gcdn.picsart.com/picsart-mcp/76ee4f2d-a85a-4e98-9ad7-683cb0a7799b.png",
        "zx_orange" to "https://gcdn.picsart.com/picsart-mcp/568668a6-5e76-48d4-bf4b-4f3fbfe4b973.png",
)

val downloadZxComposeArtwork = tasks.register("downloadZxComposeArtwork") {
    val outputDir = layout.projectDirectory.dir("src/commonMain/composeResources/drawable")
    outputs.files(zxComposeArtwork.keys.map { outputDir.file("$it.png") })
    doLast {
        outputDir.asFile.mkdirs()
        zxComposeArtwork.forEach { (name, url) ->
            val target = outputDir.file("$name.png").asFile
            if (!target.exists() || target.length() == 0L) {
                java.net.URL(url).openStream().use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
    }
}

tasks.matching { it.name == "prepareComposeResourcesTaskForCommonMain" }.configureEach {
    dependsOn(downloadZxComposeArtwork)
}
