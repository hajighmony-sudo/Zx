@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package dev.cluvex.zedsecure.ui.zx

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.cluvex.zedsecure.data.net.NetworkInfo
import dev.cluvex.zedsecure.domain.model.ConnectionState
import dev.cluvex.zedsecure.ui.bento.BentoActiveServer
import dev.cluvex.zedsecure.ui.bento.BentoServerPick
import dev.cluvex.zedsecure.ui.connection.ConnectionUiState
import dev.cluvex.zedsecure.ui.format.formatBytes
import dev.cluvex.zedsecure.ui.format.formatElapsed
import dev.cluvex.zedsecure.ui.format.formatRate
import dev.cluvex.zedsecure.shared.resources.Res
import dev.cluvex.zedsecure.shared.resources.action_connect
import dev.cluvex.zedsecure.shared.resources.action_disconnect
import dev.cluvex.zedsecure.shared.resources.bento_connected_for
import dev.cluvex.zedsecure.shared.resources.bento_current_server
import dev.cluvex.zedsecure.shared.resources.bento_no_server
import dev.cluvex.zedsecure.shared.resources.bento_no_server_hint
import dev.cluvex.zedsecure.shared.resources.fault_open_logs
import dev.cluvex.zedsecure.shared.resources.home_download
import dev.cluvex.zedsecure.shared.resources.home_local_proxy
import dev.cluvex.zedsecure.shared.resources.home_upload
import dev.cluvex.zedsecure.shared.resources.ic_description
import dev.cluvex.zedsecure.shared.resources.ic_download
import dev.cluvex.zedsecure.shared.resources.ic_fill_map
import dev.cluvex.zedsecure.shared.resources.ic_fill_power
import dev.cluvex.zedsecure.shared.resources.ic_fill_speed
import dev.cluvex.zedsecure.shared.resources.ic_lock
import dev.cluvex.zedsecure.shared.resources.ic_upload
import dev.cluvex.zedsecure.shared.resources.logs_title
import dev.cluvex.zedsecure.shared.resources.map_title
import dev.cluvex.zedsecure.shared.resources.state_connected
import dev.cluvex.zedsecure.shared.resources.state_connecting
import dev.cluvex.zedsecure.shared.resources.state_error
import dev.cluvex.zedsecure.shared.resources.state_idle
import dev.cluvex.zedsecure.shared.resources.title_speedtest
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Independent ZX dashboard. It consumes the existing UI state and callbacks; connection logic stays in the host. */
@Composable
internal fun ZxHome(
    ui: ConnectionUiState,
    info: NetworkInfo,
    active: BentoActiveServer?,
    picks: List<BentoServerPick>,
    lockedNote: String?,
    localProxyAddress: String?,
    showConnectionInfo: Boolean,
    reduceMotion: Boolean,
    contentPadding: PaddingValues,
    onToggleConnection: () -> Unit,
    onBrowseConfigs: () -> Unit,
    onPickServer: (String) -> Unit,
    onOpenSpeedTest: (() -> Unit)?,
    onOpenMap: (() -> Unit)?,
    onRefreshInfo: () -> Unit,
    onSecretUnlocked: () -> Unit,
    onShowLogs: () -> Unit,
    onBurst: () -> Unit,
    onStatusTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val connected = ui.state == ConnectionState.Connected
    val transitioning = ui.state.isTransitioning
    val stateText = when {
        connected -> stringResource(Res.string.state_connected)
        transitioning -> stringResource(Res.string.state_connecting)
        ui.state == ConnectionState.Error -> stringResource(Res.string.state_error)
        else -> stringResource(Res.string.state_idle)
    }
    val stateColor = when {
        connected -> Color(0xFF159B78)
        transitioning -> cs.tertiary
        ui.state == ConnectionState.Error -> cs.error
        else -> cs.onSurfaceVariant
    }
    val down = formatRate(ui.downloadBps)
    val up = formatRate(ui.uploadBps)

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding() + 18.dp)
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp)
                .combinedClickable(onClick = onStatusTap, onLongClick = onSecretUnlocked),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("ZX / CONTROL CENTER", style = MaterialTheme.typography.labelSmall, color = cs.primary, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text("ZedSecure", style = MaterialTheme.typography.headlineSmall, color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
            }
            Surface(
                onClick = onRefreshInfo,
                shape = RoundedCornerShape(12.dp),
                color = cs.surfaceContainerHigh,
                border = androidx.compose.foundation.BorderStroke(1.dp, cs.outlineVariant),
            ) {
                Text("↻", Modifier.padding(horizontal = 14.dp, vertical = 9.dp), color = cs.onSurface, fontSize = 20.sp)
            }
        }

        // The connection console is deliberately a single strong surface, not a grid of Bento tiles.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 30.dp, bottomEnd = 8.dp, bottomStart = 30.dp),
            color = cs.surfaceContainer,
            border = androidx.compose.foundation.BorderStroke(1.dp, cs.outlineVariant),
            shadowElevation = 5.dp,
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(9.dp).clip(CircleShape).background(stateColor))
                    Spacer(Modifier.width(9.dp))
                    Text(stateText, color = stateColor, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    if (connected) Text(formatElapsed(ui.elapsedSeconds), color = cs.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                }
                Text(
                    if (connected) (active?.name ?: ui.activeConfigName ?: stringResource(Res.string.bento_current_server))
                    else if (active != null) active.name else stringResource(Res.string.bento_no_server),
                    style = MaterialTheme.typography.headlineMedium,
                    color = cs.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    when {
                        connected -> active?.detail ?: active?.engine.orEmpty()
                        active == null -> stringResource(Res.string.bento_no_server_hint)
                        else -> active.detail.orEmpty()
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Button(
                    onClick = onToggleConnection,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (connected) cs.error else cs.primary,
                        contentColor = if (connected) cs.onError else cs.onPrimary,
                    ),
                ) {
                    Icon(painterResource(Res.drawable.ic_fill_power), null, Modifier.size(21.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(if (connected || transitioning) Res.string.action_disconnect else Res.string.action_connect),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (!lockedNote.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(Res.drawable.ic_lock), null, tint = cs.tertiary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(lockedNote, color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (ui.state == ConnectionState.Error) {
                    Text(ui.error ?: stringResource(Res.string.state_error), color = cs.error, style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = onShowLogs) { Text(stringResource(Res.string.fault_open_logs)) }
                }
            }
        }

        if (showConnectionInfo) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ZxMetricCard(Modifier.weight(1f), stringResource(Res.string.home_download), down.first, down.second, Res.drawable.ic_download, cs.primary)
                ZxMetricCard(Modifier.weight(1f), stringResource(Res.string.home_upload), up.first, up.second, Res.drawable.ic_upload, cs.tertiary)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ZxMetricCard(Modifier.weight(1f), stringResource(Res.string.bento_data_session), formatBytes(ui.totalDownload + ui.totalUpload), "", Res.drawable.ic_description, cs.secondary)
                ZxMetricCard(Modifier.weight(1f), stringResource(Res.string.bento_connected_for), if (connected) formatElapsed(ui.elapsedSeconds) else "—", "", Res.drawable.ic_fill_power, cs.primary)
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onBrowseConfigs),
            shape = RoundedCornerShape(8.dp, 20.dp, 8.dp, 20.dp),
            color = cs.surfaceContainerHigh,
            border = androidx.compose.foundation.BorderStroke(1.dp, cs.outlineVariant),
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(Res.string.bento_current_server), color = cs.onSurface, fontWeight = FontWeight.Bold)
                    Text(active?.name ?: stringResource(Res.string.bento_no_server), color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                Text("‹", color = cs.primary, fontSize = 28.sp)
            }
        }

        if (picks.isNotEmpty()) {
            Text(stringResource(Res.string.bento_fastest), style = MaterialTheme.typography.titleMedium, color = cs.onSurface, fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                picks.take(3).forEach { pick ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { onPickServer(pick.id) },
                        shape = RoundedCornerShape(12.dp),
                        color = cs.surfaceContainerLow,
                        border = androidx.compose.foundation.BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.7f)),
                    ) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(pick.name, color = cs.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(pick.detail, color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(pick.pingMs?.let { "$it ms" } ?: "—", color = cs.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (localProxyAddress != null) {
            Text("${stringResource(Res.string.home_local_proxy)}: $localProxyAddress", color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (onOpenSpeedTest != null) ZxQuickAction(Modifier.weight(1f), stringResource(Res.string.title_speedtest), Res.drawable.ic_fill_speed, onOpenSpeedTest)
            if (onOpenMap != null) ZxQuickAction(Modifier.weight(1f), stringResource(Res.string.map_title), Res.drawable.ic_fill_map, onOpenMap)
            ZxQuickAction(Modifier.weight(1f), stringResource(Res.string.logs_title), Res.drawable.ic_description, onShowLogs)
        }
        // Retain the existing hidden interaction hook without coupling it to connection behavior.
        Text("ZX", Modifier.align(Alignment.CenterHorizontally).clickable(onClick = onBurst).padding(6.dp), color = cs.onSurfaceVariant.copy(alpha = 0.45f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ZxMetricCard(
    modifier: Modifier,
    label: String,
    value: String,
    unit: String,
    icon: org.jetbrains.compose.resources.DrawableResource,
    accent: Color,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier,
        shape = RoundedCornerShape(9.dp, 18.dp, 9.dp, 18.dp),
        color = cs.surfaceContainerHigh,
        border = androidx.compose.foundation.BorderStroke(1.dp, cs.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(icon), null, tint = accent, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text(label, color = cs.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = cs.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                if (unit.isNotBlank()) Text(" $unit", color = cs.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun ZxQuickAction(modifier: Modifier, label: String, icon: org.jetbrains.compose.resources.DrawableResource, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(12.dp), color = cs.surfaceContainerLow, border = androidx.compose.foundation.BorderStroke(1.dp, cs.outlineVariant)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(painterResource(icon), null, tint = cs.primary, modifier = Modifier.size(21.dp))
            Text(label, color = cs.onSurface, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

/** Preview tile used by the appearance picker for the ZX control-center design. */
@Composable
internal fun ZxDesignPreview() {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().background(cs.background).padding(8.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Surface(Modifier.fillMaxWidth().height(13.dp), shape = RoundedCornerShape(6.dp), color = cs.surfaceContainerHigh) {}
        Surface(Modifier.weight(1f).fillMaxWidth(), shape = RoundedCornerShape(7.dp, 18.dp, 7.dp, 18.dp), color = cs.surfaceContainer, border = androidx.compose.foundation.BorderStroke(1.dp, cs.outlineVariant)) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(Modifier.size(24.dp), shape = CircleShape, color = cs.primary) {}
                Surface(Modifier.fillMaxWidth(0.62f).height(7.dp), shape = CircleShape, color = cs.onSurface.copy(alpha = 0.18f)) {}
                Surface(Modifier.fillMaxWidth().height(22.dp), shape = RoundedCornerShape(5.dp), color = cs.primary) {}
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(2) { Surface(Modifier.weight(1f).height(18.dp), shape = RoundedCornerShape(5.dp), color = cs.surfaceContainerHigh) {} }
        }
        Surface(Modifier.fillMaxWidth().height(13.dp), shape = RoundedCornerShape(6.dp), color = cs.surfaceContainerHigh) {}
    }
}
