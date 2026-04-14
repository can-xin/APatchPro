package me.bmax.apatch.util

import android.os.Parcelable
import android.util.Log
import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.apApp
import java.io.File
import java.io.FileWriter
import java.io.IOException
import kotlin.concurrent.thread

object PkgConfig {
    private const val TAG = "PkgConfig"

    private const val CSV_HEADER = "pkg,exclude,allow,uid,to_uid,sctx,cap_enabled,cap_inheritable,cap_permitted,cap_effective,cap_bset,cap_ambient"
    private const val DEFAULT_CAP_MASK: Long = (1L shl 39) - 1

    @Immutable
    @Keep
    data class PendingRollbackItem(
        val uid: Int,
        val appLabel: String,
        val pkg: String,
    )

    @Immutable
    @Parcelize
    @Keep
    data class Config(
        var pkg: String = "",
        var exclude: Int = 0,
        var allow: Int = 0,
        var profile: Natives.Profile,
        var capEnabled: Int = 0,
        var capInheritable: Long = DEFAULT_CAP_MASK,
        var capPermitted: Long = DEFAULT_CAP_MASK,
        var capEffective: Long = DEFAULT_CAP_MASK,
        var capBset: Long = DEFAULT_CAP_MASK,
        var capAmbient: Long = DEFAULT_CAP_MASK,
    ) : Parcelable {
        companion object {
            fun fromLine(line: String): Config {
                val sp = line.split(",")
                val profile = Natives.Profile(sp[3].toInt(), sp[4].toInt(), sp[5])
                return Config(
                    pkg = sp[0],
                    exclude = sp[1].toInt(),
                    allow = sp[2].toInt(),
                    profile = profile,
                    capEnabled = sp.getOrNull(6)?.toIntOrNull() ?: 0,
                    capInheritable = sp.getOrNull(7)?.toLongOrNull() ?: DEFAULT_CAP_MASK,
                    capPermitted = sp.getOrNull(8)?.toLongOrNull() ?: DEFAULT_CAP_MASK,
                    capEffective = sp.getOrNull(9)?.toLongOrNull() ?: DEFAULT_CAP_MASK,
                    capBset = sp.getOrNull(10)?.toLongOrNull() ?: DEFAULT_CAP_MASK,
                    capAmbient = sp.getOrNull(11)?.toLongOrNull() ?: DEFAULT_CAP_MASK,
                )
            }
        }

        fun hasCustomCapabilities(): Boolean {
            if (capEnabled != 0) return true
            return capInheritable != DEFAULT_CAP_MASK ||
                capPermitted != DEFAULT_CAP_MASK ||
                capEffective != DEFAULT_CAP_MASK ||
                capBset != DEFAULT_CAP_MASK ||
                capAmbient != DEFAULT_CAP_MASK
        }

        fun isDefault(): Boolean {
            return allow == 0 && exclude == 0 && !hasCustomCapabilities()
        }

        fun toLine(): String {
            return "${pkg},${exclude},${allow},${profile.uid},${profile.toUid},${profile.scontext},${capEnabled},${capInheritable},${capPermitted},${capEffective},${capBset},${capAmbient}"
        }
    }

    fun readConfigs(): HashMap<Int, Config> {
        val configs = HashMap<Int, Config>()
        val lines = runCatching {
            File(APApplication.PACKAGE_CONFIG_FILE).readLines()
        }.getOrElse {
            val result = rootShellForResult("cat ${APApplication.PACKAGE_CONFIG_FILE}")
            if (result.isSuccess) {
                result.out
            } else {
                emptyList()
            }
        }

        lines.drop(1).filter { it.isNotEmpty() }.forEach {
            Log.d(TAG, it)
            val p = Config.fromLine(it)
            if (!p.isDefault()) {
                configs[p.profile.uid] = p
            }
        }
        return configs
    }

    private fun writeConfigs(configs: HashMap<Int, Config>) {
        val tmpFile = File.createTempFile("package_config", ".tmp", apApp.cacheDir)
        runCatching {
            FileWriter(tmpFile, false).use { writer ->
                writer.write(CSV_HEADER + '\n')
                configs.values.forEach {
                    if (!it.isDefault()) {
                        writer.write(it.toLine() + '\n')
                    }
                }
            }
            val result = rootShellForResult(
                "mkdir -p ${APApplication.APATCH_FOLDER}",
                "cp ${tmpFile.absolutePath} ${APApplication.PACKAGE_CONFIG_FILE}",
                "chmod 600 ${APApplication.PACKAGE_CONFIG_FILE}",
                "chown 0:0 ${APApplication.PACKAGE_CONFIG_FILE}"
            )
            if (!result.isSuccess) {
                throw IOException("write package_config failed: ${result.out}")
            }
        }.onFailure {
            Log.e(TAG, "writeConfigs failed", it)
            throw it
        }.also {
            tmpFile.delete()
        }
    }

    fun changeConfig(config: Config) {
        thread {
            synchronized(PkgConfig.javaClass) {
                Natives.su()
                val configs = readConfigs()
                val uid = config.profile.uid
                // Root App should not be excluded
                if (config.allow == 1) {
                    config.exclude = 0
                }
                if (config.allow == 0 && configs[uid] != null && config.exclude != 0) {
                    configs.remove(uid)
                } else {
                    Log.d(TAG, "change config: $config")
                    configs[uid] = config
                }
                writeConfigs(configs)
            }
        }
    }

    fun getPendingRollbackList(): List<PendingRollbackItem> {
        val pm = apApp.packageManager
        return readConfigs().values
            .sortedBy { it.profile.uid }
            .map { config ->
                val uid = config.profile.uid
                val pkg = pm.getPackagesForUid(uid)?.firstOrNull() ?: config.pkg
                val label = runCatching {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    pm.getApplicationLabel(appInfo).toString()
                }.getOrElse { pkg }
                PendingRollbackItem(uid = uid, appLabel = label, pkg = pkg)
            }
    }

    fun hasKernelConfigDrift(): Boolean {
        return runCatching {
            synchronized(PkgConfig.javaClass) {
                val configs = readConfigs()
                val expectedAllow = configs.values
                    .filter { it.allow == 1 && it.exclude == 0 }
                    .associateBy { it.profile.uid }
                val actualAllow = Natives.suUids()
                    .filter { it != 0 && it != 2000 }
                    .toSet()

                if (actualAllow != expectedAllow.keys) {
                    return@synchronized true
                }

                for ((uid, config) in expectedAllow) {
                    val profile = Natives.suProfile(uid)
                    if (profile.toUid != config.profile.toUid || profile.scontext != config.profile.scontext) {
                        return@synchronized true
                    }
                }

                for ((uid, config) in configs) {
                    val expectedExclude = config.allow == 0 && config.exclude == 1
                    val actualExclude = Natives.isUidExcluded(uid) != 0
                    if (expectedExclude != actualExclude) {
                        return@synchronized true
                    }

                    val actualCap = Natives.suCapProfile(uid)
                    if (config.capEnabled != 0) {
                        if (actualCap == null) {
                            return@synchronized true
                        }
                        if (actualCap.enabled != config.capEnabled ||
                            actualCap.inheritable != config.capInheritable ||
                            actualCap.permitted != config.capPermitted ||
                            actualCap.effective != config.capEffective ||
                            actualCap.bset != config.capBset ||
                            actualCap.ambient != config.capAmbient
                        ) {
                            return@synchronized true
                        }
                    } else if (actualCap != null && actualCap.enabled != 0) {
                        return@synchronized true
                    }
                }

                false
            }
        }.getOrElse {
            Log.e(TAG, "hasKernelConfigDrift failed", it)
            true
        }
    }

    fun reconcileKernelFromConfig(authKey: String): Boolean {
        return runCatching {
            synchronized(PkgConfig.javaClass) {
                val configs = readConfigs()
                val expectedAllow = configs.values
                    .filter { it.allow == 1 && it.exclude == 0 }
                    .associateBy { it.profile.uid }
                val actualAllow = Natives.suUids()
                    .filter { it != 0 && it != 2000 }
                    .toSet()

                // Revoke stale allows that no longer exist in package_config.
                for (uid in actualAllow) {
                    if (!expectedAllow.containsKey(uid)) {
                        Natives.revokeSu(authKey, uid)
                    }
                }

                for ((uid, config) in configs) {
                    if (config.allow == 1 && config.exclude == 0) {
                        Natives.grantSu(authKey, uid, config.profile.toUid, config.profile.scontext)
                        Natives.setUidExclude(authKey, uid, 0)
                    } else {
                        if (actualAllow.contains(uid)) {
                            Natives.revokeSu(authKey, uid)
                        }
                        Natives.setUidExclude(authKey, uid, if (config.exclude == 1) 1 else 0)
                    }

                    if (config.capEnabled != 0) {
                        val profile = Natives.CapProfile(
                            uid = uid,
                            enabled = config.capEnabled,
                            inheritable = config.capInheritable,
                            permitted = config.capPermitted,
                            effective = config.capEffective,
                            bset = config.capBset,
                            ambient = config.capAmbient,
                        )
                        Natives.setSuCapProfile(authKey, profile)
                    } else {
                        Natives.deleteSuCapProfile(authKey, uid)
                    }
                }

                !hasKernelConfigDrift()
            }
        }.getOrElse {
            Log.e(TAG, "reconcileKernelFromConfig failed", it)
            false
        }
    }
}
