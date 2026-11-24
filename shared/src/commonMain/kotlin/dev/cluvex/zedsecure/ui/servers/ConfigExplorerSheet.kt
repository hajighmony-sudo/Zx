@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package dev.cluvex.zedsecure.ui.servers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.cluvex.zedsecure.data.config.ConfigExplorerEngine
import dev.cluvex.zedsecure.data.config.ConfigExplorerEntry
import dev.cluvex.zedsecure.data.config.ConfigRepository
import dev.cluvex.zedsecure.platform.currentTimeMillis
import dev.cluvex.zedsecure.ui.platform.LocalPlatform
import dev.cluvex.zedsecure.ui.platform.rememberAppSheetState

@Composable
fun ConfigExplorerSheet(
    repository: ConfigRepository,
    onDismiss: () -> Unit,
    onServerActivated: () -> Unit,
) {
    val state by repository.configExplorer.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf("found") }
    val discovered by repository.configExplorer.discovered.collectAsStateWithLifecycle()
    var detailTarget by remember { mutableStateOf<ExplorerTarget?>(null) }
    var renameTarget by remember { mutableStateOf<ExplorerTarget?>(null) }
    var expandedCountry by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var protoFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var sort by rememberSaveable { mutableStateOf("latency") }
    var countryFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var fastOnly by rememberSaveable { mutableStateOf(false) }
    var newSource by rememberSaveable { mutableStateOf("") }
    var hunting by remember { mutableStateOf(false) }
    var huntMessage by remember { mutableStateOf<String?>(null) }
    val platform = LocalPlatform.current

    LaunchedEffect(repository) { ConfigExplorerEngine.start(repository) }
    // While the best list is still empty, show the live findings instead of a blank "preparing" page.
    LaunchedEffect(state.healthyPool.isEmpty(), state.liveEntries.isNotEmpty()) {
        if (tab == "best" && state.healthyPool.isEmpty() && state.liveEntries.isNotEmpty()) tab = "live"
    }

    val visibleHealthyCount = (state.healthyPool + state.liveEntries).distinctBy { it.id }.size

    val entries = when (tab) {
        "found" -> emptyList()
        "live" -> state.liveEntries
        "stars" -> state.favorites
        "reserve" -> state.healthyPool.drop(ConfigExplorerEngine.BEST_LIMIT)
        "pool" -> state.healthyPool
        else -> state.healthyPool.take(ConfigExplorerEngine.BEST_LIMIT)
    }

    val foundShown = discovered
        .filter { protoFilter == null || it.protocol == protoFilter }
        .filter {
            val q = query.trim().lowercase()
            q.isEmpty() || it.name.lowercase().contains(q) || it.address.lowercase().contains(q) ||
                it.protocol.lowercase().contains(q) || it.sourceName.lowercase().contains(q)
        }
        .sortedWith(compareBy<dev.cluvex.zedsecure.data.config.DiscoveredConfig> {
            when (it.status) { 1 -> 0; 0 -> 1; else -> 2 }
        }.thenBy { if (it.status == 1) it.ms else 0 })
    val foundAlive = discovered.count { it.status == 1 }
    val foundDone = discovered.count { it.status != 0 }
    val protocols = (if (tab == "found") discovered.map { it.protocol } else entries.map { it.protocol }).distinct().sorted()
    val countries = entries.mapNotNull { it.countryCode }.distinct().sorted()
    val shown = entries
        .distinctBy { it.id }
        .filter { protoFilter == null || it.protocol == protoFilter }
        .filter { countryFilter == null || it.countryCode == countryFilter }
        .filter { !fastOnly || it.medianMs in 1..300 }
        .filter {
            val q = query.trim().lowercase()
            q.isEmpty() || it.name.lowercase().contains(q) || it.address.lowercase().contains(q) ||
                (it.countryCode?.lowercase() ?: "").contains(q) || it.protocol.lowercase().contains(q) ||
                it.sourceName.lowercase().contains(q)
        }
        .let { list ->
            when (sort) {
                "jitter" -> list.sortedBy { it.jitterMs }
                "recent" -> list.sortedByDescending { it.lastCheckedAt }
                "quality" -> list.sortedByDescending { explorerQuality(it) }
                else -> list
            }
        }

    fun nameOf(t: ExplorerTarget): String =
        state.renames[t.link] ?: t.name.ifBlank { t.protocol + " · " + t.address }
    val allTargets = ((state.healthyPool + state.liveEntries + state.favorites).map { it.toTarget() } +
        discovered.filter { it.status == 1 }.map { it.toTarget() })
        .distinctBy { it.link }
        .filter {
            val q = query.trim().lowercase()
            q.isEmpty() || nameOf(it).lowercase().contains(q) || it.address.lowercase().contains(q) ||
                countryNameFa(it.countryCode).contains(q) || (it.countryCode?.lowercase() ?: "").contains(q) ||
                it.protocol.lowercase().contains(q)
        }
    val countryGroups = groupByCountry(allTargets)
    val unknownCountry = allTargets.count { it.countryCode == null }

    fun exportTarget(t: ExplorerTarget) {
        val link = dev.cluvex.zedsecure.data.config.LinkRename.apply(t.link, nameOf(t))
        platform.copyToClipboard(link)
        platform.shareText(link)
        platform.toast("لینک استخراج و کپی شد · " + nameOf(t))
    }
    fun connectTarget(t: ExplorerTarget) {
        val id = t.profileId ?: run {
            repository.importText(t.link, repository.configExplorer.state.value.groupId.orEmpty())
            repository.profiles.value.firstOrNull { it.rawPayload()?.trim() == t.link.trim() }?.id
        }
        if (id != null) {
            state.renames[t.link]?.let { repository.rename(id, it) }
            repository.setActive(id); onServerActivated(); onDismiss()
        } else {
            platform.toast("افزودن این سرور انجام نشد")
        }
    }
    @Composable
    fun SwipeRow(t: ExplorerTarget, content: @Composable () -> Unit) {
        SwipeActionRow(
            startLabel = "✏️ تغییر نام",
            endLabel = "📤 استخراج",
            onSwipeRight = { renameTarget = t },
            onSwipeLeft = { exportTarget(t) },
            content = content,
        )
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val startHunt: (String) -> Unit = { input ->
        if (!hunting) {
            hunting = true
            huntMessage = "در حال شکار لینک…"
            scope.launch {
                val (ok, message) = runCatching { ConfigExplorerEngine.hunt(repository, input) }
                    .getOrElse { false to (it.message?.take(120) ?: "شکار لینک انجام نشد") }
                huntMessage = message
                hunting = false
                if (ok) {
                    newSource = ""
                    query = ""
                    tab = "found"
                }
                platform.toast(message)
            }
        }
    }
    // Reset the scroll position when the list being viewed changes.
    LaunchedEffect(tab, sort, protoFilter, countryFilter, fastOnly) { listState.scrollToItem(0) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberAppSheetState()) {
      Box(Modifier.fillMaxWidth()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ExplorerRadar(
                    running = state.isRunning,
                    found = maxOf(discovered.count { it.status == 1 }, visibleHealthyCount),
                    modifier = Modifier.padding(end = 12.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text("کاوشگر ZX", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "کشف خودکار، آزمایش واقعی و رتبه‌بندی سرورها",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onDismiss) { Text("بستن") }
            }
            }
            item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExplorerMetric("کاندید", state.candidateCount.toString(), Modifier.weight(1f))
                ExplorerMetric("قابل‌تحلیل", state.parsedCount.toString(), Modifier.weight(1f))
                ExplorerMetric("سالم", visibleHealthyCount.toString(), Modifier.weight(1f))
                ExplorerMetric("برتر", state.healthyPool.take(ConfigExplorerEngine.BEST_LIMIT).size.toString(), Modifier.weight(1f))
            }
            }
            item {
                if (state.isRunning) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { ConfigExplorerEngine.stop(repository) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("⏹ توقف کاوش (یافته‌ها حفظ می‌شود)") }
                } else {
                    Button(
                        onClick = { ConfigExplorerEngine.runNow(repository) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("▶ شروع کاوش") }
                }
            }
            val best = state.healthyPool.firstOrNull { it.profileId != null }
            if (best != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "بهترین سرور فعلی: " + (best.countryCode?.let(::countryFlag) ?: "🌐") + " " +
                                best.protocol + " · " + best.medianMs + " ms · امتیاز " + explorerQuality(best),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        val pool = state.healthyPool
                        val protoStats = pool.groupingBy { it.protocol }.eachCount().entries
                            .sortedByDescending { it.value }.take(4)
                            .joinToString(" · ") { it.key + " " + it.value }
                        val countryStats = pool.mapNotNull { it.countryCode }.groupingBy { it }.eachCount().entries
                            .sortedByDescending { it.value }.take(5)
                            .joinToString("  ") { countryFlag(it.key) + it.value }
                        if (protoStats.isNotBlank()) {
                            Text(protoStats, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        if (countryStats.isNotBlank()) {
                            Text(countryStats, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Button(
                            onClick = { best.profileId?.let { repository.setActive(it); onServerActivated(); onDismiss() } },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("اتصال سریع به بهترین سرور") }
                    }
                }
            }
            }
            item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("چرخه خودکار", fontWeight = FontWeight.SemiBold)
                            Text(
                                "اجرای خودکار هر ۱۵ دقیقه (پیش‌فرض خاموش). با دکمهٔ «شروع کاوش» هر وقت خواستید دستی اجرا کنید",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = state.enabled,
                            onCheckedChange = { ConfigExplorerEngine.setEnabled(repository, it) },
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "چرخهٔ خودکار فقط روی Wi-Fi (صرفه‌جویی در دیتای موبایل)",
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Switch(
                            checked = state.wifiOnly,
                            onCheckedChange = { repository.configExplorer.setWifiOnly(it) },
                        )
                    }
                    if (state.isRunning) {
                        Text(explorerPhaseLabel(state.phase), style = MaterialTheme.typography.labelMedium)
                        Text(
                            "زمان سپری‌شده " + (((currentTimeMillis() - state.startedAt) / 1000).coerceAtLeast(0)) +
                                " ثانیه · سالم‌های تأییدشده تا اینجا: " + visibleHealthyCount,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val fraction = if (state.progressTotal > 0) {
                            state.progressDone.toFloat() / state.progressTotal.toFloat()
                        } else 0f
                        LinearProgressIndicator(
                            progress = fraction.coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            state.progressDone.toString() + " / " + state.progressTotal,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            state.lastError ?: state.lastRunAt?.let { last ->
                                "آخرین چرخه " + relativeAgo(last) + " · چرخهٔ بعدی " + relativeIn(last + ConfigExplorerEngine.INTERVAL_MS)
                            } ?: "هنوز چرخه‌ای اجرا نشده است",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.lastError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "چرخه " + state.cycleCount + " · تعداد تست‌ها " + state.testedCount,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            }
            stickyHeader {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                ExplorerTab("کشف‌شده‌ها " + foundAlive + "/" + discovered.size, tab == "found") { tab = "found" }
                ExplorerTab("یافته‌های زنده " + state.liveEntries.size, tab == "live") { tab = "live" }
                ExplorerTab("۱۰۰ برتر", tab == "best") { tab = "best" }
                ExplorerTab("ذخیره جایگزین", tab == "reserve") { tab = "reserve" }
                ExplorerTab("همه سالم‌ها", tab == "pool") { tab = "pool" }
                ExplorerTab("🌍 کشورها " + countryGroups.count { it.items.isNotEmpty() }, tab == "countries") { tab = "countries" }
                ExplorerTab("★ نشان‌دارها " + state.favorites.size, tab == "stars") { tab = "stars" }
                ExplorerTab("🎯 شکارچی و منابع", tab == "sources") { tab = "sources" }
                        }
                        HorizontalDivider()
                    }
                }
            }
            if (tab != "sources") {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text("جست‌وجو یا چسباندن لینک GitHub / raw") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (dev.cluvex.zedsecure.data.config.LinkHunter.looksLikeLink(query)) {
                    Button(
                        onClick = { startHunt(query) },
                        enabled = !hunting,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (hunting) "در حال شکار…" else "🎯 شکار این لینک و کاوش کانفیگ‌ها") }
                }
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(selected = protoFilter == null, onClick = { protoFilter = null }, label = { Text("همه") })
                    protocols.forEach { proto ->
                        FilterChip(
                            selected = protoFilter == proto,
                            onClick = { protoFilter = if (protoFilter == proto) null else proto },
                            label = { Text(proto) },
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(selected = sort == "latency", onClick = { sort = "latency" }, label = { Text("تأخیر") })
                    FilterChip(selected = sort == "jitter", onClick = { sort = "jitter" }, label = { Text("کمترین نوسان") })
                    FilterChip(selected = sort == "quality", onClick = { sort = "quality" }, label = { Text("امتیاز کیفیت") })
                    FilterChip(selected = sort == "recent", onClick = { sort = "recent" }, label = { Text("جدیدترین") })
                    FilterChip(selected = fastOnly, onClick = { fastOnly = !fastOnly }, label = { Text("فقط سریع (≤۳۰۰ms)") })
                    Text(
                        shown.size.toString() + " نتیجه",
                        Modifier.padding(start = 6.dp, top = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (countries.size > 1) {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        FilterChip(selected = countryFilter == null, onClick = { countryFilter = null }, label = { Text("همه کشورها") })
                        countries.forEach { code ->
                            FilterChip(
                                selected = countryFilter == code,
                                onClick = { countryFilter = if (countryFilter == code) null else code },
                                label = { Text(countryFlag(code) + " " + code) },
                            )
                        }
                    }
                }
                if (shown.isNotEmpty()) {
                    TextButton(onClick = {
                        platform.copyToClipboard(shown.joinToString("\n") { it.link })
                        platform.toast(shown.size.toString() + " لینک کپی شد")
                    }) { Text("کپی همهٔ لینک‌های این فهرست (" + shown.size + ")") }
                }
                }
            }
            }
            if (tab == "sources") {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🎯 شکارچی لینک ZX", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "هر لینکی بدهید: صفحهٔ مخزن GitHub، یک پوشه، صفحهٔ فایل (blob)، لینک raw، Gist یا هر فید https. " +
                                "شکارچی فایل‌های شبیه کانفیگ را پیدا می‌کند، محتوایشان را راستی‌آزمایی می‌کند و کاوش را شروع می‌کند.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        OutlinedTextField(
                            value = newSource,
                            onValueChange = { newSource = it },
                            singleLine = true,
                            label = { Text("github.com/… یا raw.githubusercontent.com/… یا https://…") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = { startHunt(newSource) },
                            enabled = !hunting && newSource.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(if (hunting) "در حال شکار…" else "شکار و افزودن") }
                        huntMessage?.let {
                            Text(it, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                ConfigExplorerEngine.allSources(state).forEach { source ->
                    val status = state.sourceStatuses.firstOrNull { it.sourceId == source.id }
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(source.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            Text(
                                when {
                                    source.feedUrl == null -> "مرجع مکمل"
                                    status == null -> "در انتظار"
                                    status.ok -> "دریافت موفق"
                                    else -> "خطا"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    source.feedUrl == null -> MaterialTheme.colorScheme.onSurfaceVariant
                                    status?.ok == true -> MaterialTheme.colorScheme.primary
                                    status == null -> MaterialTheme.colorScheme.onSurfaceVariant
                                    else -> MaterialTheme.colorScheme.error
                                },
                            )
                        }
                        Text(
                            source.repositoryUrl,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            status?.let { it.fetchedCount.toString() + " لینک · " + it.message } ?: source.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (source.id.startsWith("custom-")) {
                        TextButton(onClick = { repository.configExplorer.removeCustomSource(source.repositoryUrl) }) { Text("حذف این منبع") }
                    }
                    HorizontalDivider()
                }
                }
            }
            } else if (tab == "countries") {
                item {
                    Text(
                        allTargets.size.toString() + " کانفیگ سالم · " + countryGroups.count { it.items.isNotEmpty() } + " کشور" +
                            (if (unknownCountry > 0) " · کشور " + unknownCountry + " مورد در حال شناسایی" else ""),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                countryGroups.forEach { group ->
                    item(key = "country-" + group.code) {
                        val open = expandedCountry == group.code
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (open) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth().clickable(enabled = group.items.isNotEmpty()) {
                                expandedCountry = if (open) null else group.code
                            },
                        ) {
                            Row(
                                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(countryFlag(group.code), fontSize = 28.sp)
                                Column(Modifier.weight(1f)) {
                                    Text(countryNameFa(group.code), fontWeight = FontWeight.Bold)
                                    Text(
                                        if (group.items.isEmpty()) "هنوز کانفیگی نیست" else
                                            group.items.size.toString() + " کانفیگ" + (group.best?.let { " · بهترین " + it + " ms" } ?: ""),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(if (group.items.isEmpty()) "—" else if (open) "▲" else "▼")
                            }
                        }
                    }
                    if (expandedCountry == group.code) {
                        itemsIndexed(group.items.take(40), key = { _, t -> "c-" + group.code + "-" + t.link }) { index, t ->
                            SwipeRow(t) { ExplorerTargetRow(index + 1, t, nameOf(t)) { detailTarget = t } }
                            HorizontalDivider()
                        }
                    }
                }
            } else if (tab == "found") {
                if (foundShown.isEmpty()) {
                    item {
                        Text(
                            if (state.isRunning) "در حال دریافت و بررسی منابع؛ هر کانفیگی که پیدا شود همین‌جا بلافاصله نمایش داده می‌شود." else "کانفیگی کشف نشده؛ «شروع کاوش» را بزنید یا تب منابع را بررسی کنید.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 18.dp),
                        )
                    }
                } else {
                    item {
                        Text(
                            "سالم: " + foundAlive + " · آزمایش‌شده: " + foundDone + " از " + discovered.size,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    itemsIndexed(foundShown, key = { _, d -> d.id }) { index, d ->
                        SwipeRow(d.toTarget()) {
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { detailTarget = d.toTarget() }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text((index + 1).toString().padStart(2, '0'), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Column(Modifier.weight(1f)) {
                                Text((d.countryCode?.let { countryFlag(it) + "  " } ?: "") + nameOf(d.toTarget()), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    d.protocol + " · " + d.address + ":" + d.port + " · " + d.sourceName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            when (d.status) {
                                1 -> Text("✓ " + d.ms + " ms", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                2 -> Text("✗ ناموفق", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                                else -> Text("در حال تست…", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        }
                        HorizontalDivider()
                    }
                }
            } else if (shown.isEmpty()) {
            item {
                Text(
                    if (state.isRunning) "هنوز نتیجه سالمی تأیید نشده؛ با پایان اولین آزمایش‌ها، نتایج همین‌جا ظاهر می‌شوند." else "هنوز کانفیگ سالم تأییدشده‌ای در این فهرست وجود ندارد.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 18.dp),
                )
            }
            } else {
                itemsIndexed(shown, key = { _, e -> e.id }) { index, entry ->
                    SwipeRow(entry.toTarget()) {
                        ExplorerEntryRow(
                            index = index + 1,
                            entry = entry,
                            name = nameOf(entry.toTarget()),
                            isReserve = tab == "reserve",
                            onClick = { detailTarget = entry.toTarget() },
                            onPromote = { ConfigExplorerEngine.promoteReserve(repository, entry.id) },
                        )
                    }
                    HorizontalDivider()
                }
            }
            item {
            Text(
                if (tab == "live") "نتایج به‌محض تأیید مجدد وارد این فهرست می‌شوند؛ اطلاعات IP و کشور در پس‌زمینه تکمیل می‌شود." else "سلامت یعنی حداقل دو موفقیت از سه آزمایش HTTPS از مسیر هسته ZX؛ تست TCP به‌تنهایی کافی نیست.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            }
        }
        val showTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 4 } }
        if (showTop) {
            SmallFloatingActionButton(
                onClick = { scope.launch { listState.animateScrollToItem(0) } },
                modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp),
            ) { Text("↑") }
        }
      }
    }

    detailTarget?.let { target ->
        ExplorerDetailSheet(
            target = target,
            displayName = nameOf(target),
            isFavorite = state.favorites.any { it.link == target.link },
            onDismiss = { detailTarget = null },
            onConnect = { detailTarget = null; connectTarget(target) },
            onToggleFavorite = {
                val entry = (state.healthyPool + state.liveEntries + state.favorites).firstOrNull { it.link == target.link }
                    ?: ConfigExplorerEntry(
                        id = target.link, name = target.name, protocol = target.protocol, address = target.address, port = target.port,
                        sourceId = "", sourceName = target.sourceName, link = target.link, samplesMs = target.samplesMs,
                        medianMs = target.ms, meanMs = target.ms, jitterMs = target.jitterMs ?: 0,
                        lastCheckedAt = currentTimeMillis(), countryCode = target.countryCode,
                    )
                repository.configExplorer.toggleFavorite(entry)
            },
            onRename = { renameTarget = target },
            onExport = { exportTarget(target) },
        )
    }
    renameTarget?.let { target ->
        ExplorerRenameDialog(
            initial = nameOf(target),
            onSave = { name ->
                repository.configExplorer.rename(target.link, name)
                target.profileId?.let { repository.rename(it, name) }
                platform.toast("نام ذخیره شد")
                renameTarget = null
            },
            onReset = {
                repository.configExplorer.rename(target.link, "")
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
    }
}

internal fun countryFlag(code: String): String {
    if (code.length != 2) return "🌐"
    val base = 0x1F1E6
    return code.uppercase().map { char ->
        val point = base + char.code - 'A'.code
        val high = ((point - 0x10000) shr 10) + 0xD800
        val low = ((point - 0x10000) and 0x3FF) + 0xDC00
        "" + high.toChar() + low.toChar()
    }.joinToString("")
}

@Composable
private fun ExplorerMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(horizontal = 9.dp, vertical = 11.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ExplorerTab(label: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            label,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun ExplorerEntryRow(
    index: Int,
    entry: ConfigExplorerEntry,
    name: String,
    isReserve: Boolean,
    onClick: () -> Unit,
    onPromote: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(
            Modifier.fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(index.toString().padStart(2, '0'), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f)) {
                Text((entry.countryCode?.let(::countryFlag) ?: "🌐") + "  " + name, fontWeight = FontWeight.SemiBold)
                Text(
                    entry.protocol + " · " + entry.address + ":" + entry.port + " · " + entry.sourceName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(entry.medianMs.toString() + " ms", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("★ " + explorerQuality(entry), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                Text("نوسان " + entry.jitterMs + " ms", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (isReserve) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onPromote) { Text("جایگزین در ۱۰۰ برتر") }
            }
        }
    }
}

private fun explorerPhaseLabel(phase: String): String = when (phase) {
    "collecting" -> "در حال دریافت فیدهای کانفیگ"
    "parsing" -> "در حال بررسی ساختار و سازگاری کانفیگ‌ها"
    "testing_first" -> "تست واقعی تأخیر · مرحله اول"
    "testing_second" -> "تست واقعی تأخیر · مرحله دوم"
    "rechecking" -> "تأیید مجدد پایداری کانفیگ‌های برتر"
    "publishing" -> "در حال همگام‌سازی ۱۰۰ سرور برتر"
    "error" -> "چرخه با خطا متوقف شد"
    else -> "در حال اجرا"
}

/** 0..100 score from median latency and jitter: lower is better, stable beats fast-but-jumpy. */
internal fun explorerQuality(entry: ConfigExplorerEntry): Int =
    (100 - entry.medianMs / 12 - entry.jitterMs / 8).coerceIn(1, 100)

internal fun relativeAgo(timestamp: Long): String {
    val minutes = ((currentTimeMillis() - timestamp) / 60_000L).coerceAtLeast(0L)
    return when {
        minutes < 1 -> "هم‌اکنون"
        minutes < 60 -> minutes.toString() + " دقیقه پیش"
        minutes < 1_440 -> (minutes / 60).toString() + " ساعت پیش"
        else -> (minutes / 1_440).toString() + " روز پیش"
    }
}

private fun relativeIn(timestamp: Long): String {
    val minutes = ((timestamp - currentTimeMillis()) / 60_000L)
    return if (minutes <= 0L) "به‌زودی" else "تا " + minutes.toString() + " دقیقه دیگر"
}
