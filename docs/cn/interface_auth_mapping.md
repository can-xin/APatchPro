# 接口分级映射表（以内核分类为准）

## 分类依据（唯一权威）
- 命令级鉴权分类：`get_cmd_auth_level`：`SC_AUTH_READ/WRITE/SU/DEFAULT`。
  - [KernelPatch-0.12.2副本2/kernel/patch/common/supercall.c#L48-L97](KernelPatch-0.12.2副本2/kernel/patch/common/supercall.c#L48-L97)
- 读鉴权：`auth_read_superkey(key)`（key1）。
  - [KernelPatch-0.12.2副本2/kernel/base/predata.c#L86-L91](KernelPatch-0.12.2副本2/kernel/base/predata.c#L86-L91)
- 写鉴权：`auth_write_superkey(key)`，核心先走 `auth_concat_write_key`（key1+key2）。
  - [KernelPatch-0.12.2副本2/kernel/base/predata.c#L22-L70](KernelPatch-0.12.2副本2/kernel/base/predata.c#L22-L70)
- 超级调用入口处读/写/SU判定：
  - [KernelPatch-0.12.2副本2/kernel/patch/common/supercall.c#L502-L514](KernelPatch-0.12.2副本2/kernel/patch/common/supercall.c#L502-L514)

## 方法级映射（Natives -> JNI -> SUPERCALL -> 内核分类）
| Natives 方法 | JNI/sc 调用 | SUPERCALL | 分类 | 说明 |
|---|---|---|---|---|
| CapProfile | N/A | N/A | N/A | 数据结构构造，不是内核调用 |
| Profile | N/A | N/A | N/A | 数据结构构造，不是内核调用 |
| deleteSuCapProfile | sc_su_cap_del_profile | SUPERCALL_SU_CAP_DEL_PROFILE | WRITE | Capabilities 删除 |
| getSafeMode | N/A | N/A | N/A | 注释行，当前无实现调用 |
| grantSu | sc_su_grant_uid | SUPERCALL_SU_GRANT_UID | WRITE | 授权写入 |
| isUidExcluded | sc_get_ap_mod_exclude -> sc_kstorage_read | SUPERCALL_KSTORAGE_READ | READ | 排除标记读取 |
| kernelPatchBuildTime | sc_get_build_time | SUPERCALL_BUILD_TIME | READ | 编译时间读取 |
| kernelPatchModuleControl | sc_kpm_control | SUPERCALL_KPM_CONTROL | WRITE | KPM 控制 |
| kernelPatchModuleInfo | sc_kpm_info | SUPERCALL_KPM_INFO | READ | KPM 信息读取 |
| kernelPatchModuleList | sc_kpm_list | SUPERCALL_KPM_LIST | READ | KPM 列表读取 |
| kernelPatchModuleNum | sc_kpm_nums | SUPERCALL_KPM_NUMS | READ | KPM 数量读取 |
| kernelPatchVersion | sc_kp_ver | SUPERCALL_KERNELPATCH_VER | READ | 版本读取 |
| loadKernelPatchModule | sc_kpm_load | SUPERCALL_KPM_LOAD | WRITE | KPM 加载 |
| nativeReady | sc_ready -> sc_hello | SUPERCALL_HELLO | READ | 内核可用性探测 |
| panic | sc_panic | SUPERCALL_PANIC | WRITE | 触发内核恐慌 |
| resetSuPath | sc_su_reset_path | SUPERCALL_SU_RESET_PATH | WRITE | su 路径写入 |
| revokeSu | sc_su_revoke_uid | SUPERCALL_SU_REVOKE_UID | WRITE | 授权移除 |
| setSuCapProfile | sc_su_cap_set_profile | SUPERCALL_SU_CAP_SET_PROFILE | WRITE | Capabilities 写入 |
| setUidExclude | sc_set_ap_mod_exclude -> sc_kstorage_write/remove | SUPERCALL_KSTORAGE_WRITE/REMOVE | WRITE | 排除标记写入/移除 |
| su | sc_su | SUPERCALL_SU | SU | 白名单路径（按你的要求不纳入读写策略） |
| suCapProfile | sc_su_cap_get_profile | SUPERCALL_SU_CAP_GET_PROFILE | READ | Capabilities 读取 |
| suPath | sc_su_get_path | SUPERCALL_SU_GET_PATH | READ | su 路径读取 |
| suProfile | sc_su_uid_profile | SUPERCALL_SU_PROFILE | READ | 授权配置读取 |
| suUids | sc_su_uid_nums + sc_su_allow_uids | SUPERCALL_SU_NUMS + SUPERCALL_SU_LIST | READ | 授权 uid 列表读取 |
| unloadKernelPatchModule | sc_kpm_unload | SUPERCALL_KPM_UNLOAD | WRITE | KPM 卸载 |

## 管理器调用点全集（APatch-official）
| 方法 | 分类 | 调用点 | 代码片段 | 风险提示 |
|---|---|---|---|---|
| CapProfile | N/A | [app/src/main/java/me/bmax/apatch/ui/screen/SuperUser.kt#L591](app/src/main/java/me/bmax/apatch/ui/screen/SuperUser.kt#L591) | val profile = Natives.CapProfile( | 非接口调用 |
| Profile | N/A | [app/src/main/java/me/bmax/apatch/ui/viewmodel/SuperUserViewModel.kt#L144](app/src/main/java/me/bmax/apatch/ui/viewmodel/SuperUserViewModel.kt#L144) | uid, PkgConfig.Config(appInfo.packageName, Natives.isUidExcluded(uid), 0, Natives.Profile(uid = uid)) | 非接口调用 |
| Profile | N/A | [app/src/main/java/me/bmax/apatch/util/PkgConfig.kt#L28](app/src/main/java/me/bmax/apatch/util/PkgConfig.kt#L28) | val profile = Natives.Profile(sp[3].toInt(), sp[4].toInt(), sp[5]) | 非接口调用 |
| deleteSuCapProfile | WRITE | [app/src/main/java/me/bmax/apatch/ui/screen/SuperUser.kt#L575](app/src/main/java/me/bmax/apatch/ui/screen/SuperUser.kt#L575) | val rc = Natives.deleteSuCapProfile(authKey, app.uid) | 必须走 key1+key2（compose 后 authKey） |
| getSafeMode | N/A | [app/src/main/java/me/bmax/apatch/ui/screen/APM.kt#L149](app/src/main/java/me/bmax/apatch/ui/screen/APM.kt#L149) | //TODO: FIXME -> val isSafeMode = Natives.getSafeMode() | 非接口调用 |
| grantSu | WRITE | [app/src/main/java/me/bmax/apatch/ui/screen/SuperUser.kt#L334](app/src/main/java/me/bmax/apatch/ui/screen/SuperUser.kt#L334) | Natives.grantSu(authKey, app.uid, 0, config.profile.scontext) | 必须走 key1+key2（compose 后 authKey） |
| isUidExcluded | READ | [app/src/main/java/me/bmax/apatch/ui/viewmodel/SuperUserViewModel.kt#L144](app/src/main/java/me/bmax/apatch/ui/viewmodel/SuperUserViewModel.kt#L144) | uid, PkgConfig.Config(appInfo.packageName, Natives.isUidExcluded(uid), 0, Natives.Profile(uid = uid)) | 必须仅走 key1（read key） |
| kernelPatchBuildTime | READ | [app/src/main/java/me/bmax/apatch/util/Version.kt#L92](app/src/main/java/me/bmax/apatch/util/Version.kt#L92) | val time = Natives.kernelPatchBuildTime() | 必须仅走 key1（read key） |
| kernelPatchModuleControl | WRITE | [app/src/main/java/me/bmax/apatch/ui/screen/KPM.kt#L380](app/src/main/java/me/bmax/apatch/ui/screen/KPM.kt#L380) | controlResult = Natives.kernelPatchModuleControl(authKey, module.name, controlParam) | 必须走 key1+key2（compose 后 authKey） |
| kernelPatchModuleInfo | READ | [app/src/main/java/me/bmax/apatch/ui/viewmodel/KPModuleViewModel.kt#L63](app/src/main/java/me/bmax/apatch/ui/viewmodel/KPModuleViewModel.kt#L63) | val infoline = Natives.kernelPatchModuleInfo(it) | 必须仅走 key1（read key） |
| kernelPatchModuleList | READ | [app/src/main/java/me/bmax/apatch/ui/viewmodel/KPModuleViewModel.kt#L57](app/src/main/java/me/bmax/apatch/ui/viewmodel/KPModuleViewModel.kt#L57) | var names = Natives.kernelPatchModuleList() | 必须仅走 key1（read key） |
| kernelPatchModuleNum | READ | [app/src/main/java/me/bmax/apatch/ui/viewmodel/KPModuleViewModel.kt#L58](app/src/main/java/me/bmax/apatch/ui/viewmodel/KPModuleViewModel.kt#L58) | if (Natives.kernelPatchModuleNum() <= 0) | 必须仅走 key1（read key） |
| kernelPatchVersion | READ | [app/src/main/java/me/bmax/apatch/util/Version.kt#L109](app/src/main/java/me/bmax/apatch/util/Version.kt#L109) | return Natives.kernelPatchVersion().toUInt() | 必须仅走 key1（read key） |
| loadKernelPatchModule | WRITE | [app/src/main/java/me/bmax/apatch/ui/screen/KPM.kt#L351](app/src/main/java/me/bmax/apatch/ui/screen/KPM.kt#L351) | rc = Natives.loadKernelPatchModule(authKey, kpm.path, args).toInt() | 必须走 key1+key2（compose 后 authKey） |
| nativeReady | READ | [app/src/main/java/me/bmax/apatch/APatchApp.kt#L224](app/src/main/java/me/bmax/apatch/APatchApp.kt#L224) | val ready = Natives.nativeReady(value) | 必须仅走 key1（read key） |
| nativeReady | READ | [app/src/main/java/me/bmax/apatch/ui/screen/Home.kt#L422](app/src/main/java/me/bmax/apatch/ui/screen/Home.kt#L422) | val preVerifyKey = Natives.nativeReady(key) | 必须仅走 key1（read key） |
| panic | WRITE | [app/src/main/java/me/bmax/apatch/ui/screen/Settings.kt#L719](app/src/main/java/me/bmax/apatch/ui/screen/Settings.kt#L719) | val rc = Natives.panic(APApplication.composeWriteKey(panicKey2)) | 必须走 key1+key2（compose 后 authKey） |
| resetSuPath | WRITE | [app/src/main/java/me/bmax/apatch/ui/screen/Settings.kt#L824](app/src/main/java/me/bmax/apatch/ui/screen/Settings.kt#L824) | val success = Natives.resetSuPath(authKey, suPath) | 必须走 key1+key2（compose 后 authKey） |
| revokeSu | WRITE | [app/src/main/java/me/bmax/apatch/ui/screen/SuperUser.kt#L280](app/src/main/java/me/bmax/apatch/ui/screen/SuperUser.kt#L280) | Natives.revokeSu(authKey, app.uid) | 必须走 key1+key2（compose 后 authKey） |
| revokeSu | WRITE | [app/src/main/java/me/bmax/apatch/ui/screen/SuperUser.kt#L340](app/src/main/java/me/bmax/apatch/ui/screen/SuperUser.kt#L340) | Natives.revokeSu(authKey, app.uid) | 必须走 key1+key2（compose 后 authKey） |
| revokeSu | WRITE | [app/src/main/java/me/bmax/apatch/ui/screen/SuperUser.kt#L365](app/src/main/java/me/bmax/apatch/ui/screen/SuperUser.kt#L365) | Natives.revokeSu(authKey, app.uid) | 必须走 key1+key2（compose 后 authKey） |
| setSuCapProfile | WRITE | [app/src/main/java/me/bmax/apatch/ui/screen/SuperUser.kt#L601](app/src/main/java/me/bmax/apatch/ui/screen/SuperUser.kt#L601) | val rc = Natives.setSuCapProfile(authKey, profile) | 必须走 key1+key2（compose 后 authKey） |
| setUidExclude | WRITE | [app/src/main/java/me/bmax/apatch/ui/screen/SuperUser.kt#L335](app/src/main/java/me/bmax/apatch/ui/screen/SuperUser.kt#L335) | Natives.setUidExclude(authKey, app.uid, 0) | 必须走 key1+key2（compose 后 authKey） |
| setUidExclude | WRITE | [app/src/main/java/me/bmax/apatch/ui/screen/SuperUser.kt#L373](app/src/main/java/me/bmax/apatch/ui/screen/SuperUser.kt#L373) | Natives.setUidExclude(authKey, app.uid, excludeApp) | 必须走 key1+key2（compose 后 authKey） |
| su | SU | [app/src/main/java/me/bmax/apatch/APatchApp.kt#L235](app/src/main/java/me/bmax/apatch/APatchApp.kt#L235) | val rc = Natives.su(0, null) | 白名单路径，不纳入读写策略 |
| su | SU | [app/src/main/java/me/bmax/apatch/ui/viewmodel/SuperUserViewModel.kt#L133](app/src/main/java/me/bmax/apatch/ui/viewmodel/SuperUserViewModel.kt#L133) | Natives.su() | 白名单路径，不纳入读写策略 |
| su | SU | [app/src/main/java/me/bmax/apatch/util/PkgConfig.kt#L74](app/src/main/java/me/bmax/apatch/util/PkgConfig.kt#L74) | Natives.su() | 白名单路径，不纳入读写策略 |
| suCapProfile | READ | [app/src/main/java/me/bmax/apatch/ui/screen/SuperUser.kt#L438](app/src/main/java/me/bmax/apatch/ui/screen/SuperUser.kt#L438) | val profile = Natives.suCapProfile(app.uid) | 必须仅走 key1（read key） |
| suPath | READ | [app/src/main/java/me/bmax/apatch/ui/screen/Home.kt#L955](app/src/main/java/me/bmax/apatch/ui/screen/Home.kt#L955) | InfoCardItem(stringResource(R.string.home_su_path), Natives.suPath()) | 必须仅走 key1（read key） |
| suPath | READ | [app/src/main/java/me/bmax/apatch/ui/screen/Settings.kt#L819](app/src/main/java/me/bmax/apatch/ui/screen/Settings.kt#L819) | var suPath by remember { mutableStateOf(Natives.suPath()) } | 必须仅走 key1（read key） |
| suProfile | READ | [app/src/main/java/me/bmax/apatch/ui/viewmodel/SuperUserViewModel.kt#L142](app/src/main/java/me/bmax/apatch/ui/viewmodel/SuperUserViewModel.kt#L142) | val actProfile = if (uids.contains(uid)) Natives.suProfile(uid) else null | 必须仅走 key1（read key） |
| suUids | READ | [app/src/main/java/me/bmax/apatch/ui/viewmodel/SuperUserViewModel.kt#L128](app/src/main/java/me/bmax/apatch/ui/viewmodel/SuperUserViewModel.kt#L128) | val uids = Natives.suUids().toList() | 必须仅走 key1（read key） |
| unloadKernelPatchModule | WRITE | [app/src/main/java/me/bmax/apatch/ui/screen/KPM.kt#L520](app/src/main/java/me/bmax/apatch/ui/screen/KPM.kt#L520) | Natives.unloadKernelPatchModule(authKey, module.name) == 0L | 必须走 key1+key2（compose 后 authKey） |

## 已知静默风险点（当前仓库）
- `Natives.getSafeMode()` 仅存在注释，不是有效接口调用：
  - [app/src/main/java/me/bmax/apatch/ui/screen/APM.kt#L149](app/src/main/java/me/bmax/apatch/ui/screen/APM.kt#L149)
- 如后续新增 `Natives.*` 方法，必须先补本表再接入 UI，避免上层名称误导。