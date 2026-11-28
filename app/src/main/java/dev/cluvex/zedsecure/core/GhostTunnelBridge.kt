package dev.cluvex.zedsecure.core

import dev.cluvex.zedsecure.core.ghost.GhostEngine
import dev.cluvex.zedsecure.core.ghost.GhostStatusManager
import dev.cluvex.zedsecure.domain.model.GhostSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Coordinates the existing GHOST engine with the VPN session lifecycle.
 *
 * Also drives the advanced Ghost Status dashboard (DPI level, visibility,
 * adaptive ladder) via GhostStatusManager — without modifying GhostEngine
 * internals, tunnel configs, packet routing, or any other feature.
 */
internal object GhostTunnelBridge {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun onTunnelConnected(settings: GhostSettings) {
        // Existing engine path (unchanged)
        GhostEngine.updateSettings(settings)
        GhostEngine.onTunnelConnected()

        // Advanced status layer (new) — fire-and-forget
        scope.launch {
            val profile = when {
                settings.autoMode -> "auto"
                else -> settings.strategy?.lowercase()?.ifBlank { "auto" } ?: "auto"
            }
            GhostStatusManager.onEnabled(profile)
        }
    }

    fun onTunnelStopped() {
        // Existing engine path (unchanged)
        GhostEngine.onTunnelDisconnected()

        // Advanced status layer
        GhostStatusManager.onDisabled()
    }
}
