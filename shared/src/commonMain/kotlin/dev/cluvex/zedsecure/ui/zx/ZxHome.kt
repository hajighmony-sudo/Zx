package dev.cluvex.zedsecure.ui.zx

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.cluvex.zedsecure.data.net.NetworkInfo
import dev.cluvex.zedsecure.domain.model.ConnectionState
import dev.cluvex.zedsecure.ui.bento.BentoActiveServer
import dev.cluvex.zedsecure.ui.bento.BentoHome
import dev.cluvex.zedsecure.ui.bento.BentoServerPick
import dev.cluvex.zedsecure.ui.connection.ConnectionUiState
import dev.cluvex.zedsecure.ui.theme.LocalMotionBudget
import dev.cluvex.zedsecure.ui.theme.MotionBudget

/**
 * ZX Reimagined: a clean hybrid interface built on the shared Bento dashboard
 * and Material 3 navigation patterns. Kept as an adapter so the app shell and
 * all existing connection actions remain unchanged.
 */
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
    val motionAllowed = LocalMotionBudget.current != MotionBudget.Paused && !reduceMotion
    BentoHome(
        ui = ui,
        info = info,
        active = active,
        picks = picks,
        lockedNote = lockedNote,
        localProxyAddress = localProxyAddress,
        showConnectionInfo = showConnectionInfo,
        animate = motionAllowed,
        contentPadding = contentPadding,
        onToggleConnection = onToggleConnection,
        onBrowseConfigs = onBrowseConfigs,
        onPickServer = onPickServer,
        onOpenSpeedTest = onOpenSpeedTest,
        onOpenMap = onOpenMap,
        onRefreshInfo = onRefreshInfo,
        onSecretUnlocked = onSecretUnlocked,
        onShowLogs = onShowLogs,
        onBurst = onBurst,
        onStatusTap = onStatusTap,
        modifier = modifier,
    )
}

/** Preview tile used by the appearance picker for the new hybrid ZX design. */
@Composable
internal fun ZxDesignPreview() {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
            .padding(8.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        androidx.compose.foundation.layout.Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(5.dp),
        ) {
            androidx.compose.material3.Surface(
                Modifier.weight(1f).height(14.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {}
            androidx.compose.material3.Surface(
                Modifier.size(width = 24.dp, height = 14.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
            ) {}
        }
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp),
            color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer,
        ) {
            androidx.compose.foundation.layout.Column(
                Modifier.padding(7.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                androidx.compose.material3.Surface(
                    Modifier.size(34.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                ) {}
                androidx.compose.foundation.layout.Spacer(Modifier.height(6.dp))
                androidx.compose.material3.Surface(
                    Modifier.fillMaxWidth(0.58f).height(5.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
                ) {}
            }
        }
        androidx.compose.foundation.layout.Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(5.dp),
        ) {
            repeat(2) {
                androidx.compose.material3.Surface(
                    Modifier.weight(1f).height(16.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(7.dp),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer,
                ) {}
            }
        }
        androidx.compose.material3.Surface(
            Modifier.fillMaxWidth().height(14.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
            color = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
        ) {}
    }
}
