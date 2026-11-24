package dev.cluvex.zedsecure

import android.content.Context
import android.net.VpnService
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import dev.cluvex.zedsecure.core.AndroidVpn
import dev.cluvex.zedsecure.core.Ikev2Controller
import dev.cluvex.zedsecure.core.StartPlanner
import dev.cluvex.zedsecure.core.VpnManager
import dev.cluvex.zedsecure.data.config.ConfigRepository
import dev.cluvex.zedsecure.data.net.PingService
import dev.cluvex.zedsecure.domain.config.VpnProfile
import dev.cluvex.zedsecure.domain.model.ConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Compact, dedicated Quick Settings profile picker.
 * It is launched by ACTION_QS_TILE_PREFERENCES when the ZX tile is long-pressed.
 */
class QuickConnectActivity : ComponentActivity() {
    private val configRepository: ConfigRepository
        get() = (application as ZedSecureApp).container.configRepository

    private val planner by lazy { StartPlanner(this) }
    private var pendingStart: StartPlanner.Plan.Start? = null
    private var pendingIkev2Remark: String? = null
    private var pingJob: Job? = null
    private var switching = false

    private var connectingProfileId by mutableStateOf<String?>(null)
    private val pingingProfiles = mutableStateMapOf<String, Boolean>()

    private val vpnPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val plan = pendingStart
            pendingStart = null
            if (result.resultCode == RESULT_OK && plan != null) {
                startVpn(plan)
            } else if (result.resultCode != RESULT_OK) {
                toast(getString(R.string.vpn_permission_denied))
            }
        }

    private val ikev2ConsentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val remark = pendingIkev2Remark
            pendingIkev2Remark = null
            if (result.resultCode == RESULT_OK && remark != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    Ikev2Controller.startProvisioned(this@QuickConnectActivity, remark)
                    withContext(Dispatchers.Main) { finish() }
                }
            } else {
                Ikev2Controller.forgetProvisionedProfile(this)
                VpnManager.onDisconnected()
                if (result.resultCode != RESULT_OK) toast(getString(R.string.vpn_permission_denied))
            }
        }

    override fun attachBaseContext(newBase: Context) {
        val tag = dev.cluvex.zedsecure.data.settings.SettingsRepository.readLanguageTag(newBase)
        super.attachBaseContext(dev.cluvex.zedsecure.core.platform.LocaleManager.wrap(newBase, tag))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                (resources.displayMetrics.widthPixels * 0.92f).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT,
            )
            setGravity(Gravity.CENTER)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes.apply { dimAmount = 0.56f }
        }

        setContent {
            val profiles by configRepository.profiles.collectAsState()
            val activeId by configRepository.activeId.collectAsState()
            val status by VpnManager.status.collectAsState()
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFFBFC8FF),
                surface = Color(0xFF24252A),
                background = Color(0xFF17181C),
            )) {
                QuickConnectContent(
                    profiles = profiles,
                    activeId = activeId,
                    connectionState = status.state,
                    serverName = status.serverName,
                    connectingProfileId = connectingProfileId,
                    pingingProfiles = pingingProfiles,
                    onSelect = ::connectProfile,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPings()
    }

    override fun onDestroy() {
        pingJob?.cancel()
        super.onDestroy()
    }

    private fun refreshPings() {
        pingJob?.cancel()
        val snapshot = configRepository.profiles.value
        if (snapshot.isEmpty()) return
        snapshot.forEach { pingingProfiles[it.id] = true }
        pingJob = lifecycleScope.launch {
            val permits = Semaphore(6)
            val results = withContext(Dispatchers.IO) {
                snapshot.map { profile ->
                    async {
                        val latency = permits.withPermit {
                            when {
                                profile.isManagedTunnel || profile.isProxyChain -> null
                                profile.address.isBlank() || profile.address == "-" || profile.port !in 1..65535 -> null
                                profile.protocol.uppercase() in UDP_PING_PROTOCOLS ->
                                    runCatching { PingService.realDelay(profile) }
                                        .getOrDefault(-1L)
                                        .let { it.takeIf { value -> value > 0 }?.toInt() ?: PingService.FAILED_PING }
                                else ->
                                    runCatching { PingService.tcpPing(profile.address, profile.port) }
                                        .getOrDefault(-1L)
                                        .let { it.takeIf { value -> value > 0 }?.toInt() ?: PingService.FAILED_PING }
                            }
                        }
                        profile.id to latency
                    }
                }.awaitAll()
            }
            results.forEach { (id, latency) ->
                configRepository.setPing(id, latency, persist = false)
                pingingProfiles[id] = false
            }
            configRepository.flushProfiles()
        }
    }

    private fun connectProfile(profile: VpnProfile) {
        if (switching) return
        lifecycleScope.launch {
            switching = true
            connectingProfileId = profile.id
            try {
                val current = VpnManager.status.value
                if (current.state.isActive && configRepository.activeId.value == profile.id) {
                    finish()
                    return@launch
                }

                if (current.state.isActive || current.state.isTransitioning) {
                    if (Ikev2Controller.isActive) Ikev2Controller.stop(this@QuickConnectActivity)
                    else AndroidVpn.stop(this@QuickConnectActivity)

                    val stopped = withTimeoutOrNull(12_000L) {
                        VpnManager.status.first { !it.state.isActive && !it.state.isTransitioning }
                        true
                    } == true
                    if (!stopped) {
                        toast(getString(R.string.switch_server_timeout))
                        return@launch
                    }
                }

                configRepository.setActive(profile.id)
                when (val plan = withContext(Dispatchers.IO) { planner.plan(profile) }) {
                    is StartPlanner.Plan.Failure -> toast(plan.message)
                    is StartPlanner.Plan.Ikev2 -> startIkev2(plan.remark, plan.profile)
                    is StartPlanner.Plan.Start -> {
                        val consent = VpnService.prepare(this@QuickConnectActivity)
                        if (consent != null) {
                            pendingStart = plan
                            vpnPermissionLauncher.launch(consent)
                        } else {
                            startVpn(plan)
                        }
                    }
                }
            } finally {
                switching = false
                connectingProfileId = null
            }
        }
    }

    private fun startVpn(plan: StartPlanner.Plan.Start) {
        val started = runCatching {
            AndroidVpn.start(
                this,
                plan.configJson,
                plan.remark,
                plan.socksPort,
                plan.kind,
                plan.engine,
                proxyOnly = plan.proxyOnly,
            )
        }.isSuccess
        if (started) finish() else toast(getString(R.string.state_error))
    }

    private suspend fun startIkev2(
        remark: String,
        profile: dev.cluvex.zedsecure.domain.config.Ikev2Profile,
    ) {
        if (!Ikev2Controller.isSupportedSdk || !Ikev2Controller.isSupported(this)) {
            toast(getString(R.string.ikev2_requires_android11))
            return
        }
        if (VpnManager.status.value.state.isActive) AndroidVpn.stop(this)
        VpnManager.onStarting(remark)
        val consent = try {
            withContext(Dispatchers.IO) { Ikev2Controller.provision(this@QuickConnectActivity, profile) }
        } catch (error: Exception) {
            VpnManager.onError(error.message ?: getString(R.string.ikev2_failed_generic))
            return
        }
        if (consent != null) {
            pendingIkev2Remark = remark
            ikev2ConsentLauncher.launch(consent)
        } else {
            withContext(Dispatchers.IO) { Ikev2Controller.startProvisioned(this@QuickConnectActivity, remark) }
            finish()
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    private companion object {
        val UDP_PING_PROTOCOLS = setOf("WIREGUARD", "AMNEZIAWG", "HYSTERIA", "MASQUE")
    }
}

@Composable
private fun QuickConnectContent(
    profiles: List<VpnProfile>,
    activeId: String?,
    connectionState: ConnectionState,
    serverName: String?,
    connectingProfileId: String?,
    pingingProfiles: Map<String, Boolean>,
    onSelect: (VpnProfile) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF24252A),
        shape = RoundedCornerShape(30.dp),
        tonalElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.quick_connect_title), color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = when {
                            connectionState.isActive -> serverName ?: stringResource(R.string.quick_connect_connected)
                            connectionState.isTransitioning -> stringResource(R.string.quick_connect_connecting)
                            else -> stringResource(R.string.quick_connect_subtitle),
                        },
                        color = Color(0xFFB8BBC4),
                        fontSize = 13.sp,
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            if (connectionState.isActive) Color(0xFF234D3B) else Color(0xFF3B3D44),
                            RoundedCornerShape(50),
                        )
                        .padding(horizontal = 11.dp, vertical = 7.dp),
                ) {
                    Text(
                        if (connectionState.isActive) "CONNECTED" else "ZX",
                        color = if (connectionState.isActive) Color(0xFF8BE0B0) else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (profiles.isEmpty()) {
                Text(
                    text = stringResource(R.string.quick_connect_empty),
                    color = Color(0xFFB8BBC4),
                    modifier = Modifier.padding(vertical = 28.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    items(profiles, key = { it.id }) { profile ->
                        val selected = activeId == profile.id
                        val connecting = connectingProfileId == profile.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (selected) Color(0xFF383B44) else Color(0xFF2D2F35),
                                    RoundedCornerShape(17.dp),
                                )
                                .clickable(enabled = !connecting) { onSelect(profile) }
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    profile.name,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = when {
                                        connecting -> stringResource(R.string.quick_connect_connecting)
                                        selected && connectionState.isActive -> "Connected"
                                        else -> profile.protocol,
                                    },
                                    color = Color(0xFFB8BBC4),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = when {
                                    pingingProfiles[profile.id] == true -> "…"
                                    (profile.lastPingMs ?: -1) > 0 -> "${profile.lastPingMs} ms"
                                    profile.lastPingMs == PingService.FAILED_PING -> stringResource(R.string.quick_connect_timeout)
                                    else -> "—"
                                },
                                color = if ((profile.lastPingMs ?: -1) > 0) Color(0xFF8BE0B0) else Color(0xFFB8BBC4),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = when {
                                    connecting -> "•••"
                                    selected && connectionState.isActive -> "✓"
                                    else -> "›"
                                },
                                color = if (selected && connectionState.isActive) Color(0xFF8BE0B0) else Color.White,
                                fontSize = 20.sp,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
