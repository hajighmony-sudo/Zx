@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.cluvex.zedsecure.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ZxTopBar(
    current: TopDestination,
    drawerOpen: Boolean,
    onMenuClick: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val borderBrush = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.24f),
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f),
        ),
    )
    Box(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.01f))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .border(1.dp, borderBrush, shape),
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f),
            tonalElevation = 2.dp,
            shadowElevation = 8.dp,
        ) {
            Row(
                Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onMenuClick,
                    modifier = Modifier.size(42.dp).semantics {
                        contentDescription = if (drawerOpen) "Close navigation menu" else "Open navigation menu"
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        ZxHamburgerGlyph(open = drawerOpen)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "ZX",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        stringResource(current.labelRes),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                    contentColor = MaterialTheme.colorScheme.tertiary,
                ) {
                    Text(
                        "UI / XU",
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ZxHamburgerGlyph(open: Boolean) {
    val progress = animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
        ),
        label = "zx-menu-morph",
    ).value
    val lineColor = MaterialTheme.colorScheme.onSurface
    Canvas(Modifier.size(24.dp)) {
        val stroke = 2.2.dp.toPx()
        val cx = size.width * 0.5f
        val left = size.width * 0.18f
        val right = size.width * 0.82f
        val topY = size.height * 0.29f
        val midY = size.height * 0.50f
        val bottomY = size.height * 0.71f
        val halfLength = size.width * 0.32f
        val angle = progress * 0.7853982f
        val dx = kotlin.math.cos(angle.toDouble()).toFloat() * halfLength
        val dy = kotlin.math.sin(angle.toDouble()).toFloat() * halfLength
        val topCenterY = topY + (midY - topY) * progress
        val bottomCenterY = bottomY - (bottomY - midY) * progress

        // The two outer strokes travel inward and rotate into a crisp close glyph.
        drawLine(
            lineColor,
            androidx.compose.ui.geometry.Offset(cx - dx, topCenterY - dy),
            androidx.compose.ui.geometry.Offset(cx + dx, topCenterY + dy),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        if (progress < 0.98f) {
            drawLine(
                lineColor.copy(alpha = 1f - progress),
                androidx.compose.ui.geometry.Offset(left, midY),
                androidx.compose.ui.geometry.Offset(right, midY),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
        drawLine(
            lineColor,
            androidx.compose.ui.geometry.Offset(cx - dx, bottomCenterY + dy),
            androidx.compose.ui.geometry.Offset(cx + dx, bottomCenterY - dy),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
@Composable
fun ZxNavigationDrawer(
    current: TopDestination,
    onSelect: (TopDestination) -> Unit,
) {
    val shape = RoundedCornerShape(topEnd = 30.dp, bottomEnd = 30.dp)
    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxHeight()
            .width(320.dp)
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    ),
                ),
                shape,
            ),
        drawerShape = shape,
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.97f),
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        drawerTonalElevation = 0.dp,
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.02f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f),
                        ),
                    ),
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(18.dp))
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.86f),
                tonalElevation = 3.dp,
                shadowElevation = 7.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.48f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.24f),
                            ),
                        ),
                        RoundedCornerShape(24.dp),
                    ),
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(52.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("ZX", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("ZX", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text(
                            "FUTURE OS  •  UI / XU",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            Text(
                "NAVIGATION",
                modifier = Modifier.padding(start = 12.dp, bottom = 9.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            val navigationSections = listOf(
                "CORE" to listOf(TopDestination.Home),
                "NETWORK" to listOf(
                    TopDestination.Servers,
                    TopDestination.Subscriptions,
                    TopDestination.Vault,
                ),
                "TOOLS" to listOf(
                    TopDestination.SpeedTest,
                    TopDestination.Map,
                    TopDestination.Logs,
                    TopDestination.Routing,
                    TopDestination.Apps,
                ),
                "SYSTEM" to listOf(TopDestination.Settings),
            )
            navigationSections.forEach { (sectionTitle, destinations) ->
                Text(
                    sectionTitle,
                    modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 7.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                )
                destinations.forEach { destination ->
                    NavigationDrawerItem(
                        label = {
                            Text(
                                stringResource(destination.labelRes),
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        selected = current == destination,
                        onClick = { onSelect(destination) },
                        icon = {
                            Icon(
                                painterResource(destination.icon),
                                contentDescription = null,
                                modifier = Modifier.size(21.dp),
                            )
                        },
                        shape = RoundedCornerShape(18.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedContainerColor = Color.Transparent,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.62f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "GLASS  •  LIQUID  •  HOLOGRAPHIC",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
fun ZxBottomBar(
    tabs: List<TopDestination>,
    current: TopDestination,
    onSelect: (TopDestination) -> Unit,
) {
    val shape = RoundedCornerShape(30.dp)
    val borderBrush = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.26f),
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f),
        ),
    )
    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f),
            tonalElevation = 2.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, borderBrush, shape),
        ) {
            Row(
                Modifier.padding(horizontal = 5.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { destination ->
                    val selected = current == destination
                    Surface(
                        onClick = { onSelect(destination) },
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = if (selected) 1.dp else 0.dp,
                                brush = borderBrush,
                                shape = RoundedCornerShape(22.dp),
                            ),
                        shape = RoundedCornerShape(22.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent,
                        contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    ) {
                        Row(
                            Modifier.padding(horizontal = 5.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painterResource(destination.icon),
                                contentDescription = stringResource(destination.labelRes),
                                modifier = Modifier.size(21.dp),
                            )
                            AnimatedVisibility(visible = selected || tabs.size <= 4) {
                                Text(
                                    stringResource(destination.labelRes),
                                    modifier = Modifier.padding(start = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
