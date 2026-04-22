package ru.lopon.ui.more

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.lopon.R
import ru.lopon.ui.components.ScreenHeader
import ru.lopon.ui.theme.LoponColors
import ru.lopon.ui.theme.LoponDimens
import ru.lopon.ui.theme.LoponShapes
import ru.lopon.ui.theme.LoponTypography

@Composable
fun MoreScreen(
    onOpenSensor: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenOfflineMaps: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(title = "Ещё")

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = LoponDimens.contentMaxWidthCapped)
                    .fillMaxSize()
                    .padding(LoponDimens.screenPadding),
                verticalArrangement = Arrangement.spacedBy(LoponDimens.spacerMedium)
            ) {
                // Brand card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = LoponShapes.card,
                    colors = CardDefaults.cardColors(containerColor = LoponColors.black)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(LoponDimens.cardPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LoponDimens.spacerMedium)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_lopon_wolf),
                            contentDescription = "LOPON",
                            modifier = Modifier.size(48.dp)
                        )
                        Column {
                            Text(
                                text = "LOPON",
                                style = LoponTypography.brandTitle,
                                color = LoponColors.primaryYellow
                            )
                            Text(
                                text = "ваш проводник к цели",
                                style = LoponTypography.brandTagline,
                                color = LoponColors.white.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                ActionCard(
                    icon = Icons.Outlined.Settings,
                    title = "Настройки",
                    description = "Окружность колеса, единицы, режим, автоподключение BLE.",
                    onClick = onOpenSettings
                )

                ActionCard(
                    icon = Icons.Outlined.Map,
                    title = "Оффлайн-карты",
                    description = "Управление регионами карт для работы без интернета.",
                    onClick = onOpenOfflineMaps
                )

                ActionCard(
                    icon = Icons.Outlined.BugReport,
                    title = "Диагностика",
                    description = "Статусы BLE/GPS/хранилища, журнал ошибок, экспорт отчёта.",
                    onClick = onOpenDiagnostics
                )

                Button(
                    onClick = onOpenSensor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(LoponDimens.buttonHeightMedium),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LoponColors.primaryYellow,
                        contentColor = LoponColors.black
                    ),
                    shape = LoponShapes.button
                ) {
                    Icon(Icons.Filled.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(LoponDimens.spacerSmall))
                    Text("Подключение датчика", style = LoponTypography.button)
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = LoponShapes.card,
        colors = CardDefaults.cardColors(containerColor = LoponColors.surfaceCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(LoponColors.primaryYellow)
            )
            Row(
                modifier = Modifier.padding(LoponDimens.cardPadding),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(LoponDimens.spacerMedium)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = LoponColors.onSurfacePrimary,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = LoponTypography.body,
                        fontWeight = FontWeight.SemiBold,
                        color = LoponColors.onSurfacePrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = description,
                        style = LoponTypography.caption,
                        color = LoponColors.onSurfaceSecondary
                    )
                }
            }
        }
    }
}
