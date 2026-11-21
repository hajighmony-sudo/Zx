package dev.cluvex.zedsecure.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.cluvex.zedsecure.platform.LinkQuality
import dev.cluvex.zedsecure.platform.detectLinkQuality
import dev.cluvex.zedsecure.shared.resources.Res
import dev.cluvex.zedsecure.shared.resources.link_quality_down
import dev.cluvex.zedsecure.shared.resources.link_quality_internet_limited
import dev.cluvex.zedsecure.shared.resources.link_quality_internet_ok
import dev.cluvex.zedsecure.shared.resources.link_quality_metered
import dev.cluvex.zedsecure.shared.resources.link_quality_network
import dev.cluvex.zedsecure.shared.resources.link_quality_signal
import dev.cluvex.zedsecure.shared.resources.link_quality_subtitle
import dev.cluvex.zedsecure.shared.resources.link_quality_title
import dev.cluvex.zedsecure.shared.resources.link_quality_unknown
import dev.cluvex.zedsecure.shared.resources.link_quality_up
import dev.cluvex.zedsecure.shared.resources.link_quality_unmetered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
internal fun LinkQualityCard(modifier: Modifier = Modifier) {
    var quality by remember { mutableStateOf<LinkQuality?>(null) }

    LaunchedEffect(Unit) {
        suspend fun refresh() {
            quality = withContext(Dispatchers.Default) { detectLinkQuality() }
        }
        refresh()
        while (true) {
            delay(5_000)
            refresh()
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text(
                stringResource(Res.string.link_quality_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                stringResource(Res.string.link_quality_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Metric(
                    stringResource(Res.string.link_quality_network),
                    quality?.generation ?: stringResource(Res.string.link_quality_unknown),
                    Modifier.weight(1f),
                )
                Metric(
                    stringResource(Res.string.link_quality_signal),
                    quality?.signalDbm?.let { "$it dBm" } ?: stringResource(Res.string.link_quality_unknown),
                    Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Metric(
                    stringResource(Res.string.link_quality_down),
                    quality?.let { formatLinkRate(it.downKbps) } ?: stringResource(Res.string.link_quality_unknown),
                    Modifier.weight(1f),
                )
                Metric(
                    stringResource(Res.string.link_quality_up),
                    quality?.let { formatLinkRate(it.upKbps) } ?: stringResource(Res.string.link_quality_unknown),
                    Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))
            val status = when {
                quality == null -> stringResource(Res.string.link_quality_unknown)
                quality!!.validated -> stringResource(Res.string.link_quality_internet_ok)
                else -> stringResource(Res.string.link_quality_internet_limited)
            }
            Text(
                status,
                style = MaterialTheme.typography.labelMedium,
                color = if (quality?.validated == true) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            quality?.let {
                Text(
                    stringResource(if (it.metered) Res.string.link_quality_metered else Res.string.link_quality_unmetered),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(3.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatLinkRate(kbps: Int): String = when {
    kbps <= 0 -> "—"
    kbps >= 1_000 -> "${(kbps / 1_000f).roundToInt()} Mbps"
    else -> "$kbps Kbps"
}
