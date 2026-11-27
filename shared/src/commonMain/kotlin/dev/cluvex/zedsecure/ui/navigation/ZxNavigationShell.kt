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
        connectionState == ConnectionState.Connected -> Color(0xFF5CDCB4)
        connectionState == ConnectionState.Error -> MaterialTheme.colorScheme.error
        connectionState.isTransitioning -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
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
                .height(56.dp)
                .border(1.dp, borderBrush, shape),
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f),
            tonalElevation = 0.dp,
            shadowElevation = 12.dp,
        ) {
            Row(
                Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onMenuClick,
                    modifier = Modifier.size(46.dp).semantics {
                        contentDescription = if (drawerOpen) "Close navigation menu" else "Open navigation menu"
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    shadowElevation = 2.dp,
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
    // Smooth Aras-like morph: soft spring, clean X formation
    val progress = animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = if (reduceMotion) androidx.compose.animation.core.snap()
        else androidx.compose.animation.core.spring(
            dampingRatio = 0.72f,
            stiffness = 380f,
        ),
        label = "zx-menu-morph",
    ).value
    val lineColor = MaterialTheme.colorScheme.onSurface
    Canvas(Modifier.size(24.dp)) {
        val stroke = 2.35.dp.toPx()
        val cx = size.width * 0.5f
        val left = size.width * 0.17f
        val right = size.width * 0.83f
        val topY = size.height * 0.28f
        val midY = size.height * 0.50f
        val bottomY = size.height * 0.72f
        val halfLength = size.width * 0.33f
        val angle = progress * 0.7853982f // 45°
        val dx = kotlin.math.cos(angle.toDouble()).toFloat() * halfLength
        val dy = kotlin.math.sin(angle.toDouble()).toFloat() * halfLength
        val topCenterY = topY + (midY - topY) * progress
        val bottomCenterY = bottomY - (bottomY - midY) * progress

        // Outer lines travel inward + rotate into a crisp close glyph
        drawLine(
            lineColor,
            androidx.compose.ui.geometry.Offset(cx - dx, topCenterY - dy),
            androidx.compose.ui.geometry.Offset(cx + dx, topCenterY + dy),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        if (progress < 0.97f) {
            drawLine(
                lineColor.copy(alpha = (1f - progress).coerceIn(0f, 1f)),
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
                    destinations.forEach { destination ->
                    ZxDrawerTile(
                        destination = destination,
                        selected = current == destination,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSelect(destination) },
                    )
                    Spacer(Modifier.height(7.dp))
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
                        "ZX  ·  SIGNATURE INTERFACE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Precision layout. Clear navigation. Built for focus.",
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
    val shape = RoundedCornerShape(14.dp)
    val edge = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = if (selected) 0.85f else 0.24f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = if (selected) 0.48f else 0.12f),
        ),
    )
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 62.dp).border(if (selected) 1.dp else 0.6.dp, edge, shape),
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
        contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(destination.icon), contentDescription = stringResource(destination.labelRes), modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Text(
                stringResource(destination.labelRes),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Box(Modifier.size(width = 4.dp, height = 24.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.primary))
            }
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
        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.82f),
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f),
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
                destinations.forEach { destination ->
                    ZxDrawerTile(
                        destination = destination,
                        selected = current == destination,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSelect(destination) },
                    )
                    Spacer(Modifier.height(7.dp))
                }
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
            Spacer(Modifier.height(8.dp))
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
    val shape = RoundedCornerShape(18.dp)
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
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
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

/** Ambient light field behind the ZX interface — liquid aurora atmosphere. */
@Composable
fun ZxAmbientBackdrop(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Canvas(modifier) {
        // Deep atmospheric ground
        drawRect(
            Brush.verticalGradient(
                colors = listOf(
                    colors.background,
                    colors.surfaceContainerLow.copy(alpha = 0.85f),
                    colors.background,
                ),
            ),
        )
        // Soft liquid aurora wash
        drawRect(
            Brush.linearGradient(
                colors = listOf(
                    colors.primary.copy(alpha = 0.055f),
                    colors.tertiary.copy(alpha = 0.065f),
                    colors.secondary.copy(alpha = 0.035f),
                    Color.Transparent,
                ),
                start = androidx.compose.ui.geometry.Offset(0f, size.height * 0.12f),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.95f),
            ),
        )
        // Primary glow — top-left
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colors.primary.copy(alpha = 0.20f), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(size.width * 0.06f, size.height * 0.10f),
                radius = size.minDimension * 0.85f,
            ),
            radius = size.minDimension * 0.85f,
            center = androidx.compose.ui.geometry.Offset(size.width * 0.06f, size.height * 0.10f),
        )
        // Tertiary glow — bottom-right
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colors.tertiary.copy(alpha = 0.16f), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(size.width * 0.94f, size.height * 0.72f),
                radius = size.minDimension * 0.72f,
            ),
            radius = size.minDimension * 0.72f,
            center = androidx.compose.ui.geometry.Offset(size.width * 0.94f, size.height * 0.72f),
        )
        // Secondary soft bloom — center-right
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colors.secondary.copy(alpha = 0.08f), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(size.width * 0.78f, size.height * 0.38f),
                radius = size.minDimension * 0.55f,
            ),
            radius = size.minDimension * 0.55f,
            center = androidx.compose.ui.geometry.Offset(size.width * 0.78f, size.height * 0.38f),
        )
        // Very subtle holographic grid
        val gridColor = colors.outlineVariant.copy(alpha = 0.07f)
        val step = 36.dp.toPx()
        var x = 0f
        while (x <= size.width) {
            drawLine(gridColor, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), strokeWidth = 0.4.dp.toPx())
            x += step
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(gridColor, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 0.4.dp.toPx())
            y += step
        }
    }
}
