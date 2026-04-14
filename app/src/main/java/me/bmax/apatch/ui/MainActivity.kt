package me.bmax.apatch.ui

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.Coil
import coil.ImageLoader
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.rememberNavHostEngine
import com.ramcosta.composedestinations.utils.isRouteOnBackStackAsState
import com.ramcosta.composedestinations.utils.rememberDestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.screen.BottomBarDestination
import me.bmax.apatch.ui.theme.APatchThemeWithBackground
import me.bmax.apatch.ui.viewmodel.SuperUserViewModel
import me.bmax.apatch.util.PkgConfig
import me.bmax.apatch.util.ui.LocalSnackbarHost
import me.zhanghai.android.appiconloader.coil.AppIconFetcher
import me.zhanghai.android.appiconloader.coil.AppIconKeyer

class MainActivity : AppCompatActivity() {

    private var isLoading = true

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {

        installSplashScreen().setKeepOnScreenCondition { isLoading }

        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        super.onCreate(savedInstanceState)
        isLoading = false

        val biometricEnabled = APApplication.sharedPreferences.getBoolean("biometric_login", false)
        if (biometricEnabled) {
            val canAuthenticate = BiometricManager.from(this).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
                Toast.makeText(this, getString(R.string.settings_biometric_unavailable), Toast.LENGTH_SHORT).show()
                finishAndRemoveTask()
                return
            }
            promptBiometricAndLaunch()
        } else {
            setupUIContent()
        }
    }

    private fun promptBiometricAndLaunch() {
        val biometricPrompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@MainActivity, errString, Toast.LENGTH_SHORT).show()
                    finishAndRemoveTask()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    setupUIContent()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.action_biometric))
            .setSubtitle(getString(R.string.msg_biometric))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun setupUIContent() {
        setContent {
            APatchThemeWithBackground {
                val navController = rememberNavController()
                val snackBarHostState = remember { SnackbarHostState() }
                val configuration = LocalConfiguration.current
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val bottomBarRoutes = remember {
                    BottomBarDestination.entries.map { it.direction.route }.toSet()
                }
                val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
                val kPatchReady = state != APApplication.State.UNKNOWN_STATE
                val aPatchReady = state == APApplication.State.ANDROIDPATCH_INSTALLED
                var showRollbackDialog by remember { mutableStateOf(false) }
                var rollbackPendingList by remember { mutableStateOf<List<PkgConfig.PendingRollbackItem>>(emptyList()) }
                var rollbackBusy by remember { mutableStateOf(false) }
                val visibleDestinations = remember(state) {
                    BottomBarDestination.entries.filter { destination ->
                        !(destination.kPatchRequired && !kPatchReady) && !(destination.aPatchRequired && !aPatchReady)
                    }.toSet()
                }

                val defaultTransitions = object : NavHostAnimatedDestinationStyle() {
                    override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
                        {
                            if (targetState.destination.route !in bottomBarRoutes) {
                                slideInHorizontally(initialOffsetX = { it })
                            } else {
                                fadeIn(animationSpec = tween(340))
                            }
                        }

                    override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
                        {
                            if (initialState.destination.route in bottomBarRoutes && targetState.destination.route !in bottomBarRoutes) {
                                slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut()
                            } else {
                                fadeOut(animationSpec = tween(340))
                            }
                        }

                    override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
                        {
                            if (targetState.destination.route in bottomBarRoutes) {
                                slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn()
                            } else {
                                fadeIn(animationSpec = tween(340))
                            }
                        }

                    override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
                        {
                            if (initialState.destination.route !in bottomBarRoutes) {
                                scaleOut(targetScale = 0.9f) + fadeOut()
                            } else {
                                fadeOut(animationSpec = tween(340))
                            }
                        }
                }

                LaunchedEffect(Unit) {
                    if (SuperUserViewModel.apps.isEmpty()) {
                        SuperUserViewModel().fetchAppList()
                    }
                }

                LaunchedEffect(Unit) {
                    val (drift, pending) = withContext(Dispatchers.IO) {
                        val pendingList = PkgConfig.getPendingRollbackList()
                        val hasDrift = PkgConfig.hasKernelConfigDrift()
                        hasDrift to pendingList
                    }
                    if (drift) {
                        rollbackPendingList = pending
                        showRollbackDialog = true
                    }
                }

                Scaffold(
                    bottomBar = {
                        if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                            BottomBar(navController, visibleDestinations)
                        }
                    },
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    CompositionLocalProvider(
                        LocalSnackbarHost provides snackBarHostState,
                    ) {
                        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            Row(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))) {
                                SideBar(
                                    navController = navController,
                                    modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top)),
                                    visibleDestinations = visibleDestinations
                                )
                                DestinationsNavHost(
                                    modifier = Modifier
                                        .weight(1f)
                                        .consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Start)),
                                    navGraph = NavGraphs.root,
                                    navController = navController,
                                    engine = rememberNavHostEngine(navHostContentAlignment = Alignment.TopCenter),
                                    defaultTransitions = defaultTransitions
                                )
                            }
                        } else {
                            DestinationsNavHost(
                                modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
                                navGraph = NavGraphs.root,
                                navController = navController,
                                engine = rememberNavHostEngine(navHostContentAlignment = Alignment.TopCenter),
                                defaultTransitions = defaultTransitions
                            )
                        }
                    }
                }

                if (showRollbackDialog) {
                    RollbackReconcileDialog(
                        pendingItems = rollbackPendingList,
                        running = rollbackBusy,
                        onDismiss = { showRollbackDialog = false },
                        onConfirm = { key2 ->
                            rollbackBusy = true
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) {
                                    PkgConfig.reconcileKernelFromConfig(APApplication.composeWriteKey(key2))
                                }
                                rollbackBusy = false
                                if (ok) {
                                    showRollbackDialog = false
                                    Toast.makeText(context, "配置回灌完成", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "配置回灌失败，请重试", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }

        val iconSize = resources.getDimensionPixelSize(android.R.dimen.app_icon_size)
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .components {
                    add(AppIconKeyer())
                    add(AppIconFetcher.Factory(iconSize, false, this@MainActivity))
                }
                .build()
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RollbackReconcileDialog(
    pendingItems: List<PkgConfig.PendingRollbackItem>,
    running: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var writeKey2 by remember { mutableStateOf("") }
    var showPendingList by remember { mutableStateOf(false) }

    BasicAlertDialog(
        onDismissRequest = {
            if (!running) onDismiss()
        },
        properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(340.dp)
                .wrapContentHeight(),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = stringResource(R.string.key2_dialog_title))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "检测到内核授权与用户空间配置不一致，请输入 2key 执行回灌。")

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = writeKey2,
                    onValueChange = { writeKey2 = it },
                    label = { Text(stringResource(R.string.patch_set_key2)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )

                Spacer(modifier = Modifier.height(6.dp))
                TextButton(onClick = { showPendingList = !showPendingList }) {
                    Text("待回滚列表")
                }

                if (showPendingList) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        tonalElevation = 1.dp,
                    ) {
                        if (pendingItems.isEmpty()) {
                            Text(
                                text = "暂无待回滚项",
                                modifier = Modifier.padding(12.dp),
                            )
                        } else {
                            LazyColumn(modifier = Modifier.padding(8.dp)) {
                                items(pendingItems, key = { "${it.uid}:${it.pkg}" }) { item ->
                                    Text(
                                        text = "uid=${item.uid}  ${item.appLabel} (${item.pkg})",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = onDismiss,
                        enabled = !running,
                    ) {
                        Text(stringResource(id = android.R.string.cancel))
                    }
                    Button(
                        onClick = { onConfirm(writeKey2) },
                        enabled = !running && writeKey2.length in 3..10 && writeKey2.all { it.isDigit() },
                    ) {
                        Text(stringResource(id = android.R.string.ok))
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController, visibleDestinations: Set<BottomBarDestination>) {
    val navigator = navController.rememberDestinationsNavigator()

    Crossfade(
        targetState = visibleDestinations,
        label = "BottomBarStateCrossfade"
    ) { visibleDestinations ->
        NavigationBar(tonalElevation = 8.dp) {
            visibleDestinations.forEach { destination ->
                val isCurrentDestOnBackStack by navController.isRouteOnBackStackAsState(destination.direction)

                NavigationBarItem(
                    selected = isCurrentDestOnBackStack,
                    onClick = {
                        if (isCurrentDestOnBackStack) {
                            navigator.popBackStack(destination.direction, false)
                        }
                        navigator.navigate(destination.direction) {
                            popUpTo(NavGraphs.root) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        if (isCurrentDestOnBackStack) {
                            Icon(destination.iconSelected, stringResource(destination.label))
                        } else {
                            Icon(destination.iconNotSelected, stringResource(destination.label))
                        }
                    },
                    label = {
                        Text(
                            text = stringResource(destination.label),
                            overflow = TextOverflow.Visible,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    alwaysShowLabel = false
                )
            }
        }
    }
}

@Composable
private fun SideBar(navController: NavHostController, modifier: Modifier = Modifier, visibleDestinations: Set<BottomBarDestination>) {
    val navigator = navController.rememberDestinationsNavigator()

    Crossfade(
        targetState = visibleDestinations,
        label = "SideBarStateCrossfade"
    ) { visibleDestinations ->
        NavigationRail(
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
            ) {
                visibleDestinations.forEach { destination ->
                    val isCurrentDestOnBackStack by navController.isRouteOnBackStackAsState(destination.direction)
                    NavigationRailItem(
                        selected = isCurrentDestOnBackStack,
                        onClick = {
                            if (isCurrentDestOnBackStack) {
                                navigator.popBackStack(destination.direction, false)
                            }
                            navigator.navigate(destination.direction) {
                                popUpTo(NavGraphs.root) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            if (isCurrentDestOnBackStack) {
                                Icon(destination.iconSelected, stringResource(destination.label))
                            } else {
                                Icon(destination.iconNotSelected, stringResource(destination.label))
                            }
                        },
                        label = { Text(stringResource(destination.label)) },
                        alwaysShowLabel = false,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}
