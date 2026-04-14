plugins {
    alias(libs.plugins.agp.app) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.kotlin.compose.compiler) apply false
}

project.ext.set("kernelPatchVersion", "0.13.0")

val androidMinSdkVersion by extra(26)
val androidTargetSdkVersion by extra(36)
val androidCompileSdkVersion by extra(36)
val androidBuildToolsVersion by extra("36.1.0")
val androidCompileNdkVersion by extra("29.0.14206865")
val managerVersionCode by extra(1)
val managerVersionName by extra("1.0")
val branchName by extra(getBranch())
fun Project.exec(command: String): String {
    return try {
        providers.exec {
            commandLine(command.split(" "))
        }.standardOutput.asText.get().trim()
    } catch (_: Exception) {
        ""
    }
}

fun getGitCommitCount(): Int {
    return exec("git rev-list --count HEAD").trim().toIntOrNull() ?: 0
}

fun getGitDescribe(): String {
    return exec("git rev-parse --verify --short HEAD").trim().ifBlank { "nogit" }
}

fun getVersionCode(): Int {
    return 1
}

fun getBranch(): String {
    return exec("git rev-parse --abbrev-ref HEAD").trim().ifBlank { "local" }
}

fun getVersionName(): String {
    return "1.0"
}

tasks.register("printVersion") {
    doLast {
        println("Version code: $managerVersionCode")
        println("Version name: $managerVersionName")
    }
}
