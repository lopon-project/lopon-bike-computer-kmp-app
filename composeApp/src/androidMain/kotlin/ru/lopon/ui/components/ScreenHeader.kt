package ru.lopon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.lopon.ui.theme.LoponColors
import ru.lopon.ui.theme.LoponDimens
import ru.lopon.ui.theme.LoponTypography

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(LoponColors.topBarBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = LoponDimens.screenPadding,
                    vertical = LoponDimens.spacerMedium
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = LoponTypography.screenTitle,
                color = LoponColors.topBarContent,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            trailing?.invoke()
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(LoponColors.primaryYellow)
        )
    }
}
