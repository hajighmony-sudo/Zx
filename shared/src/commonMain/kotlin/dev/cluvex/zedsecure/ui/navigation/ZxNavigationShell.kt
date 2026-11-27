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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import dev.cluvex.zedsecure.domain.model.ConnectionState
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
    connectionState: ConnectionState = ConnectionState.Idle,
    reduceMotion: Boolean = false,
    onMenuClick: () -> Unit,
) {
    val connectionTone = when {
        connectionState == ConnectionState.Connected -> Color(0xFF45D6AD)
        connectionState == ConnectionState.Error -> MaterialTheme.colorScheme.error
        connectionState.isTransitioning -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val connectionLabel = when {
        connectionState == ConnectionState.Connected -> "متصل"
        connectionState == ConnectionState.Error -> "خطا"
        connectionState.isTransitioning -> "در حال اتصال"
        else -> "آفلاین"
    }
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
                    modifier = Modifier.size(48.dp).semantics {
                        contentDescription = if (drawerOpen) "Close navigation menu" else "Open navigation menu"
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        ZxHamburgerGlyph(open = drawerOpen, reduceMotion = reduceMotion)
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
                    color = connectionTone.copy(alpha = 0.13f),
                    contentColor = connectionTone,
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(Modifier.size(7.dp).background(connectionTone, CircleShape))
                        Text(
                            connectionLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZxHamburgerGlyph(open: Boolean, reduceMotion: Boolean) {
    val progress = animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = if (reduceMotion) androidx.compose.animation.core.snap()
        else androidx.compose.animation.core.spring(
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
    val shape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp)
    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxHeight()
            .width(328.dp)
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.58f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.32f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f),
                    ),
                ),
                shape,
            ),
        drawerShape = shape,
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f),
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        drawerTonalElevation = 0.dp,
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.11f),
                            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.02f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
                        ),
                    ),
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
                tonalElevation = 2.dp,
                shadowElevation = 9.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f),
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                            ),
                        ),
                        RoundedCornerShape(26.dp),
                    ),
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 17.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(54.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("ZX", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        }
                    }
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text("ZX", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text(
                            "FUTURE OS  ·  UI / XU",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                    ) {
                        Box(Modifier.size(10.dp).padding(2.dp).background(MaterialTheme.colorScheme.tertiary, CircleShape))
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.50f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "CURRENT SPACE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(current.labelRes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        contentColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Icon(painterResource(current.icon), null, Modifier.padding(9.dp).size(22.dp))
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            Text(
                "NAVIGATION",
                modifier = Modifier.padding(start = 10.dp, bottom = 8.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            val sections = listOf(
                "CORE" to listOf(TopDestination.Home),
                "NETWORK" to listOf(TopDestination.Servers, TopDestination.Subscriptions, TopDestination.Vault),
                "TOOLS" to listOf(TopDestination.SpeedTest, TopDestination.Map, TopDestination.Logs, TopDestination.Routing, TopDestination.Apps),
                "SYSTEM" to listOf(TopDestination.Settings),
            )
            sections.forEach { (sectionTitle, destinations) ->
                Text(
                    sectionTitle,
                    modifier = Modifier.padding(start = 10.dp, top = 12.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                )
                if (destinations.size == 1) {
                    ZxDrawerTile(
                        destination = destinations.first(),
                        selected = current == destinations.first(),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
                        onClick = { onSelect(destinations.first()) },
                    )
                } else {
                    destinations.chunked(2).forEach { pair ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            pair.forEach { destination ->
                                ZxDrawerTile(
                                    destination = destination,
                                    selected = current == destination,
                                    modifier = Modifier.weight(1f).heightIn(min = 88.dp),
                                    onClick = { onSelect(destination) },
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                Spacer(Modifier.height(2.dp))
            }
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.54f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(
                        "GLASS  ·  LIQUID  ·  HOLOGRAPHIC",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "A calmer, adaptive ZX experience",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ZxDrawerTile(
    destination: TopDestination,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val border = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = if (selected) 0.75f else 0.28f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = if (selected) 0.52f else 0.18f),
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
        ),
    )
    Surface(
        onClick = onClick,
        modifier = modifier.border(if (selected) 1.2.dp else 0.7.dp, border, shape),
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.42f),
        contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        shadowElevation = if (selected) 5.dp else 1.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 11.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(13.dp),
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(38.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painterResource(destination.icon), null, Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(9.dp))
            Text(
                stringResource(destination.labelRes),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun ZxNavigationMenuSheet(
    current: TopDestination,
    onSelect: (TopDestination) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f),
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.48f),
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 11.dp)
                    .size(width = 42.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)),
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 760.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.76f),
                tonalElevation = 1.dp,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.62f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.46f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.24f),
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f),
                            ),
                        ),
                        RoundedCornerShape(26.dp),
                    ),
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(17.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(50.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("ZX", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("ZX", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text(
                            "FUTURE OS  ·  UI / XU",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                    ) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .padding(2.dp)
                                .background(MaterialTheme.colorScheme.tertiary, CircleShape),
                        )
                    }
                }
            }
            Spacer(Modifier.size(14.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.48f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "CURRENT SPACE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(current.labelRes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        contentColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Icon(painterResource(current.icon), null, Modifier.padding(9.dp).size(21.dp))
                    }
                }
            }
            Spacer(Modifier.size(18.dp))
            val sections = listOf(
                "CORE" to listOf(TopDestination.Home),
                "NETWORK" to listOf(TopDestination.Servers, TopDestination.Subscriptions, TopDestination.Vault),
                "TOOLS" to listOf(TopDestination.SpeedTest, TopDestination.Map, TopDestination.Logs, TopDestination.Routing, TopDestination.Apps),
                "SYSTEM" to listOf(TopDestination.Settings),
            )
            sections.forEach { (sectionTitle, destinations) ->
                Text(
                    sectionTitle,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp, top = 7.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                )
                destinations.chunked(2).forEach { pair ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        pair.forEach { destination ->
                            ZxDrawerTile(
                                destination = destination,
                                selected = current == destination,
                                modifier = Modifier.weight(1f).heightIn(min = 72.dp),
                                onClick = { onSelect(destination) },
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.size(8.dp))
                }
            }
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.48f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                    Text(
                        "GLASS  ·  LIQUID  ·  HOLOGRAPHIC",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "A calmer, adaptive ZX experience",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                    )
                }
            }
        }
    }
}

@Composable
fun ZxBottomBar(
    tabs: List<TopDestination>,
    current: TopDestination,
    onSelect: (TopDestination) -> Unit,
    reduceMotion: Boolean = false,
) {
    val shape = RoundedCornerShape(30.dp)
    val borderBrush = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = 0.50f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.58f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.42f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f),
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f),
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
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.78f),
            tonalElevation = 1.dp,
            shadowElevation = 18.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, borderBrush, shape),
        ) {
            Row(
                Modifier.padding(horizontal = 5.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { destination ->
                    val selected = current == destination
                    val container by androidx.compose.animation.animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent,
                        animationSpec = if (reduceMotion) androidx.compose.animation.core.snap() else androidx.compose.animation.core.tween(180),
                        label = "zx-nav-container-${destination.name}",
                    )
                    val content by androidx.compose.animation.animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = if (reduceMotion) androidx.compose.animation.core.snap() else androidx.compose.animation.core.tween(180),
                        label = "zx-nav-content-${destination.name}",
                    )
                    val scale by animateFloatAsState(
                        targetValue = if (selected) 1.06f else 1f,
                        animationSpec = if (reduceMotion) androidx.compose.animation.core.snap() else androidx.compose.animation.core.spring(),
                        label = "zx-nav-scale-${destination.name}",
                    )
                    Surface(
                        onClick = { onSelect(destination) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .border(
                                width = if (selected) 1.dp else 0.dp,
                                brush = borderBrush,
                                shape = RoundedCornerShape(22.dp),
                            ),
                        shape = RoundedCornerShape(22.dp),
                        color = container,
                        contentColor = content,
                    ) {
                        Row(
                            Modifier.padding(horizontal = 3.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painterResource(destination.icon),
                                contentDescription = stringResource(destination.labelRes),
                                modifier = Modifier.size(21.dp).then(
                                    Modifier,
                                ).graphicsLayer(scaleX = scale, scaleY = scale),
                            )
                            AnimatedVisibility(visible = selected || tabs.size <= 4) {
                                Text(
                                    stringResource(destination.labelRes),
                                    modifier = Modifier.padding(start = 5.dp),
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

/** Subtle ambient light field behind the ZX interface. */
@Composable
fun ZxAmbientBackdrop(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Canvas(modifier) {
        drawRect(
            Brush.linearGradient(
                colors = listOf(
                    colors.background,
                    colors.surfaceContainerLow,
                    colors.background,
                ),
                start = androidx.compose.ui.geometry.Offset.Zero,
                end = androidx.compose.ui.geometry.Offset(size.width, size.height),
            ),
        )
        drawRect(
            Brush.linearGradient(
                colors = listOf(
                    colors.primary.copy(alpha = 0.035f),
                    colors.tertiary.copy(alpha = 0.045f),
                    colors.secondary.copy(alpha = 0.025f),
                    Color.Transparent,
                ),
                start = androidx.compose.ui.geometry.Offset(0f, size.height * 0.18f),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.92f),
            ),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colors.primary.copy(alpha = 0.16f), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(size.width * 0.08f, size.height * 0.12f),
                radius = size.minDimension * 0.78f,
            ),
            radius = size.minDimension * 0.78f,
            center = androidx.compose.ui.geometry.Offset(size.width * 0.08f, size.height * 0.12f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colors.tertiary.copy(alpha = 0.12f), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(size.width * 0.94f, size.height * 0.70f),
                radius = size.minDimension * 0.68f,
            ),
            radius = size.minDimension * 0.68f,
            center = androidx.compose.ui.geometry.Offset(size.width * 0.94f, size.height * 0.70f),
        )
        val gridColor = colors.outlineVariant.copy(alpha = 0.10f)
        val step = 32.dp.toPx()
        var x = 0f
        while (x <= size.width) {
            drawLine(gridColor, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), strokeWidth = 0.45.dp.toPx())
            x += step
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(gridColor, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 0.45.dp.toPx())
            y += step
        }
    }
}
