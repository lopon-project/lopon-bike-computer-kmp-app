package ru.lopon.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.lopon.ui.theme.LoponColors
import ru.lopon.ui.theme.LoponDimens
import ru.lopon.ui.theme.LoponShapes
import ru.lopon.ui.theme.LoponTypography

/**
 * Error message card with dismiss button.
 */
@Composable
fun ErrorCard(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = LoponColors.error.copy(alpha = 0.08f)
        ),
        shape = LoponShapes.card
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LoponDimens.cardPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = LoponColors.error,
                style = LoponTypography.body,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    }
}

/**
 * Empty state card with emoji and message.
 */
@Composable
fun EmptyStateCard(
    emoji: String,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = LoponShapes.card
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = emoji,
                fontSize = 48.sp
            )
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                color = LoponColors.onSurfacePrimary
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = LoponTypography.caption,
                    color = LoponColors.onSurfaceSecondary
                )
            }
        }
    }
}
