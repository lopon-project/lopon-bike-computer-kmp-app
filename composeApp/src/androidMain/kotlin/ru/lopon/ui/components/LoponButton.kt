package ru.lopon.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ru.lopon.ui.theme.LoponColors
import ru.lopon.ui.theme.LoponDimens
import ru.lopon.ui.theme.LoponShapes
import ru.lopon.ui.theme.LoponTypography

enum class LoponButtonVariant {
    Primary,
    Secondary,
    Danger,
    Outlined
}

@Composable
fun LoponButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: LoponButtonVariant = LoponButtonVariant.Primary,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    large: Boolean = false
) {
    val height = if (large) LoponDimens.buttonHeightLarge else LoponDimens.buttonHeightMedium
    val textStyle = if (large) LoponTypography.buttonLarge else LoponTypography.button

    if (variant == LoponButtonVariant.Outlined) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.height(height),
            shape = LoponShapes.button
        ) {
            ButtonContent(icon = icon, text = text, textStyle = textStyle)
        }
        return
    }

    val (containerColor, contentColor) = when (variant) {
        LoponButtonVariant.Primary -> LoponColors.primaryYellow to LoponColors.black
        LoponButtonVariant.Secondary -> LoponColors.black to LoponColors.white
        LoponButtonVariant.Danger -> LoponColors.error to LoponColors.white
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = LoponShapes.button
    ) {
        ButtonContent(icon = icon, text = text, textStyle = textStyle)
    }
}

@Composable
private fun ButtonContent(
    icon: ImageVector?,
    text: String,
    textStyle: androidx.compose.ui.text.TextStyle
) {
    if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(4.dp))
    }
    Text(text = text, style = textStyle)
}
