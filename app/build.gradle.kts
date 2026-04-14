@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.tasks.PackageAndroidArtifact
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.lsplugin.apksign)
    alias(libs.plugins.lsplugin.resopt)
    id("kotlin-parcelize")
}

val androidCompileSdkVersion: Int by rootProject.extra
val androidCompileNdkVersion: String by rootProject.extra
val androidBuildToolsVersion: String by rootProject.extra
val androidMinSdkVersion: Int by rootProject.extra
val androidTargetSdkVersion: Int by rootProject.extra
val androidSourceCompatibility: JavaVersion by rootProject.extra
val androidTargetCompatibility: JavaVersion by rootProject.extra
val managerVersionCode: Int by rootProject.extra
val managerVersionName: String by rootProject.extra
val branchName: String by rootProject.extra
val kernelPatchVersion: String by rootProject.extra

apksign {
    storeFileProperty = "KEYSTORE_FILE"
    storePasswordProperty = "KEYSTORE_PASSWORD"
    keyAliasProperty = "KEY_ALIAS"
    keyPasswordProperty = "KEY_PASSWORD"
}

val ccache = System.getenv("PATH")?.split(File.pathSeparator)
    ?.map { File(it, "ccache") }?.firstOrNull { it.exists() }?.absolutePath

val baseFlags = listOf(
    "-Wall", "-Qunused-arguments", "-fno-rtti", "-fvisibility=hidden",
    "-fvisibility-inlines-hidden", "-fno-exceptions", "-fno-stack-protector",
    "-fomit-frame-pointer", "-Wno-builtin-macro-redefined", "-Wno-unused-value",
    "-D__FILE__=__FILE_NAME__",
    "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON", "-Wno-unused", "-Wno-unused-parameter",
    "-Wno-unused-command-line-argument", "-Wno-incompatible-function-pointer-types",
    "-U_FORTIFY_SOURCE", "-D_FORTIFY_SOURCE=0"
)

val baseArgs = mutableListOf(
    "-DANDROID_STL=none", "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON",
    "-DCMAKE_CXX_STANDARD=23", "-DCMAKE_C_STANDARD=23",
    "-DCMAKE_INTERPROCEDURAL_OPTIMIZATION=ON", "-DCMAKE_VISIBILITY_INLINES_HIDDEN=ON",
    "-DCMAKE_CXX_VISIBILITY_PRESET=hidden", "-DCMAKE_C_VISIBILITY_PRESET=hidden"
).apply { if (ccache != null) add("-DANDROID_CCACHE=$ccache") }

android {
    namespace = "me.bmax.apatch"

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            externalNativeBuild {
                cmake {
                    arguments += listOf("-DCMAKE_CXX_FLAGS_DEBUG=-Og", "-DCMAKE_C_FLAGS_DEBUG=-Og")
                }
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            multiDexEnabled = true
            vcsInfo.include = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            externalNativeBuild {
                cmake {
                    val relFlags = listOf(
                        "-flto", "-ffunction-sections", "-fdata-sections", "-Wl,--gc-sections",
                        "-fno-unwind-tables", "-fno-asynchronous-unwind-tables", "-Wl,--exclude-libs,ALL",
                        "-Ofast", "-fmerge-all-constants", "-flto=full", "-ffat-lto-objects",
                        "-fno-semantic-interposition", "-fno-threadsafe-statics"
                    )
                    cppFlags += relFlags
                    cFlags += relFlags
                    arguments += listOf("-DCMAKE_BUILD_TYPE=Release", "-DCMAKE_CXX_FLAGS_RELEASE=-O3 -DNDEBUG", "-DCMAKE_C_FLAGS_RELEASE=-O3 -DNDEBUG")
                }
            }
        }
    }

    dependenciesInfo.includeInApk = false

    buildFeatures {
        aidl = true
        buildConfig = true
        compose = true
        prefab = true
    }

    defaultConfig {
        applicationId = "com.kpatch.apatchpro"
        minSdk = androidMinSdkVersion
        targetSdk = androidTargetSdkVersion
        versionCode = managerVersionCode
        versionName = managerVersionName
        ndk.abiFilters.addAll(arrayOf("arm64-v8a"))
        externalNativeBuild {
            cmake {
                cppFlags += baseFlags + "-std=c++2b"
                cFlags += baseFlags + "-std=c2x"
                arguments += baseArgs
                abiFilters("arm64-v8a")
            }
        }
        buildConfigField("String", "buildKPV", "\"$kernelPatchVersion\"")
        base.archivesName = "APatchPro_${managerVersionCode}_${managerVersionName}_${branchName}"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "**"
            merges += "META-INF/com/google/android/**"
        }
    }

    externalNativeBuild {
        cmake {
            version = "3.28.0+"
            path("src/main/cpp/CMakeLists.txt")
        }
    }

    androidResources {
        generateLocaleConfig = true
    }

    compileSdk = androidCompileSdkVersion
    ndkVersion = androidCompileNdkVersion
    buildToolsVersion = androidBuildToolsVersion

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    android.sourceSets.named("main") {
        kotlin.directories += "build/generated/ksp/$name/kotlin"
        jniLibs.directories += "libs"
    }
}

// https://stackoverflow.com/a/77745844
tasks.withType<PackageAndroidArtifact> {
    doFirst { appMetadata.asFile.orNull?.writeText("") }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

fun gradlePathProp(name: String): File? {
    val raw = providers.gradleProperty(name).orNull?.trim().orEmpty()
    return if (raw.isNotEmpty()) File(raw) else null
}

fun firstExisting(candidates: List<File>): File? = candidates.firstOrNull { it.exists() }

val staticResourceFiles = listOf(
    File(project.projectDir, "src/main/assets/kpimg"),
    File(project.projectDir, "src/main/assets/kptools"),
    File(project.projectDir, "libs/arm64-v8a/libmagiskboot.so"),
    File(project.projectDir, "libs/arm64-v8a/libresetprop.so"),
)
val staticResourceSnapshot = mutableListOf<File>()

tasks.register("prepareLocalKpimg") {
    doLast {
        val assetKpimg = File(project.projectDir, "src/main/assets/kpimg")
        val localCandidates = listOfNotNull(
            gradlePathProp("APATCH_KPIMG"),
            assetKpimg,
        )
        val localKpimg = firstExisting(localCandidates)
            ?: throw GradleException(
                "kpimg not found. Put it at app/src/main/assets/kpimg or pass -PAPATCH_KPIMG=/absolute/path/to/kpimg"
            )

        assetKpimg.parentFile?.mkdirs()
        if (localKpimg.absolutePath != assetKpimg.absolutePath) {
            localKpimg.copyTo(assetKpimg, overwrite = true)
        }
        println(" - Using kpimg: ${localKpimg.absolutePath}")
    }
}

tasks.register("prepareLocalKptools") {
    doLast {
        val assetDest = File("${project.projectDir}/src/main/assets/kptools")
        val libDest = File("${project.projectDir}/libs/arm64-v8a/libkptools.so")
        val arm64Candidates = listOfNotNull(
            gradlePathProp("APATCH_KPTOOLS"),
            assetDest,
            libDest,
        )
        val localKptools = firstExisting(arm64Candidates)
            ?: throw GradleException(
                "kptools not found. Put it at app/src/main/assets/kptools or app/libs/arm64-v8a/libkptools.so, or pass -PAPATCH_KPTOOLS=/absolute/path/to/file"
            )

        assetDest.parentFile?.mkdirs()
        if (localKptools.absolutePath != assetDest.absolutePath) {
            localKptools.copyTo(assetDest, overwrite = true)
        }

        libDest.parentFile?.mkdirs()
        if (localKptools.absolutePath != libDest.absolutePath) {
            localKptools.copyTo(libDest, overwrite = true)
        }
        println(" - Using kptools: ${localKptools.absolutePath}")
    }
}

tasks.register("prepareLocalCompatKpatch") {
    doLast {
        val dest = File("${project.projectDir}/libs/arm64-v8a/libkpatch.so")
        val localCandidates = listOfNotNull(
            gradlePathProp("APATCH_COMPAT_KPATCH"),
            dest,
        )
        val localKpatch = firstExisting(localCandidates)
        if (localKpatch == null) {
            println(" - Compat kpatch not found, skip copy (optional).")
            return@doLast
        }

        if (localKpatch.absolutePath != dest.absolutePath) {
            dest.parentFile?.mkdirs()
            localKpatch.copyTo(dest, overwrite = true)
        }
        println(" - Using compat kpatch: ${localKpatch.absolutePath}")
    }
}

tasks.register("prepareLocalMagiskboot") {
    doLast {
        val dest = File("${project.projectDir}/libs/arm64-v8a/libmagiskboot.so")
        val localCandidates = listOf(
            gradlePathProp("APATCH_MAGISKBOOT"),
            dest,
        )
        val localMagiskboot = firstExisting(localCandidates.filterNotNull())
            ?: throw GradleException(
                "libmagiskboot.so not found. Put it at app/libs/arm64-v8a/libmagiskboot.so or pass -PAPATCH_MAGISKBOOT=/absolute/path/to/file"
            )

        if (localMagiskboot.absolutePath != dest.absolutePath) {
            dest.parentFile?.mkdirs()
            localMagiskboot.copyTo(dest, overwrite = true)
        }
        println(" - Using magiskboot: ${localMagiskboot.absolutePath}")
    }
}

tasks.register("prepareLocalResetprop") {
    doLast {
        val dest = File("${project.projectDir}/libs/arm64-v8a/libresetprop.so")
        val localCandidates = listOf(
            gradlePathProp("APATCH_RESETPROP"),
            dest,
        )
        val localResetprop = firstExisting(localCandidates.filterNotNull())
            ?: throw GradleException(
                "libresetprop.so not found. Put it at app/libs/arm64-v8a/libresetprop.so or pass -PAPATCH_RESETPROP=/absolute/path/to/file"
            )

        if (localResetprop.absolutePath != dest.absolutePath) {
            dest.parentFile?.mkdirs()
            localResetprop.copyTo(dest, overwrite = true)
        }
        println(" - Using resetprop: ${localResetprop.absolutePath}")
    }
}

tasks.register<Copy>("mergeScripts") {
    into("${project.projectDir}/src/main/resources/META-INF/com/google/android")
    from(rootProject.file("${project.rootDir}/scripts/update_binary.sh")) {
        rename { "update-binary" }
    }
    from(rootProject.file("${project.rootDir}/scripts/update_script.sh")) {
        rename { "updater-script" }
    }
}

tasks.getByName("preBuild").dependsOn(
    "prepareLocalKpimg",
    "prepareLocalKptools",
    "prepareLocalCompatKpatch",
    "prepareLocalMagiskboot",
    "prepareLocalResetprop",
    "mergeScripts",
)

// https://github.com/bbqsrc/cargo-ndk
// cargo ndk -t arm64-v8a build --release
tasks.register<Exec>("cargoBuild") {
    executable("cargo")
    args("ndk", "-t", "arm64-v8a", "build", "--release")
    workingDir("${project.rootDir}/apd")
}

tasks.register<Copy>("buildApd") {
    dependsOn("cargoBuild")
    from("${project.rootDir}/apd/target/aarch64-linux-android/release/apd")
    into("${project.projectDir}/libs/arm64-v8a")
    rename("apd", "libapd.so")
}

tasks.configureEach {
    if (name == "mergeDebugJniLibFolders" || name == "mergeReleaseJniLibFolders") {
        dependsOn("buildApd")
    }
}

tasks.register<Exec>("cargoClean") {
    executable("cargo")
    args("clean")
    workingDir("${project.rootDir}/apd")
}

tasks.register<Delete>("apdClean") {
    dependsOn("cargoClean")
    delete(file("${project.projectDir}/libs/arm64-v8a/libapd.so"))
}

tasks.clean {
    dependsOn("apdClean")
    doFirst {
        staticResourceSnapshot.clear()
        staticResourceSnapshot.addAll(staticResourceFiles.filter { it.exists() })
        if (staticResourceSnapshot.isEmpty()) {
            logger.warn(" - No static release resources found before clean; check your repository resources.")
        }
    }
    doLast {
        val removed = staticResourceSnapshot.filter { !it.exists() }
        if (removed.isNotEmpty()) {
            throw GradleException(
                "clean removed static release resources unexpectedly: ${removed.joinToString { it.path }}"
            )
        }
        println(" - Verified static resources preserved after clean.")
    }
}

ksp {
    arg("compose-destinations.defaultTransitions", "none")
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.biometric)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.runtime.livedata)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.compose.destinations.core)
    ksp(libs.compose.destinations.ksp)

    implementation(libs.com.github.topjohnwu.libsu.core)
    implementation(libs.com.github.topjohnwu.libsu.service)
    implementation(libs.com.github.topjohnwu.libsu.nio)
    implementation(libs.com.github.topjohnwu.libsu.io)

    implementation(libs.dev.rikka.rikkax.parcelablelist)

    implementation(libs.io.coil.kt.coil.compose)

    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.me.zhanghai.android.appiconloader.coil)

    implementation(libs.sheet.compose.dialogs.core)
    implementation(libs.sheet.compose.dialogs.list)
    implementation(libs.sheet.compose.dialogs.input)

    implementation(libs.markdown)

    implementation(libs.ini4j)

    compileOnly(libs.cxx)
}
