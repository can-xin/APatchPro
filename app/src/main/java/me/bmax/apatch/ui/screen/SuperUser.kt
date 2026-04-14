package me.bmax.apatch.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import kotlinx.coroutines.launch
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.component.ProvideMenuShape
import me.bmax.apatch.ui.component.SearchAppBar
import me.bmax.apatch.ui.component.SwitchItem
import me.bmax.apatch.ui.viewmodel.SuperUserViewModel
import me.bmax.apatch.util.PkgConfig


@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SuperUserScreen() {
    val viewModel = viewModel<SuperUserViewModel>()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showCapDialog by remember { mutableStateOf(false) }
    var selectedCapApp by remember { mutableStateOf<SuperUserViewModel.AppInfo?>(null) }
    var showWriteKeyDialog by remember { mutableStateOf(false) }
    var writeKey2 by remember { mutableStateOf("") }
    var writeKey2Visible by remember { mutableStateOf(false) }
    var pendingWriteAction by remember { mutableStateOf<((String) -> Unit)?>(null) }

    fun requireWriteAuth(action: (String) -> Unit) {
        pendingWriteAction = action
        writeKey2 = ""
        showWriteKeyDialog = true
    }

    LaunchedEffect(Unit) {
        if (viewModel.appList.isEmpty()) {
            viewModel.fetchAppList()
        }
    }

    Scaffold(
        topBar = {
            SearchAppBar(
                searchText = viewModel.search,
                onSearchTextChange = { viewModel.search = it },
                searchBarPlaceHolderText = stringResource(R.string.search_apps),
                dropdownContent = {
                    var showDropdown by remember { mutableStateOf(false) }

                    IconButton(
                        onClick = { showDropdown = true },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(id = R.string.settings)
                        )

                        ProvideMenuShape(RoundedCornerShape(10.dp)) {
                            DropdownMenu(expanded = showDropdown, onDismissRequest = {
                                showDropdown = false
                            }) {
                                DropdownMenuItem(text = {
                                    Text(stringResource(R.string.su_refresh))
                                }, onClick = {
                                    scope.launch {
                                        viewModel.fetchAppList()
                                    }
                                    showDropdown = false
                                })

                                DropdownMenuItem(text = {
                                    Text(
                                        if (viewModel.showSystemApps) {
                                            stringResource(R.string.su_hide_system_apps)
                                        } else {
                                            stringResource(R.string.su_show_system_apps)
                                        }
                                    )
                                }, onClick = {
                                    viewModel.showSystemApps = !viewModel.showSystemApps
                                    showDropdown = false
                                })
                            }
                        }
                    }
                }
            )
        },
    ) { innerPadding ->

        PullToRefreshBox(
            modifier = Modifier.padding(innerPadding),
            onRefresh = { scope.launch { viewModel.fetchAppList() } },
            isRefreshing = viewModel.isRefreshing
        ) {
            LazyColumn(Modifier.fillMaxSize()) {
                items(viewModel.appList.filter { it.packageName != apApp.packageName }, key = { it.packageName + it.uid }) { app ->
                    AppItem(
                        app = app,
                        onLongClick = {
                            selectedCapApp = app
                            showCapDialog = true
                        },
                        onRequireWriteAuth = ::requireWriteAuth
                    )
                }
            }
        }
    }

    if (showCapDialog && selectedCapApp != null) {
        CapabilitiesDialog(
            app = selectedCapApp!!,
            onDismiss = { showCapDialog = false },
            onError = {
                scope.launch {
                    android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            onSaved = {
                scope.launch {
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.su_cap_saved),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                showCapDialog = false
            },
            onRequireWriteAuth = ::requireWriteAuth
        )
    }

    if (showWriteKeyDialog) {
        BasicAlertDialog(
            onDismissRequest = {
                showWriteKeyDialog = false
                pendingWriteAction = null
            },
            properties = DialogProperties(
                decorFitsSystemWindows = true,
                usePlatformDefaultWidth = false,
            )
        ) {
            Surface(
                modifier = Modifier
                    .width(320.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(20.dp),
                tonalElevation = AlertDialogDefaults.TonalElevation,
                color = AlertDialogDefaults.containerColor,
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = stringResource(R.string.key2_dialog_title))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = writeKey2,
                        onValueChange = { writeKey2 = it },
                        label = { Text(stringResource(R.string.patch_set_key2)) },
                        visualTransformation = if (writeKey2Visible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            showWriteKeyDialog = false
                            pendingWriteAction = null
                        }) {
                            Text(stringResource(id = android.R.string.cancel))
                        }
                        Button(onClick = {
                            val action = pendingWriteAction
                            showWriteKeyDialog = false
                            pendingWriteAction = null
                            if (action != null) {
                                action(APApplication.composeWriteKey(writeKey2))
                            }
                        }, enabled = writeKey2.length in 3..10 && writeKey2.all { it.isDigit() }) {
                            Text(stringResource(id = android.R.string.ok))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppItem(
    app: SuperUserViewModel.AppInfo,
    onLongClick: () -> Unit,
    onRequireWriteAuth: ((String) -> Unit) -> Unit,
) {
    val config = app.config
    var showEditProfile by remember { mutableStateOf(false) }
    var rootGranted by remember { mutableStateOf(config.allow != 0) }
    var excludeApp by remember { mutableIntStateOf(config.exclude) }

    ListItem(
        modifier = Modifier.combinedClickable(onClick = {
            if (!rootGranted) {
                showEditProfile = !showEditProfile
            } else {
                rootGranted = false
                config.allow = 0
                onRequireWriteAuth { authKey ->
                    Natives.revokeSu(authKey, app.uid)
                    PkgConfig.changeConfig(config)
                }
            }
        }, onLongClick = onLongClick),
        headlineContent = { Text(app.label) },
        leadingContent = {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(app.packageInfo)
                    .crossfade(true).build(),
                contentDescription = app.label,
                modifier = Modifier
                    .padding(4.dp)
                    .width(48.dp)
                    .height(48.dp)
            )
        },
        supportingContent = {

            Column {
                Text(app.packageName)
                FlowRow {

                    if (excludeApp == 1) {
                        LabelText(label = stringResource(id = R.string.su_pkg_excluded_label))
                    }
                    if (rootGranted) {
                        LabelText(label = config.profile.uid.toString())
                        LabelText(label = config.profile.toUid.toString())
                        LabelText(
                            label = when {
                                // todo: valid scontext ?
                                config.profile.scontext.isNotEmpty() -> config.profile.scontext
                                else -> stringResource(id = R.string.su_selinux_via_hook)
                            }
                        )
                    }
                }
            }
        },
        trailingContent = {
            Switch(checked = rootGranted, onCheckedChange = {
                rootGranted = !rootGranted
                if (rootGranted) {
                    excludeApp = 0
                    config.allow = 1
                    config.exclude = 0
                    config.profile.scontext = APApplication.MAGISK_SCONTEXT
                } else {
                    config.allow = 0
                }
                config.profile.uid = app.uid
                if (config.allow == 1) {
                    onRequireWriteAuth { authKey ->
                        Natives.grantSu(authKey, app.uid, 0, config.profile.scontext)
                        Natives.setUidExclude(authKey, app.uid, 0)
                        PkgConfig.changeConfig(config)
                    }
                } else {
                    onRequireWriteAuth { authKey ->
                        Natives.revokeSu(authKey, app.uid)
                        PkgConfig.changeConfig(config)
                    }
                }
            })
        },
    )

    AnimatedVisibility(
        visible = showEditProfile && !rootGranted,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        SwitchItem(
            icon = Icons.Filled.Security,
            title = stringResource(id = R.string.su_pkg_excluded_setting_title),
            summary = stringResource(id = R.string.su_pkg_excluded_setting_summary),
            checked = excludeApp == 1,
            onCheckedChange = {
                if (it) {
                    excludeApp = 1
                    config.allow = 0
                    config.profile.scontext = APApplication.DEFAULT_SCONTEXT
                    onRequireWriteAuth { authKey ->
                        Natives.revokeSu(authKey, app.uid)
                    }
                } else {
                    excludeApp = 0
                }
                config.exclude = excludeApp
                config.profile.uid = app.uid
                onRequireWriteAuth { authKey ->
                    Natives.setUidExclude(authKey, app.uid, excludeApp)
                    PkgConfig.changeConfig(config)
                }
            },
        )
    }
}

@Composable
fun LabelText(label: String) {
    Box(
        modifier = Modifier
            .padding(top = 4.dp, end = 4.dp)
            .background(
                Color.Black, shape = RoundedCornerShape(4.dp)
            )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(vertical = 2.dp, horizontal = 5.dp),
            style = TextStyle(
                fontSize = 8.sp,
                color = Color.White,
            )
        )
    }
}

private data class CapabilityItem(
    val id: Int,
    val name: String,
    val summary: String,
)

private enum class CapGroup {
    EFFECTIVE,
    PERMITTED,
    INHERITABLE,
    BSET,
    AMBIENT,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CapabilitiesDialog(
    app: SuperUserViewModel.AppInfo,
    onDismiss: () -> Unit,
    onError: (String) -> Unit,
    onSaved: () -> Unit,
    onRequireWriteAuth: ((String) -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val storedConfig = app.config
    var selectedGroup by remember { mutableStateOf(CapGroup.EFFECTIVE) }
    var showGroupDropdown by remember { mutableStateOf(false) }
    var enabled by remember { mutableStateOf(false) }
    val isChineseLocale = remember {
        context.resources.configuration.locales[0].language.startsWith("zh")
    }
    val capabilityList = remember(isChineseLocale) { capabilityItems(isChineseLocale) }

    val defaultMask = remember(capabilityList) {
        capabilityList.fold(0L) { acc, item -> acc or (1L shl item.id) }
    }

    var inheritable by remember { mutableStateOf(defaultMask) }
    var permitted by remember { mutableStateOf(defaultMask) }
    var effective by remember { mutableStateOf(defaultMask) }
    var bset by remember { mutableStateOf(defaultMask) }
    var ambient by remember { mutableStateOf(defaultMask) }

    LaunchedEffect(app.uid) {
        val profile = Natives.suCapProfile(app.uid)
        if (profile != null) {
            enabled = profile.enabled != 0
            inheritable = profile.inheritable
            permitted = profile.permitted
            effective = profile.effective
            bset = profile.bset
            ambient = profile.ambient
        } else if (storedConfig.hasCustomCapabilities()) {
            enabled = storedConfig.capEnabled != 0
            inheritable = storedConfig.capInheritable
            permitted = storedConfig.capPermitted
            effective = storedConfig.capEffective
            bset = storedConfig.capBset
            ambient = storedConfig.capAmbient
        } else {
            inheritable = defaultMask
            permitted = defaultMask
            effective = defaultMask
            bset = defaultMask
            ambient = defaultMask
            enabled = false
        }
    }

    fun hasCustomRuleConfigured(): Boolean {
        return inheritable != defaultMask ||
            permitted != defaultMask ||
            effective != defaultMask ||
            bset != defaultMask ||
            ambient != defaultMask
    }

    val canApply = !enabled || hasCustomRuleConfigured()

    fun currentMask(): Long {
        return when (selectedGroup) {
            CapGroup.EFFECTIVE -> effective
            CapGroup.PERMITTED -> permitted
            CapGroup.INHERITABLE -> inheritable
            CapGroup.BSET -> bset
            CapGroup.AMBIENT -> ambient
        }
    }

    fun updateCurrentMask(mask: Long) {
        when (selectedGroup) {
            CapGroup.EFFECTIVE -> effective = mask
            CapGroup.PERMITTED -> permitted = mask
            CapGroup.INHERITABLE -> inheritable = mask
            CapGroup.BSET -> bset = mask
            CapGroup.AMBIENT -> ambient = mask
        }
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(360.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.su_cap_menu_title) + " · ${app.label}")
                Spacer(modifier = Modifier.height(8.dp))

                SwitchItem(
                    icon = Icons.Filled.Security,
                    title = stringResource(R.string.su_cap_enable_custom),
                    checked = enabled,
                    onCheckedChange = { enabled = it }
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.su_cap_select_group)) },
                    supportingContent = {
                        Text(
                            text = groupTitle(context, selectedGroup),
                            style = TextStyle(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.outline,
                        )
                    },
                    modifier = Modifier.clickable(enabled = enabled) { showGroupDropdown = true }
                )

                DropdownMenu(expanded = showGroupDropdown, onDismissRequest = { showGroupDropdown = false }) {
                    CapGroup.entries.forEach { group ->
                        DropdownMenuItem(
                            text = { Text(groupTitle(context, group)) },
                            onClick = {
                                selectedGroup = group
                                showGroupDropdown = false
                            }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(enabled = enabled, onClick = { updateCurrentMask(defaultMask) }) {
                        Text(stringResource(R.string.su_cap_select_all))
                    }
                    TextButton(enabled = enabled, onClick = { updateCurrentMask(0L) }) {
                        Text(stringResource(R.string.su_cap_clear_all))
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .heightIn(min = 140.dp, max = 360.dp)
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    items(capabilityList, key = { it.id }) { capability ->
                        val current = currentMask()
                        val checked = (current and (1L shl capability.id)) != 0L
                        SwitchItem(
                            icon = null,
                            title = capability.name,
                            summary = capability.summary,
                            checked = checked,
                            enabled = enabled,
                            onCheckedChange = { isChecked ->
                                val updated = if (isChecked) {
                                    currentMask() or (1L shl capability.id)
                                } else {
                                    currentMask() and (1L shl capability.id).inv()
                                }
                                updateCurrentMask(updated)
                            }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = {
                        onRequireWriteAuth { authKey ->
                            val rc = Natives.deleteSuCapProfile(authKey, app.uid)
                            if (rc < 0) {
                                onError(context.getString(R.string.su_cap_save_failed))
                            } else {
                                storedConfig.capEnabled = 0
                                storedConfig.capInheritable = defaultMask
                                storedConfig.capPermitted = defaultMask
                                storedConfig.capEffective = defaultMask
                                storedConfig.capBset = defaultMask
                                storedConfig.capAmbient = defaultMask
                                PkgConfig.changeConfig(storedConfig)
                                onSaved()
                            }
                        }
                    }) {
                        Text(stringResource(R.string.su_cap_menu_reset))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(enabled = canApply, onClick = {
                        if (enabled && !hasCustomRuleConfigured()) {
                            onError(context.getString(R.string.su_cap_custom_rule_required))
                            return@Button
                        }
                        val profile = Natives.CapProfile(
                            uid = app.uid,
                            enabled = if (enabled) 1 else 0,
                            inheritable = inheritable,
                            permitted = permitted,
                            effective = effective,
                            bset = bset,
                            ambient = ambient,
                        )
                        onRequireWriteAuth { authKey ->
                            val rc = Natives.setSuCapProfile(authKey, profile)
                            if (rc < 0) {
                                onError(context.getString(R.string.su_cap_save_failed))
                            } else {
                                storedConfig.capEnabled = profile.enabled
                                storedConfig.capInheritable = profile.inheritable
                                storedConfig.capPermitted = profile.permitted
                                storedConfig.capEffective = profile.effective
                                storedConfig.capBset = profile.bset
                                storedConfig.capAmbient = profile.ambient
                                PkgConfig.changeConfig(storedConfig)
                                onSaved()
                            }
                        }
                    }) {
                        Text(stringResource(R.string.su_cap_menu_apply))
                    }
                }
            }
        }
    }
}

@Composable
private fun groupTitle(context: android.content.Context, group: CapGroup): String {
    return when (group) {
        CapGroup.EFFECTIVE -> context.getString(R.string.su_cap_group_effective)
        CapGroup.PERMITTED -> context.getString(R.string.su_cap_group_permitted)
        CapGroup.INHERITABLE -> context.getString(R.string.su_cap_group_inheritable)
        CapGroup.BSET -> context.getString(R.string.su_cap_group_bset)
        CapGroup.AMBIENT -> context.getString(R.string.su_cap_group_ambient)
    }
}

private fun capabilityItems(isChineseLocale: Boolean): List<CapabilityItem> {
    return if (isChineseLocale) chineseCapabilityItems() else englishCapabilityItems()
}

private fun chineseCapabilityItems(): List<CapabilityItem> = listOf(
    CapabilityItem(0, "切换用户", "允许切换到其他用户身份"),
    CapabilityItem(1, "文件权限绕过", "忽略文件读写权限检查"),
    CapabilityItem(2, "内存与文件控制", "调整进程内存和文件属性"),
    CapabilityItem(3, "文件所有权管理", "修改文件所有者和组"),
    CapabilityItem(4, "特殊设备访问", "访问 mknod 等设备操作"),
    CapabilityItem(5, "信号跨进程", "向任意进程发送信号"),
    CapabilityItem(6, "进程审计控制", "管理进程审计策略"),
    CapabilityItem(7, "系统管理", "执行挂载、主机名等系统管理"),
    CapabilityItem(8, "审计配置", "调整审计子系统配置"),
    CapabilityItem(9, "不可变标志", "设置文件不可变/追加标志"),
    CapabilityItem(10, "网络广播", "使用网络广播与组播管理"),
    CapabilityItem(11, "管理驱动", "加载/卸载部分驱动相关能力"),
    CapabilityItem(12, "网络管理", "管理网络接口与路由"),
    CapabilityItem(13, "原始网络", "创建原始套接字"),
    CapabilityItem(14, "IPC 锁", "锁定共享内存和消息队列"),
    CapabilityItem(15, "IPC 所有者", "越权管理 IPC 对象"),
    CapabilityItem(16, "内核模块", "加载或卸载内核模块"),
    CapabilityItem(17, "原始 I/O", "执行端口级原始 I/O"),
    CapabilityItem(18, "chroot", "切换根目录环境"),
    CapabilityItem(19, "ptrace 调试", "调试和跟踪其他进程"),
    CapabilityItem(20, "进程会计", "管理进程会计功能"),
    CapabilityItem(21, "系统启动管理", "重启、关机和系统启动操作"),
    CapabilityItem(22, "资源限制", "突破部分资源限制"),
    CapabilityItem(23, "时间管理", "修改系统时间"),
    CapabilityItem(24, "TTY 配置", "管理终端设备配置"),
    CapabilityItem(25, "mknod", "创建设备节点"),
    CapabilityItem(26, "租约控制", "管理文件租约"),
    CapabilityItem(27, "审计写入", "向审计日志写入事件"),
    CapabilityItem(28, "审计读取", "读取审计日志"),
    CapabilityItem(29, "文件能力设置", "设置文件 capability"),
    CapabilityItem(30, "MAC 覆盖", "覆盖 MAC 安全策略"),
    CapabilityItem(31, "MAC 管理", "管理 MAC 安全策略"),
    CapabilityItem(32, "系统日志", "读取内核日志缓冲"),
    CapabilityItem(33, "唤醒管理", "控制系统唤醒锁"),
    CapabilityItem(34, "阻塞挂起", "阻止系统进入休眠"),
    CapabilityItem(35, "审计控制", "管理审计规则"),
    CapabilityItem(36, "性能监控", "访问性能计数器"),
    CapabilityItem(37, "BPF", "加载并控制 BPF 程序"),
    CapabilityItem(38, "检查点恢复", "进程检查点与恢复"),
    CapabilityItem(39, "混合根权限", "兼容扩展能力位 39"),
    CapabilityItem(40, "虚拟机管理", "管理 KVM/虚拟化能力"),
)

private fun englishCapabilityItems(): List<CapabilityItem> = listOf(
    CapabilityItem(0, "Set UID/GID", "Switch process identity to another user or group"),
    CapabilityItem(1, "DAC Override", "Bypass file read/write/execute permission checks"),
    CapabilityItem(2, "DAC Read Search", "Bypass file read and directory search checks"),
    CapabilityItem(3, "File Owner", "Change file owner UID/GID and related attributes"),
    CapabilityItem(4, "FSETID", "Preserve setuid/setgid bits and set file IDs"),
    CapabilityItem(5, "Kill", "Send signals to arbitrary processes"),
    CapabilityItem(6, "Set GID", "Set process GID and supplementary groups"),
    CapabilityItem(7, "Set UID", "Set process UID"),
    CapabilityItem(8, "Set PCAP", "Modify thread capability sets"),
    CapabilityItem(9, "Linux Immutable", "Set immutable or append-only inode flags"),
    CapabilityItem(10, "Net Bind Service", "Bind to privileged ports below 1024"),
    CapabilityItem(11, "Net Broadcast", "Use socket broadcasting and multicast control"),
    CapabilityItem(12, "Net Admin", "Perform network administration operations"),
    CapabilityItem(13, "Net Raw", "Create and use raw or packet sockets"),
    CapabilityItem(14, "IPC Lock", "Lock shared memory and message queue pages"),
    CapabilityItem(15, "IPC Owner", "Bypass IPC object ownership checks"),
    CapabilityItem(16, "Sys Module", "Load or unload kernel modules"),
    CapabilityItem(17, "Sys RawIO", "Perform low-level raw I/O operations"),
    CapabilityItem(18, "Sys Chroot", "Use chroot and change mount namespace root"),
    CapabilityItem(19, "Sys Ptrace", "Trace or debug arbitrary processes"),
    CapabilityItem(20, "Sys PACCT", "Configure process accounting"),
    CapabilityItem(21, "Sys Admin", "Perform broad system administration tasks"),
    CapabilityItem(22, "Sys Boot", "Reboot or power off the system"),
    CapabilityItem(23, "Sys Nice", "Raise process priority and scheduling settings"),
    CapabilityItem(24, "Sys Resource", "Override certain resource limits"),
    CapabilityItem(25, "Sys Time", "Set system clock and real-time clock"),
    CapabilityItem(26, "Sys TTY Config", "Configure terminal and tty devices"),
    CapabilityItem(27, "MKNOD", "Create special files and device nodes"),
    CapabilityItem(28, "Lease", "Take or release file leases"),
    CapabilityItem(29, "Audit Write", "Write records to the audit log"),
    CapabilityItem(30, "Audit Control", "Configure audit subsystem rules"),
    CapabilityItem(31, "SetFCAP", "Set file capabilities on executables"),
    CapabilityItem(32, "MAC Override", "Bypass mandatory access control restrictions"),
    CapabilityItem(33, "MAC Admin", "Manage mandatory access control policy"),
    CapabilityItem(34, "Syslog", "Read or control kernel logging"),
    CapabilityItem(35, "Wake Alarm", "Configure timers that wake the device"),
    CapabilityItem(36, "Block Suspend", "Prevent system suspend"),
    CapabilityItem(37, "Audit Read", "Read audit logs via multicast netlink"),
    CapabilityItem(38, "Perfmon", "Use performance monitoring facilities"),
    CapabilityItem(39, "BPF", "Load and manage eBPF programs and maps"),
    CapabilityItem(40, "Checkpoint Restore", "Use process checkpoint and restore features"),
)