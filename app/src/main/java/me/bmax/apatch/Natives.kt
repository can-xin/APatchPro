package me.bmax.apatch

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import dalvik.annotation.optimization.FastNative
import kotlinx.parcelize.Parcelize

object Natives {
    init {
        System.loadLibrary("apjni")
    }

    @Immutable
    @Parcelize
    @Keep
    data class Profile(
        var uid: Int = 0,
        var toUid: Int = 0,
        var scontext: String = APApplication.DEFAULT_SCONTEXT,
    ) : Parcelable

    @Immutable
    @Parcelize
    @Keep
    data class CapProfile(
        var uid: Int = 0,
        var enabled: Int = 0,
        var inheritable: Long = 0,
        var permitted: Long = 0,
        var effective: Long = 0,
        var bset: Long = 0,
        var ambient: Long = 0,
    ) : Parcelable

    @Keep
    class KPMCtlRes {
        var rc: Long = 0
        var outMsg: String? = null

        constructor()

        constructor(rc: Long, outMsg: String?) {
            this.rc = rc
            this.outMsg = outMsg
        }
    }


    @FastNative
    private external fun nativeSu(superKey: String, toUid: Int, scontext: String?): Long

    fun su(toUid: Int, scontext: String?): Boolean {
        return nativeSu(APApplication.superKey, toUid, scontext) == 0L
    }

    fun su(): Boolean {
        return su(0, "")
    }

    @FastNative
    external fun nativeReady(superKey: String): Boolean

    @FastNative
    private external fun nativeSuPath(superKey: String): String

    fun suPath(): String {
        return nativeSuPath(APApplication.superKey)
    }

    @FastNative
    private external fun nativeSuUids(superKey: String): IntArray

    fun suUids(): IntArray {
        return nativeSuUids(APApplication.superKey)
    }

    @FastNative
    private external fun nativeKernelPatchVersion(superKey: String): Long
    fun kernelPatchVersion(): Long {
        return nativeKernelPatchVersion(APApplication.superKey)
    }

    @FastNative
    private external fun nativeKernelPatchBuildTime(superKey: String): String
    fun kernelPatchBuildTime(): String {
        return nativeKernelPatchBuildTime(APApplication.superKey)
    }

    private external fun nativeLoadKernelPatchModule(
        superKey: String, modulePath: String, args: String
    ): Long

    fun loadKernelPatchModule(modulePath: String, args: String): Long {
        return nativeLoadKernelPatchModule(APApplication.superKey, modulePath, args)
    }

    fun loadKernelPatchModule(authKey: String, modulePath: String, args: String): Long {
        return nativeLoadKernelPatchModule(authKey, modulePath, args)
    }

    private external fun nativeUnloadKernelPatchModule(superKey: String, moduleName: String): Long
    fun unloadKernelPatchModule(moduleName: String): Long {
        return nativeUnloadKernelPatchModule(APApplication.superKey, moduleName)
    }

    fun unloadKernelPatchModule(authKey: String, moduleName: String): Long {
        return nativeUnloadKernelPatchModule(authKey, moduleName)
    }

    @FastNative
    private external fun nativeKernelPatchModuleNum(superKey: String): Long

    fun kernelPatchModuleNum(): Long {
        return nativeKernelPatchModuleNum(APApplication.superKey)
    }

    @FastNative
    private external fun nativeKernelPatchModuleList(superKey: String): String
    fun kernelPatchModuleList(): String {
        return nativeKernelPatchModuleList(APApplication.superKey)
    }

    @FastNative
    private external fun nativeKernelPatchModuleInfo(superKey: String, moduleName: String): String
    fun kernelPatchModuleInfo(moduleName: String): String {
        return nativeKernelPatchModuleInfo(APApplication.superKey, moduleName)
    }

    private external fun nativeControlKernelPatchModule(
        superKey: String, modName: String, jctlargs: String
    ): KPMCtlRes

    fun kernelPatchModuleControl(moduleName: String, controlArg: String): KPMCtlRes {
        return nativeControlKernelPatchModule(APApplication.superKey, moduleName, controlArg)
    }

    fun kernelPatchModuleControl(authKey: String, moduleName: String, controlArg: String): KPMCtlRes {
        return nativeControlKernelPatchModule(authKey, moduleName, controlArg)
    }

    @FastNative
    private external fun nativeGrantSu(
        superKey: String, uid: Int, toUid: Int, scontext: String?
    ): Long

    fun grantSu(uid: Int, toUid: Int, scontext: String?): Long {
        return nativeGrantSu(APApplication.superKey, uid, toUid, scontext)
    }

    fun grantSu(authKey: String, uid: Int, toUid: Int, scontext: String?): Long {
        return nativeGrantSu(authKey, uid, toUid, scontext)
    }

    @FastNative
    private external fun nativeRevokeSu(superKey: String, uid: Int): Long
    fun revokeSu(uid: Int): Long {
        return nativeRevokeSu(APApplication.superKey, uid)
    }

    fun revokeSu(authKey: String, uid: Int): Long {
        return nativeRevokeSu(authKey, uid)
    }

    @FastNative
    private external fun nativeSetUidExclude(superKey: String, uid: Int, exclude: Int): Int
    fun setUidExclude(uid: Int, exclude: Int): Int {
        return nativeSetUidExclude(APApplication.superKey, uid, exclude)
    }

    fun setUidExclude(authKey: String, uid: Int, exclude: Int): Int {
        return nativeSetUidExclude(authKey, uid, exclude)
    }

    @FastNative
    private external fun nativeGetUidExclude(superKey: String, uid: Int): Int
    fun isUidExcluded(uid: Int): Int {
        return nativeGetUidExclude(APApplication.superKey, uid)
    }

    @FastNative
    private external fun nativeSuProfile(superKey: String, uid: Int): Profile
    fun suProfile(uid: Int): Profile {
        return nativeSuProfile(APApplication.superKey, uid)
    }

    @FastNative
    private external fun nativeResetSuPath(superKey: String, path: String): Boolean
    fun resetSuPath(path: String): Boolean {
        return nativeResetSuPath(APApplication.superKey, path)
    }

    fun resetSuPath(authKey: String, path: String): Boolean {
        return nativeResetSuPath(authKey, path)
    }

    @FastNative
    private external fun nativePanic(superKey: String): Long

    fun panic(authKey: String): Long {
        return nativePanic(authKey)
    }

    @FastNative
    private external fun nativeSuCapProfile(superKey: String, uid: Int): CapProfile?
    fun suCapProfile(uid: Int): CapProfile? {
        return nativeSuCapProfile(APApplication.superKey, uid)
    }

    @FastNative
    private external fun nativeSetSuCapProfile(superKey: String, profile: CapProfile): Long
    fun setSuCapProfile(profile: CapProfile): Long {
        return nativeSetSuCapProfile(APApplication.superKey, profile)
    }

    fun setSuCapProfile(authKey: String, profile: CapProfile): Long {
        return nativeSetSuCapProfile(authKey, profile)
    }

    @FastNative
    private external fun nativeDeleteSuCapProfile(superKey: String, uid: Int): Long
    fun deleteSuCapProfile(uid: Int): Long {
        return nativeDeleteSuCapProfile(APApplication.superKey, uid)
    }

    fun deleteSuCapProfile(authKey: String, uid: Int): Long {
        return nativeDeleteSuCapProfile(authKey, uid)
    }
}
