@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.cluvex.zedsecure.ui.servers

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import dev.cluvex.zedsecure.data.config.ConfigExplorerEntry
import dev.cluvex.zedsecure.data.config.DiscoveredConfig
import dev.cluvex.zedsecure.data.net.GeoDetails
import dev.cluvex.zedsecure.data.net.GeoLookup
import dev.cluvex.zedsecure.ui.platform.LocalPlatform
import dev.cluvex.zedsecure.ui.platform.rememberAppSheetState
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** One config as the Explorer UI sees it, whichever list it came from. */
internal data class ExplorerTarget(
    val link: String,
    val name: String,
    val protocol: String,
    val address: String,
    val port: Int,
    val sourceName: String,
    val ms: Int,
    val jitterMs: Int?,
    val samplesMs: List<Int>,
    val countryCode: String?,
    val ipAddress: String?,
    val lastCheckedAt: Long?,
    val profileId: String?,
    val verified: Boolean,
)

internal fun ConfigExplorerEntry.toTarget() = ExplorerTarget(
    link = link, name = name, protocol = protocol, address = address, port = port, sourceName = sourceName,
    ms = medianMs, jitterMs = jitterMs, samplesMs = samplesMs, countryCode = countryCode, ipAddress = ipAddress,
    lastCheckedAt = lastCheckedAt, profileId = profileId, verified = true,
)

internal fun DiscoveredConfig.toTarget() = ExplorerTarget(
    link = link, name = name, protocol = protocol, address = address, port = port, sourceName = sourceName,
    ms = ms, jitterMs = null, samplesMs = if (ms > 0) listOf(ms) else emptyList(), countryCode = countryCode,
    ipAddress = null, lastCheckedAt = null, profileId = null, verified = status == 1,
)

/** Countries users ask for first; they always get a card in the Countries tab, even when empty. */
internal val priorityCountries = listOf("US", "DE", "TH", "ES", "NL", "GB", "FR", "TR", "JP", "SG", "CA", "FI", "SE", "CH", "IT", "AE")

private val countryNames = mapOf(
    "US" to "آمریکا", "DE" to "آلمان", "TH" to "تایلند", "ES" to "اسپانیا", "NL" to "هلند", "GB" to "بریتانیا",
    "FR" to "فرانسه", "TR" to "ترکیه", "JP" to "ژاپن", "SG" to "سنگاپور", "CA" to "کانادا", "FI" to "فنلاند",
    "SE" to "سوئد", "CH" to "سوئیس", "IT" to "ایتالیا", "AE" to "امارات", "IR" to "ایران", "RU" to "روسیه",
    "UA" to "اوکراین", "PL" to "لهستان", "RO" to "رومانی", "BG" to "بلغارستان", "AT" to "اتریش", "BE" to "بلژیک",
    "NO" to "نروژ", "DK" to "دانمارک", "IE" to "ایرلند", "PT" to "پرتغال", "CZ" to "چک", "HU" to "مجارستان",
    "LT" to "لیتوانی", "LV" to "لتونی", "EE" to "استونی", "IN" to "هند", "HK" to "هنگ‌کنگ", "TW" to "تایوان",
    "KR" to "کره جنوبی", "CN" to "چین", "AU" to "استرالیا", "NZ" to "نیوزلند", "BR" to "برزیل", "AR" to "آرژانتین",
    "MX" to "مکزیک", "ZA" to "آفریقای جنوبی", "IL" to "اسرائیل", "SA" to "عربستان", "QA" to "قطر", "KZ" to "قزاقستان",
    "AM" to "ارمنستان", "GE" to "گرجستان", "MD" to "مولداوی", "RS" to "صربستان", "GR" to "یونان", "MY" to "مالزی",
    "ID" to "اندونزی", "VN" to "ویتنام", "PH" to "فیلیپین", "PK" to "پاکستان", "AZ" to "آذربایجان", "IS" to "ایسلند",
    "LU" to "لوکزامبورگ", "SK" to "اسلواکی", "SI" to "اسلوونی", "HR" to "کرواسی", "CY" to "قبرس", "MT" to "مالت",
)

internal fun countryNameFa(code: String?): String =
    if (code == null) "نامشخص" else countryNames[code.uppercase()] ?: code.uppercase()

/** A country card in the Countries tab. */
internal data class CountryGroup(val code: String, val items: List<ExplorerTarget>) {
    val best: Int? get() = items.filter { it.ms > 0 }.minOfOrNull { it.ms }
}

/** Priority countries first (fixed order, even when empty), then every other country by size. */
internal fun groupByCountry(targets: List<ExplorerTarget>): List<CountryGroup> {
    val byCode = targets.filter { it.countryCode != null }.groupBy { it.countryCode!! }
    val priority = priorityCountries.map { CountryGroup(it, (byCode[it] ?: emptyList()).sortedBy { t -> if (t.ms > 0) t.ms else Int.MAX_VALUE }) }
    val others = byCode.filterKeys { it !in priorityCountries }
        .map { (code, list) -> CountryGroup(code, list.sortedBy { t -> if (t.ms > 0) t.ms else Int.MAX_VALUE }) }
        .sortedByDescending { it.items.size }
    return priority + others
}

/**
 * Contacts-style swipe: drag right → [startLabel] action, drag left → [endLabel] action. The row always
 * springs back; the colours and labels behind it show what will happen.
 */
@Composable
internal fun SwipeActionRow(
    startLabel: String,
    endLabel: String,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    content: @Composable () -> Unit,
) {
    val outerDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val trigger = with(density) { 84.dp.toPx() }
    val maxPull = with(density) { 128.dp.toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val rightColor = MaterialTheme.colorScheme.primaryContainer
    val leftColor = MaterialTheme.colorScheme.tertiaryContainer
    val rowColor = MaterialTheme.colorScheme.surfaceContainerLow
    // Gestures are absolute (left/right), so the swipe container ignores the app's RTL direction.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))) {
            val x = offset.value
            if (x != 0f) {
                Row(
                    Modifier.matchParentSize().background(if (x > 0) rightColor else leftColor).padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (x > 0) Arrangement.Start else Arrangement.End,
                ) {
                    val armed = kotlin.math.abs(x) >= trigger
                    Text(
                        if (x > 0) startLabel else endLabel,
                        fontWeight = if (armed) FontWeight.ExtraBold else FontWeight.SemiBold,
                        fontSize = if (armed) 16.sp else 14.sp,
                        color = if (x > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
            Box(
                Modifier
                    .offset { IntOffset(offset.value.roundToInt(), 0) }
                    .background(rowColor)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                val end = offset.value
                                scope.launch {
                                    if (end >= trigger) onSwipeRight() else if (end <= -trigger) onSwipeLeft()
                                    offset.animateTo(0f, tween(220))
                                }
                            },
                            onDragCancel = { scope.launch { offset.animateTo(0f, tween(180)) } },
                        ) { change, amount ->
                            change.consume()
                            scope.launch { offset.snapTo((offset.value + amount).coerceIn(-maxPull, maxPull)) }
                        }
                    },
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides outerDirection) { content() }
            }
        }
    }
}

/** Small, cheap radar: a rotating sweep while scanning and one blip per config found so far. */
@Composable
internal fun ExplorerRadar(running: Boolean, found: Int, modifier: Modifier = Modifier) {
    if (running) {
        val transition = rememberInfiniteTransition(label = "explorer-radar")
        val angle by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "sweep")
        val pulse by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart), label = "pulse")
        RadarCanvas(angle, pulse, found, modifier)
    } else {
        RadarCanvas(-1f, 0f, found, modifier)
    }
}

@Composable
private fun RadarCanvas(angle: Float, pulse: Float, found: Int, modifier: Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val ring = primary.copy(alpha = 0.28f)
    Canvas(modifier.size(64.dp)) {
        val radius = size.minDimension / 2f
        val middle = Offset(size.width / 2f, size.height / 2f)
        drawCircle(ring, radius - 1f, middle, style = Stroke(2f))
        drawCircle(ring, radius * 0.66f, middle, style = Stroke(1.5f))
        drawCircle(ring, radius * 0.33f, middle, style = Stroke(1.5f))
        drawLine(ring, Offset(middle.x - radius, middle.y), Offset(middle.x + radius, middle.y), 1f)
        drawLine(ring, Offset(middle.x, middle.y - radius), Offset(middle.x, middle.y + radius), 1f)
        if (angle >= 0f) {
            rotate(angle, middle) {
                drawArc(
                    color = primary.copy(alpha = 0.32f), startAngle = -70f, sweepAngle = 70f, useCenter = true,
                    topLeft = Offset(middle.x - radius, middle.y - radius), size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                )
                drawLine(primary, middle, Offset(middle.x + radius, middle.y), strokeWidth = 3f)
            }
            drawCircle(primary.copy(alpha = (1f - pulse) * 0.45f), radius * pulse, middle, style = Stroke(3f))
        }
        val blips = min(found, 16)
        for (i in 0 until blips) {
            val theta = (i * 137.5f) * (kotlin.math.PI.toFloat() / 180f)
            val distance = radius * (0.22f + 0.68f * (((i * 37) % 100) / 100f))
            drawCircle(primary, 3.2f, Offset(middle.x + kotlin.math.cos(theta) * distance, middle.y + kotlin.math.sin(theta) * distance))
        }
        drawCircle(primary, 4f, middle)
    }
}

@Composable
internal fun ExplorerRenameDialog(initial: String, onSave: (String) -> Unit, onReset: () -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تغییر نام کانفیگ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = text, onValueChange = { text = it.take(80) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text(
                    "نام جدید هنگام استخراج و اشتراک‌گذاری لینک هم اعمال می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(text.trim()) }, enabled = text.isNotBlank()) { Text("ذخیره") } },
        dismissButton = {
            Row {
                TextButton(onClick = onReset) { Text("نام اصلی") }
                TextButton(onClick = onDismiss) { Text("انصراف") }
            }
        },
    )
}

@Composable
private fun DetailLine(label: String, value: String?, onCopy: ((String) -> Unit)? = null) {
    val shown = value?.takeIf { it.isNotBlank() } ?: return
    Row(
        Modifier.fillMaxWidth().then(if (onCopy != null) Modifier.clickable { onCopy(shown) } else Modifier).padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(label, Modifier.width(92.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(shown, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

/** Full details of one config, including a live IP-database lookup for country, city, ISP and ASN. */
@Composable
internal fun ExplorerDetailSheet(
    target: ExplorerTarget,
    displayName: String,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onConnect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: () -> Unit,
    onExport: () -> Unit,
) {
    val platform = LocalPlatform.current
    var geo by remember(target.address) { mutableStateOf<GeoDetails?>(null) }
    var loading by remember(target.address) { mutableStateOf(true) }
    LaunchedEffect(target.address) {
        loading = true
        geo = runCatching { GeoLookup.detailsOf(target.address) }.getOrNull()
        loading = false
    }
    val code = geo?.countryCode ?: target.countryCode
    val copy: (String) -> Unit = { platform.copyToClipboard(it); platform.toast("کپی شد") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberAppSheetState()) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(code?.let(::countryFlag) ?: "🌐", fontSize = 46.sp)
                Column(Modifier.weight(1f)) {
                    Text(displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        when {
                            code != null -> countryNameFa(code) + " (" + code + ")" + (geo?.city?.let { " · $it" } ?: "")
                            loading -> "در حال شناسایی کشور…"
                            else -> "کشور شناسایی نشد"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())

            DetailSection("موقعیت و شبکه (بر اساس IP)") {
                DetailLine("IP", geo?.ip ?: target.ipAddress, copy)
                DetailLine("کشور", code?.let { countryNameFa(it) + " · " + (geo?.country ?: it) })
                DetailLine("منطقه", geo?.region)
                DetailLine("شهر", geo?.city)
                DetailLine("شبکه (ISP)", geo?.isp, copy)
                DetailLine("ASN", geo?.asn, copy)
                if (geo?.latitude != null && geo?.longitude != null) {
                    DetailLine("مختصات", geo?.latitude.toString() + ", " + geo?.longitude.toString()) { copy(it) }
                }
                if (!loading && geo?.countryCode == null && target.ipAddress == null) {
                    Text(
                        "پایگاه‌داده IP پاسخ نداد؛ اینترنت را بررسی کنید و دوباره باز کنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            DetailSection("عملکرد") {
                if (target.samplesMs.isEmpty() && target.ms <= 0) {
                    Text(if (target.verified) "نمونه‌ای ثبت نشده" else "هنوز موفق نشده یا در حال تست است", style = MaterialTheme.typography.bodyMedium)
                } else {
                    val maxMs = max(1, target.samplesMs.maxOrNull() ?: target.ms)
                    Row(Modifier.fillMaxWidth().height(52.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                        (if (target.samplesMs.isEmpty()) listOf(target.ms) else target.samplesMs).forEach { sample ->
                            Box(
                                Modifier.width(22.dp).height(max(6, (46f * sample / maxMs).roundToInt()).dp)
                                    .clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.primary),
                            )
                        }
                    }
                    DetailLine("تأخیر", target.ms.toString() + " ms")
                    DetailLine("نوسان", target.jitterMs?.let { "$it ms" })
                    DetailLine("نمونه‌ها", target.samplesMs.joinToString("، ") { "$it ms" })
                    target.jitterMs?.let { jitter ->
                        val quality = (100 - target.ms / 12 - jitter / 8).coerceIn(1, 100)
                        DetailLine("امتیاز کیفیت", "$quality / 100")
                        LinearProgressIndicator(progress = quality / 100f, modifier = Modifier.fillMaxWidth())
                    }
                }
                DetailLine("وضعیت", if (target.verified) "✓ سالم (تست واقعی از مسیر هسته)" else "تأیید نشده")
                target.lastCheckedAt?.let { DetailLine("آخرین بررسی", relativeAgo(it)) }
            }

            DetailSection("کانفیگ") {
                DetailLine("نام", displayName)
                DetailLine("پروتکل", target.protocol)
                DetailLine("سرور", target.address + ":" + target.port, copy)
                DetailLine("منبع", target.sourceName)
                DetailLine("لینک", target.link.take(120) + if (target.link.length > 120) "…" else "") { copy(target.link) }
            }

            Button(onClick = onConnect, modifier = Modifier.fillMaxWidth()) {
                Text(if (target.profileId != null) "اتصال به این سرور" else "افزودن و اتصال")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRename, Modifier.weight(1f)) { Text("✏️ نام") }
                OutlinedButton(onClick = onToggleFavorite, Modifier.weight(1f)) { Text(if (isFavorite) "★ نشان" else "☆ نشان") }
                OutlinedButton(onClick = onExport, Modifier.weight(1f)) { Text("📤 استخراج") }
            }
            TextButton(onClick = onDismiss, Modifier.fillMaxWidth()) { Text("بستن") }
            HorizontalDivider()
        }
    }
}

@Composable
internal fun ExplorerTargetRow(index: Int, target: ExplorerTarget, name: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(index.toString().padStart(2, '0'), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                target.protocol + " · " + target.address + ":" + target.port + " · " + target.sourceName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (target.ms > 0) Text(target.ms.toString() + " ms", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}
