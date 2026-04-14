package me.bmax.apatch.ui.screen

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.topjohnwu.superuser.io.SuFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.KeyEventBlocker
import me.bmax.apatch.ui.component.rememberCustomDialog
import me.bmax.apatch.util.hasMetaModule
import me.bmax.apatch.util.installModule
import me.bmax.apatch.util.reboot
import me.bmax.apatch.util.ui.LocalSnackbarHost
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

enum class MODULE_TYPE {
    KPM, APM
}

@Composable
@Destination<RootGraph>
fun InstallScreen(navigator: DestinationsNavigator, uri: Uri, type: MODULE_TYPE) {
    var text by remember { mutableStateOf("") }
    var tempText: String
    val logContent = remember { StringBuilder() }
    var showFloatAction by rememberSaveable { mutableStateOf(false) }
    var isScanning by rememberSaveable { mutableStateOf(false) }
    var showScanResultDialog by rememberSaveable { mutableStateOf(false) }
    var installStarted by rememberSaveable { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<ModuleScanResult?>(null) }
    val metaModuleAlertDialog = rememberCustomDialog { dismiss: () -> Unit ->
        val uriHandler = LocalUriHandler.current
        AlertDialog(
            onDismissRequest = { dismiss() },
            icon = {
                Icon(Icons.Outlined.Info, contentDescription = null)
            },
            title = {
                Row(modifier = Modifier
                    .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = stringResource(R.string.warning_of_meta_module_title))
                }
            },
            text = {
                Text(text = stringResource(R.string.warning_of_meta_module_summary))
            },
            confirmButton = {
                FilledTonalButton(onClick = { dismiss() }) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    uriHandler.openUri("https://apatch.dev/meta-module.html")
                }) {
                    Text(text = stringResource(id = R.string.learn_more))
                }
            },
        )
    }

    val context = LocalContext.current
    val snackBarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        if (text.isNotEmpty() || installStarted || isScanning || showScanResultDialog) {
            return@LaunchedEffect
        }
        if (type == MODULE_TYPE.APM) {
            isScanning = true
            scanResult = withContext(Dispatchers.IO) {
                scanApModulePackage(context, uri)
            }
            isScanning = false
            showScanResultDialog = true
        } else {
            installStarted = true
        }
    }

    LaunchedEffect(installStarted) {
        if (!installStarted) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            installModule(uri, type, onFinish = { success ->
                if (!success) return@installModule

                scope.launch {
                    showFloatAction = true

                    // check metamodule
                    if (hasMetaModule()) return@launch
                    val mountOldDirectory =
                        SuFile.open("/data/adb/modules/${getModuleIdFromUri(context, uri)}/system")
                    val mountNewDirectory =
                        SuFile.open("/data/adb/modules_update/${getModuleIdFromUri(context, uri)}/system")
                    if (!mountNewDirectory.isDirectory && !mountOldDirectory.isDirectory) return@launch

                    metaModuleAlertDialog.show()
                }

            }, onStdout = {
                tempText = "$it\n"
                if (tempText.startsWith("[H[J")) { // clear command
                    text = tempText.substring(6)
                } else {
                    text += tempText
                }
                logContent.append(it).append("\n")
            }, onStderr = {
                tempText = "$it\n"
                if (tempText.startsWith("[H[J")) { // clear command
                    text = tempText.substring(6)
                } else {
                    text += tempText
                }
                logContent.append(it).append("\n")
            })
        }
    }

    if (isScanning) {
        AlertDialog(
            onDismissRequest = {},
            icon = {
                CircularProgressIndicator()
            },
            title = {
                Text(text = stringResource(R.string.apm_scan_in_progress_title))
            },
            text = {
                Text(text = stringResource(R.string.apm_scan_in_progress_summary))
            },
            confirmButton = {}
        )
    }

    if (showScanResultDialog && scanResult != null) {
        val result = scanResult!!
        val summary = buildString {
            appendLine(stringResource(R.string.apm_scan_summary_line, result.scriptFilesScanned, result.totalFilesScanned))
            appendLine(stringResource(R.string.apm_scan_high_count, result.highRisks.size))
            appendLine(stringResource(R.string.apm_scan_medium_count, result.mediumRisks.size))
            appendLine(stringResource(R.string.apm_scan_low_count, result.lowRisks.size))
            appendLine(stringResource(R.string.apm_scan_elf_count, result.unknownElfPaths.size))

            if (result.highRisks.isNotEmpty() || result.mediumRisks.isNotEmpty() || result.lowRisks.isNotEmpty()) {
                appendLine()
                appendLine(stringResource(R.string.apm_scan_hits_title))
                (result.highRisks + result.mediumRisks + result.lowRisks).take(10).forEach { hit ->
                    appendLine("- [${hit.severity.name}] ${hit.filePath}: ${hit.evidence}")
                }
            }

            if (result.unknownElfPaths.isNotEmpty()) {
                appendLine()
                appendLine(stringResource(R.string.apm_scan_unknown_elf_title))
                result.unknownElfPaths.take(10).forEach {
                    appendLine("- $it")
                }
            }
        }

        AlertDialog(
            onDismissRequest = {
                showScanResultDialog = false
                navigator.popBackStack()
            },
            title = {
                Text(
                    text = if (result.isDangerous) {
                        stringResource(R.string.apm_scan_result_dangerous)
                    } else {
                        stringResource(R.string.apm_scan_result_safe)
                    }
                )
            },
            text = {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            },
            confirmButton = {
                FilledTonalButton(onClick = {
                    showScanResultDialog = false
                    installStarted = true
                }) {
                    Text(
                        text = if (result.isDangerous) {
                            stringResource(R.string.apm_scan_confirm_install_dangerous)
                        } else {
                            stringResource(R.string.apm_scan_confirm_install)
                        }
                    )
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showScanResultDialog = false
                    navigator.popBackStack()
                }) {
                    Text(text = stringResource(id = android.R.string.cancel))
                }
            }
        )
    }

    Scaffold(topBar = {
        TopBar(onBack = dropUnlessResumed {
            navigator.popBackStack()
        }, onSave = {
            scope.launch {
                val format = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
                val date = format.format(Date())
                val file = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "APatch_install_${type}_log_${date}.log"
                )
                file.writeText(logContent.toString())
                snackBarHost.showSnackbar("Log saved to ${file.absolutePath}")
            }
        })
    }, floatingActionButton = {
        if (showFloatAction) {
            val reboot = stringResource(id = R.string.reboot)
            ExtendedFloatingActionButton(
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            reboot()
                        }
                    }
                },
                icon = { Icon(Icons.Filled.Refresh, reboot) },
                text = { Text(text = reboot) },
            )
        }

    }, snackbarHost = { SnackbarHost(snackBarHost) }) { innerPadding ->
        KeyEventBlocker {
            it.key == Key.VolumeDown || it.key == Key.VolumeUp
        }
        Column(
            modifier = Modifier
                .fillMaxSize(1f)
                .padding(innerPadding)
                .verticalScroll(scrollState),
        ) {
            LaunchedEffect(text) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
            Text(
                modifier = Modifier.padding(8.dp),
                text = text,
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                fontFamily = FontFamily.Monospace,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
            )
        }
    }
}

fun isUriAccessible(context: Context, uri: Uri): Boolean {
    if (uri == Uri.EMPTY) return false

    return try {
        context.contentResolver.openInputStream(uri)?.use {} != null
    } catch (e: Exception) {
        Log.e("ModuleInstall", "URI is inaccessible: $uri", e)
        false
    }
}


fun extractModuleId(context: Context, uri: Uri): String? {
    if (uri == Uri.EMPTY) return null

    context.contentResolver.openInputStream(uri)?.use { inputStream ->
        ZipInputStream(inputStream).use { zip ->
            var entry: ZipEntry?

            while (zip.nextEntry.also { entry = it } != null) {
                if (entry?.name == "module.prop") {
                    val prop = Properties()
                    prop.load(zip)
                    return prop.getProperty("id")
                }
            }
        }
    }

    return null
}

suspend fun getModuleIdFromUri(context: Context, uri: Uri): String? {
    return withContext(Dispatchers.IO) {
        try {
            if (uri == Uri.EMPTY) {
                return@withContext null
            }
            if (!isUriAccessible(context, uri)) {
                return@withContext null
            }
            extractModuleId(context, uri)
        } catch (_: Exception) {
            null
        }
    }
}

private enum class RiskSeverity {
    HIGH, MEDIUM, LOW
}

private data class RiskRule(
    val severity: RiskSeverity,
    val reason: String,
    val regex: Regex,
)

private data class RiskHit(
    val severity: RiskSeverity,
    val filePath: String,
    val evidence: String,
)

private data class ModuleScanResult(
    val totalFilesScanned: Int,
    val scriptFilesScanned: Int,
    val highRisks: List<RiskHit>,
    val mediumRisks: List<RiskHit>,
    val lowRisks: List<RiskHit>,
    val unknownElfPaths: List<String>,
) {
    val isDangerous: Boolean
        get() = highRisks.isNotEmpty()
}

private val moduleRiskRules = listOf(
    RiskRule(
        RiskSeverity.HIGH,
        "pipe network script",
        Regex("(curl|wget)\\s+[^\\n]*\\|\\s*(sh|bash)", RegexOption.IGNORE_CASE)
    ),
    RiskRule(
        RiskSeverity.HIGH,
        "direct block write",
        Regex("dd\\s+[^\\n]*of=/dev/block/", RegexOption.IGNORE_CASE)
    ),
    RiskRule(
        RiskSeverity.HIGH,
        "dangerous recursive delete",
        Regex("rm\\s+-rf\\s+/(system|vendor|product|data)", RegexOption.IGNORE_CASE)
    ),
    RiskRule(
        RiskSeverity.MEDIUM,
        "selinux or ro.secure change",
        Regex("(setenforce\\s+0|resetprop\\s+ro\\.secure\\s+0|permissive)", RegexOption.IGNORE_CASE)
    ),
    RiskRule(
        RiskSeverity.MEDIUM,
        "adb root enable",
        Regex("setprop\\s+service\\.adb\\.root\\s+1", RegexOption.IGNORE_CASE)
    ),
    RiskRule(
        RiskSeverity.LOW,
        "system remount",
        Regex("mount\\s+[^\\n]*(rw|remount)", RegexOption.IGNORE_CASE)
    ),
    RiskRule(
        RiskSeverity.LOW,
        "kernel module operation",
        Regex("\\b(insmod|rmmod)\\b", RegexOption.IGNORE_CASE)
    ),
)

private suspend fun scanApModulePackage(context: Context, uri: Uri): ModuleScanResult {
    return withContext(Dispatchers.IO) {
        val highRisks = mutableListOf<RiskHit>()
        val mediumRisks = mutableListOf<RiskHit>()
        val lowRisks = mutableListOf<RiskHit>()
        val unknownElfPaths = mutableSetOf<String>()

        var totalFiles = 0
        var scriptFiles = 0

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        if (entry.isDirectory) continue

                        totalFiles++
                        val path = entry.name
                        val shouldScanScript = shouldScanAsScript(path)
                        val bytes = readEntryBytesLimited(
                            zip,
                            if (shouldScanScript) 512 * 1024 else 16 * 1024,
                        )

                        if (shouldScanScript) {
                            scriptFiles++
                            val content = bytes.toString(Charsets.UTF_8)
                            for (rule in moduleRiskRules) {
                                rule.regex.find(content)?.let { match ->
                                    val evidence = "${rule.reason}: ${normalizeEvidence(match.value)}"
                                    when (rule.severity) {
                                        RiskSeverity.HIGH -> highRisks.add(RiskHit(rule.severity, path, evidence))
                                        RiskSeverity.MEDIUM -> mediumRisks.add(RiskHit(rule.severity, path, evidence))
                                        RiskSeverity.LOW -> lowRisks.add(RiskHit(rule.severity, path, evidence))
                                    }
                                }
                            }
                        }

                        if (looksLikeElf(bytes)) {
                            unknownElfPaths.add(path)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            highRisks.add(
                RiskHit(
                    severity = RiskSeverity.HIGH,
                    filePath = "package",
                    evidence = "scan failed: ${e.message ?: "unknown error"}",
                )
            )
        }

        ModuleScanResult(
            totalFilesScanned = totalFiles,
            scriptFilesScanned = scriptFiles,
            highRisks = highRisks,
            mediumRisks = mediumRisks,
            lowRisks = lowRisks,
            unknownElfPaths = unknownElfPaths.sorted(),
        )
    }
}

private fun shouldScanAsScript(path: String): Boolean {
    val p = path.lowercase(Locale.ROOT)
    if (p.endsWith(".sh")) return true
    if (p.endsWith("/update-binary") || p.endsWith("updater-script")) return true
    return p.endsWith("post-fs-data.sh") || p.endsWith("service.sh") ||
        p.endsWith("customize.sh") || p.endsWith("install.sh") ||
        p.endsWith("uninstall.sh") || p.endsWith("action.sh")
}

private fun readEntryBytesLimited(zip: ZipInputStream, limit: Int): ByteArray {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(4096)
    var total = 0

    while (true) {
        val read = zip.read(buffer)
        if (read <= 0) break

        if (total < limit) {
            val writeLen = minOf(read, limit - total)
            out.write(buffer, 0, writeLen)
        }
        total += read
    }

    return out.toByteArray()
}

private fun normalizeEvidence(raw: String): String {
    return raw.replace("\n", " ").replace("\t", " ").trim().take(120)
}

private fun looksLikeElf(bytes: ByteArray): Boolean {
    return bytes.size >= 4 &&
        bytes[0] == 0x7F.toByte() &&
        bytes[1] == 'E'.code.toByte() &&
        bytes[2] == 'L'.code.toByte() &&
        bytes[3] == 'F'.code.toByte()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(onBack: () -> Unit = {}, onSave: () -> Unit = {}) {
    TopAppBar(title = { Text(stringResource(R.string.apm_install)) }, navigationIcon = {
        IconButton(
            onClick = onBack
        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
    }, actions = {
        IconButton(onClick = onSave) {
            Icon(
                imageVector = Icons.Filled.Save, contentDescription = "Localized description"
            )
        }
    })
}

@Preview
@Composable
fun InstallPreview() {
//    InstallScreen(DestinationsNavigator(), uri = Uri.EMPTY)
}