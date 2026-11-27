package dev.cluvex.zedsecure.ui.zx

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.cluvex.zedsecure.data.net.NetworkInfo
import dev.cluvex.zedsecure.domain.model.ConnectionState
import dev.cluvex.zedsecure.ui.bento.BentoActiveServer
import dev.cluvex.zedsecure.ui.bento.BentoHome
import dev.cluvex.zedsecure.ui.bento.BentoServerPick
import dev.cluvex.zedsecure.ui.connection.ConnectionUiState
import dev.cluvex.zedsecure.ui.theme.LocalMotionBudget
import dev.cluvex.zedsecure.ui.theme.MotionBudget

/**
 * ZX Reimagined: liquid-glass hybrid interface built on the shared Bento dashboard
 * and Material 3 navigation patterns. Adapter only — connection actions unchanged.
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

/** Preview tile used by the appearance picker for the liquid-glass ZX design. */
@Composable
internal fun ZxDesignPreview() {
    val cs = androidx.compose.material3.MaterialTheme.colorScheme
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
            .padding(8.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        // Glass top bar hint
        androidx.compose.material3.Surface(
            Modifier.fillMaxWidth().height(16.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
            color = cs.surfaceContainer.copy(alpha = 0.72f),
            shadowElevation = 2.dp,
        ) {}
        // Hero connect blob
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            color = cs.surfaceContainer.copy(alpha = 0.70f),
            shadowElevation = 4.dp,
        ) {
            androidx.compose.foundation.layout.Column(
                Modifier.padding(8.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                androidx.compose.material3.Surface(
                    Modifier.size(36.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = cs.primary.copy(alpha = 0.90f),
                    shadowElevation = 3.dp,
                ) {}
                androidx.compose.foundation.layout.Spacer(Modifier.height(6.dp))
                androidx.compose.material3.Surface(
                    Modifier.fillMaxWidth(0.55f).height(5.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = cs.onSurface.copy(alpha = 0.18f),
                ) {}
            }
        }
        // Clay chips row
        androidx.compose.foundation.layout.Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(5.dp),
        ) {
            androidx.compose.material3.Surface(
                Modifier.weight(1f).height(16.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                color = cs.secondaryContainer.copy(alpha = 0.85f),
            ) {}
            androidx.compose.material3.Surface(
                Modifier.weight(1f).height(16.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                color = cs.tertiaryContainer.copy(alpha = 0.85f),
            ) {}
        }
        // Floating glass dock
        androidx.compose.material3.Surface(
            Modifier.fillMaxWidth().height(14.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
            color = cs.surfaceContainer.copy(alpha = 0.68f),
            shadowElevation = 3.dp,
        ) {}
    }
}
