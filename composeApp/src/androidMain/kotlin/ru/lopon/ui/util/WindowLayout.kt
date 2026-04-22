package ru.lopon.ui.util

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
@ReadOnlyComposable
fun rememberIsLandscape(): Boolean =
    LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
