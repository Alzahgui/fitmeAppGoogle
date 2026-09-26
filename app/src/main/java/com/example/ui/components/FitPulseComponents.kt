package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PulseAmber
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.PulseGreen
import com.example.ui.theme.PulseGreenDark
import com.example.ui.theme.PulseGreenLight
import com.example.ui.theme.PulseRose
import com.example.ui.viewmodel.ScreenTab

@Composable
fun FitPulseBottomNav(
    currentTab: ScreenTab,
    onTabSelected: (ScreenTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.testTag("fitpulse_bottom_nav"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets.navigationBars
    ) {
        val items: List<Triple<ScreenTab, String, Pair<ImageVector, ImageVector>>> = listOf(
            Triple(ScreenTab.HOME, "Home", Icons.Filled.FitnessCenter to Icons.Outlined.FitnessCenter),
            Triple(ScreenTab.PLANS, "Plans", Icons.Filled.List to Icons.Outlined.List),
            Triple(ScreenTab.AI_COACH, "AI Coach", Icons.Filled.SmartToy to Icons.Outlined.SmartToy),
            Triple(ScreenTab.EXERCISES, "Library", Icons.Filled.FitnessCenter to Icons.Outlined.FitnessCenter),
            Triple(ScreenTab.HISTORY, "History", Icons.Filled.BarChart to Icons.Outlined.BarChart)
        )

        items.forEach { (tab, label, iconPair) ->
            val isSelected = currentTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) iconPair.first else iconPair.second,
                        contentDescription = label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun StatMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CircularRestTimerCountdown(
    secondsRemaining: Int,
    totalSeconds: Int,
    onAddSeconds: (Int) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (totalSeconds > 0) {
        (secondsRemaining.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(targetValue = progress, label = "RestTimerProgress")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, PulseCyan.copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(PulseCyan)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "REST INTERVAL TIMER",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = PulseCyan
                    )
                }

                IconButton(
                    onClick = onSkip,
                    modifier = Modifier.testTag("skip_rest_timer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Skip rest timer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Circular Visual Countdown
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(140.dp)
            ) {
                val circleTrackColor = MaterialTheme.colorScheme.surface
                val circleProgressColor = PulseCyan

                Canvas(modifier = Modifier.size(130.dp)) {
                    val strokeWidth = 10.dp.toPx()
                    // Background track
                    drawCircle(
                        color = circleTrackColor,
                        style = Stroke(width = strokeWidth)
                    )
                    // Sweep Arc
                    drawArc(
                        brush = Brush.sweepGradient(listOf(PulseCyan, PulseGreen, PulseCyan)),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$secondsRemaining",
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "seconds",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Adjustment Quick Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = { onAddSeconds(-15) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("minus_15_rest_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("-15s", fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = { onAddSeconds(30) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("plus_30_rest_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("+30s", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onSkip,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("skip_rest_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = PulseGreen, contentColor = Color(0xFF00381B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Skip", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun WeeklyActivityBarChart(
    days: List<String>,
    values: List<Float>,
    modifier: Modifier = Modifier
) {
    val maxValue = (values.maxOrNull() ?: 1f).coerceAtLeast(1f)
    val barColor = PulseGreen
    val emptyBarColor = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val count = days.size
                val barWidth = 24.dp.toPx()
                val totalWidth = size.width
                val spacing = (totalWidth - (barWidth * count)) / (count + 1)

                values.forEachIndexed { i, value ->
                    val x = spacing + (i * (barWidth + spacing))
                    val heightRatio = (value / maxValue).coerceIn(0.08f, 1f)
                    val barHeight = (size.height - 30.dp.toPx()) * heightRatio
                    val y = size.height - 30.dp.toPx() - barHeight

                    // Background slot
                    drawRoundRect(
                        color = emptyBarColor,
                        topLeft = Offset(x, 0f),
                        size = Size(barWidth, size.height - 30.dp.toPx()),
                        cornerRadius = CornerRadius(12f, 12f)
                    )

                    // Active bar
                    if (value > 0) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                listOf(PulseGreenLight, PulseGreen)
                            ),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                    }
                }
            }
        }

        // Day labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            days.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(32.dp)
                )
            }
        }
    }
}
