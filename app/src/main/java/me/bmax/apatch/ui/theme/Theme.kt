package me.bmax.apatch.ui.theme

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.MutableLiveData
import coil.compose.rememberAsyncImagePainter
import me.bmax.apatch.APApplication
import me.bmax.apatch.ui.webui.MonetColorsProvider

@Composable
private fun SystemBarStyle(
    darkMode: Boolean,
    statusBarScrim: Color = Color.Transparent,
    navigationBarScrim: Color = Color.Transparent
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity

    SideEffect {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                statusBarScrim.toArgb(),
                statusBarScrim.toArgb(),
            ) { darkMode }, navigationBarStyle = when {
                darkMode -> SystemBarStyle.dark(
                    navigationBarScrim.toArgb()
                )

                else -> SystemBarStyle.light(
                    navigationBarScrim.toArgb(),
                    navigationBarScrim.toArgb(),
                )
            }
        )
    }
}

val refreshTheme = MutableLiveData(false)

@Composable
fun APatchTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val prefs = APApplication.sharedPreferences

    var darkThemeFollowSys by remember {
        mutableStateOf(
            prefs.getBoolean(
                "night_mode_follow_sys",
                true
            )
        )
    }
    var nightModeEnabled by remember {
        mutableStateOf(
            prefs.getBoolean(
                "night_mode_enabled",
                false
            )
        )
    }
    // Dynamic color is available on Android 12+, and custom 1t!
    var dynamicColor by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) prefs.getBoolean(
                "use_system_color_theme",
                true
            ) else false
        )
    }
    var customColorScheme by remember { mutableStateOf(prefs.getString("custom_color", "blue")) }

    val refreshThemeObserver by refreshTheme.observeAsState(false)
    if (refreshThemeObserver == true) {
        darkThemeFollowSys = prefs.getBoolean("night_mode_follow_sys", true)
        nightModeEnabled = prefs.getBoolean("night_mode_enabled", false)
        dynamicColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) prefs.getBoolean(
            "use_system_color_theme",
            true
        ) else false
        customColorScheme = prefs.getString("custom_color", "blue")
        refreshTheme.postValue(false)
    }

    val darkTheme = if (darkThemeFollowSys) {
        isSystemInDarkTheme()
    } else {
        nightModeEnabled
    }

    val baseColorScheme = if (!dynamicColor) {
        if (darkTheme) {
            when (customColorScheme) {
                "amber" -> DarkAmberTheme
                "blue_grey" -> DarkBlueGreyTheme
                "blue" -> DarkBlueTheme
                "brown" -> DarkBrownTheme
                "cyan" -> DarkCyanTheme
                "deep_orange" -> DarkDeepOrangeTheme
                "deep_purple" -> DarkDeepPurpleTheme
                "green" -> DarkGreenTheme
                "indigo" -> DarkIndigoTheme
                "light_blue" -> DarkLightBlueTheme
                "light_green" -> DarkLightGreenTheme
                "lime" -> DarkLimeTheme
                "orange" -> DarkOrangeTheme
                "pink" -> DarkPinkTheme
                "purple" -> DarkPurpleTheme
                "red" -> DarkRedTheme
                "sakura" -> DarkSakuraTheme
                "teal" -> DarkTealTheme
                "yellow" -> DarkYellowTheme
                else -> DarkBlueTheme
            }
        } else {
            when (customColorScheme) {
                "amber" -> LightAmberTheme
                "blue_grey" -> LightBlueGreyTheme
                "blue" -> LightBlueTheme
                "brown" -> LightBrownTheme
                "cyan" -> LightCyanTheme
                "deep_orange" -> LightDeepOrangeTheme
                "deep_purple" -> LightDeepPurpleTheme
                "green" -> LightGreenTheme
                "indigo" -> LightIndigoTheme
                "light_blue" -> LightLightBlueTheme
                "light_green" -> LightLightGreenTheme
                "lime" -> LightLimeTheme
                "orange" -> LightOrangeTheme
                "pink" -> LightPinkTheme
                "purple" -> LightPurpleTheme
                "red" -> LightRedTheme
                "sakura" -> LightSakuraTheme
                "teal" -> LightTealTheme
                "yellow" -> LightYellowTheme
                else -> LightBlueTheme
            }
        }
    } else {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> DarkBlueTheme
            else -> LightBlueTheme
        }
    }

    val backgroundEnabled = GlobalBackgroundConfig.isEnabled(context)
    val colorScheme = if (backgroundEnabled) {
        baseColorScheme.copy(
            background = Color.Transparent,
            surface = baseColorScheme.surface.copy(alpha = 0.92f),
            surfaceVariant = baseColorScheme.surfaceVariant.copy(alpha = 0.92f),
            secondaryContainer = baseColorScheme.secondaryContainer.copy(alpha = 0.92f)
        )
    } else {
        baseColorScheme
    }

    SystemBarStyle(
        darkMode = darkTheme
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = {
            MonetColorsProvider.UpdateCss()
            content()
        }
    )
}

@Composable
fun APatchThemeWithBackground(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    APatchTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            if (GlobalBackgroundConfig.isEnabled(context)) {
                val uri = GlobalBackgroundConfig.getBackgroundUri(context)
                if (!uri.isNullOrBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(model = uri),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = if (isSystemInDarkTheme()) 0.42f else 0.18f))
                    )
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}
