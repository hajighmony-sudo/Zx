@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package dev.cluvex.zedsecure.ui.vault

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.cluvex.zedsecure.shared.resources.Res
import dev.cluvex.zedsecure.shared.resources.*
import dev.cluvex.zedsecure.core.VaultImportBus
import dev.cluvex.zedsecure.ui.platform.LocalPlatform
import dev.cluvex.zedsecure.crypto.ZsxSealRequest
import dev.cluvex.zedsecure.data.config.ConfigRepository
import dev.cluvex.zedsecure.domain.config.VpnProfile
import dev.cluvex.zedsecure.ui.components.MorphingBlob
import dev.cluvex.zedsecure.ui.components.PageHeader
import dev.cluvex.zedsecure.ui.theme.ZedGradients
import dev.cluvex.zedsecure.ui.platform.appVerticalScroll
import dev.cluvex.zedsecure.ui.platform.mouseDragScrollVertical
import androidx.compose.foundation.lazy.rememberLazyListState
import dev.cluvex.zedsecure.ui.platform.rememberAppSheetState
import dev.cluvex.zedsecure.ui.theme.LocalAppDesign
import dev.cluvex.zedsecure.ui.bento.Bento
import dev.cluvex.zedsecure.ui.bento.BentoColors

private const val NOTE_MAX_LEN = 500

// ZX currently shares the Bento palette in ZedSecureTheme.
private val BentoColors.neonA: Color get() = lime
private val BentoColors.neonB: Color get() = cyan.content
private val BentoColors.neonC: Color get() = pink.content

@Composable
fun VaultScreen(
    repository: ConfigRepository,
    contentPadding: PaddingValues,
    onImportRequested: () -> Unit,
    modifier: Modifier = Modifier,
    onActiveChanged: () -> Unit = {},
) {
    val platform = LocalPlatform.current
    val needConfigMsg = stringResource(Res.string.vault_need_config_first)
    val profiles by repository.profiles.collectAsStateWithLifecycle()
    val activeId by repository.activeId.collectAsStateWithLifecycle()
    val locked = profiles.filter { it.isLocked }

    val sources = remember(profiles) {
        profiles.filter { p ->
            !p.isLocked && !p.isProxyChain && !p.isCrossChain && !p.isAutoSelect && p.rawPayload() != null
        }
    }
    var showCreate by remember { mutableStateOf(false) }
    var sealing by remember { mutableStateOf(false) }
    val sealScope = rememberCoroutineScope()

    Column(modifier.fillMaxSize().padding(contentPadding)) {
        Spacer(Modifier.height(8.dp))
        if (LocalAppDesign.current.name == "Zx") {
            ZxVaultHero(lockedCount = locked.size, sourceCount = sources.size)
            ZxVaultNotice()
        } else {
            PageHeader(
                title = stringResource(Res.string.vault_title),
                subtitle = stringResource(Res.string.vault_intro_body),
            )
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painterResource(Res.drawable.ic_info),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            stringResource(Res.string.vault_experimental_badge),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        Text(
                            stringResource(Res.string.vault_experimental_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
                        )
                    }
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (locked.isEmpty()) {
                VaultEmptyState(Modifier.fillMaxSize())
            } else {
                val listState = rememberLazyListState()
                LazyColumn(
                    Modifier.fillMaxSize().mouseDragScrollVertical(listState).padding(horizontal = 20.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(locked, key = { it.id }) { profile ->
                        val selectedMsg = stringResource(Res.string.vault_selected, profile.name)
                        val active = profile.id == activeId
                        if (LocalAppDesign.current.name == "Zx") {
                            ZxLockedCard(
                                profile = profile,
                                active = active,
                                onUse = {
                                    val changed = profile.id != activeId
                                    repository.setActive(profile.id)
                                    if (changed) onActiveChanged()
                                    platform.toast(selectedMsg)
                                },
                                onDelete = { repository.remove(profile.id) },
                            )
                        } else {
                            LockedCard(
                                profile = profile,
                                active = active,
                                onUse = {
                                    val changed = profile.id != activeId
                                    repository.setActive(profile.id)
                                    if (changed) onActiveChanged()
                                    platform.toast(selectedMsg)
                                },
                                onDelete = { repository.remove(profile.id) },
                            )
                        }
                    }
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = {
                    if (sources.isEmpty()) {
                        platform.toast(needConfigMsg)
                    } else {
                        showCreate = true
                    }
                },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                Icon(painterResource(Res.drawable.ic_lock), null, Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(Res.string.vault_create), fontWeight = FontWeight.SemiBold)
            }
            FilledTonalButton(
                onClick = onImportRequested,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                Icon(painterResource(Res.drawable.ic_ios_share), null, Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(Res.string.vault_import), fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showCreate) {
        CreateLockedSheet(
            sources = sources,
            sealing = sealing,
            onDismiss = { showCreate = false },
            onCreate = { chosen, form, combine, pack ->
                sealing = true
                sealScope.launch {
                    val result = runCatching {
                        withContext(Dispatchers.Default) {
                            repository.lockedRequests(chosen, form, combine, pack)
                                .map { (request, fileName) -> fileName to repository.createLockedZsx(request) }
                        }
                    }
                    sealing = false
                    result.onSuccess { files ->
                        if (files.isNotEmpty()) {
                            platform.shareFiles(files)
                            showCreate = false
                        } else {
                            platform.toast("هیچ فایل قابل اشتراکی ساخته نشد")
                        }
                    }.onFailure { error ->
                        platform.toast(error.message?.takeIf { it.isNotBlank() } ?: "ساخت فایل قفل‌شده ناموفق بود")
                    }
                }
            },
        )
    }
}

@Composable
private fun ZxVaultHero(lockedCount: Int, sourceCount: Int) {
    val c = Bento.colors
    val borderTone by animateColorAsState(
        if (lockedCount > 0) c.neonA else c.neonB,
        tween(420),
        label = "zx-vault-hero-border",
    )
    ZxVaultPanel(accent = borderTone) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "ZX / CONFIG VAULT",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = c.neonA,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    stringResource(Res.string.vault_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = c.ink,
                )
            }
            Box(
                Modifier
                    .size(58.dp)
                    .background(c.neonA.copy(alpha = 0.10f), CutCornerShape(8.dp))
                    .border(1.dp, c.neonA.copy(alpha = 0.55f), CutCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(Res.drawable.ic_encrypted),
                    contentDescription = null,
                    tint = c.neonA,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(Res.string.vault_intro_body),
            style = MaterialTheme.typography.bodySmall,
            color = c.muted,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZxVaultStat(
                label = stringResource(Res.string.vault_locked_configs),
                value = lockedCount.toString(),
                accent = c.neonA,
                modifier = Modifier.weight(1f),
            )
            ZxVaultStat(
                label = stringResource(Res.string.vault_form_source),
                value = sourceCount.toString(),
                accent = c.neonB,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ZxVaultPanel(
    accent: Color,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val c = Bento.colors
    val shape = CutCornerShape(topStart = 0.dp, topEnd = 24.dp, bottomEnd = 0.dp, bottomStart = 24.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(c.surface2.copy(alpha = 0.96f), c.surface.copy(alpha = 0.92f), c.ground.copy(alpha = 0.94f)),
                ),
                shape,
            )
            .border(1.dp, accent.copy(alpha = 0.78f), shape)
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun ZxVaultStat(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    val shape = CutCornerShape(topStart = 0.dp, topEnd = 12.dp, bottomEnd = 0.dp, bottomStart = 12.dp)
    Column(
        modifier
            .background(accent.copy(alpha = 0.08f), shape)
            .border(1.dp, accent.copy(alpha = 0.34f), shape)
            .padding(horizontal = 11.dp, vertical = 9.dp),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Bento.colors.muted, maxLines = 1)
        Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = accent)
    }
}

@Composable
private fun ZxVaultNotice() {
    val c = Bento.colors
    ZxVaultPanel(accent = c.neonC) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(Res.drawable.ic_info), null, tint = c.neonC, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(9.dp))
            Column {
                Text(
                    stringResource(Res.string.vault_experimental_badge).uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = c.neonC,
                )
                Text(
                    stringResource(Res.string.vault_experimental_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.muted,
                )
            }
        }
    }
}

@Composable
private fun ZxLockedCard(
    profile: VpnProfile,
    active: Boolean,
    onUse: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = Bento.colors
    val accent by animateColorAsState(if (active) c.neonA else c.line, tween(300), label = "zx-vault-card-accent")
    val shape = CutCornerShape(topStart = 0.dp, topEnd = 20.dp, bottomEnd = 0.dp, bottomStart = 20.dp)
    Surface(
        onClick = onUse,
        shape = shape,
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accent.copy(alpha = if (active) 0.9f else 0.62f), shape),
    ) {
        Row(
            Modifier
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(
                            if (active) c.neonA.copy(alpha = 0.14f) else c.surface2.copy(alpha = 0.88f),
                            c.surface.copy(alpha = 0.94f),
                        ),
                    ),
                )
                .padding(start = 14.dp, end = 4.dp, top = 13.dp, bottom = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(Res.drawable.ic_encrypted),
                null,
                tint = accent,
                modifier = Modifier.size(25.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    profile.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = c.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(if (active) Res.string.config_active else Res.string.locked_badge).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                )
                profile.note?.takeIf { it.isNotBlank() }?.let { note ->
                    Spacer(Modifier.height(4.dp))
                    Text(note, style = MaterialTheme.typography.bodySmall, color = c.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            if (active) {
                Icon(painterResource(Res.drawable.ic_check_circle), null, tint = c.good, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(painterResource(Res.drawable.ic_delete), stringResource(Res.string.action_delete), tint = c.muted)
            }
        }
    }
}

@Composable
private fun VaultEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        MorphingBlob(progress = 0.5f, brush = ZedGradients.connected, modifier = Modifier.size(132.dp)) {
            Icon(
                painterResource(Res.drawable.ic_encrypted),
                null,
                tint = Color.White,
                modifier = Modifier.size(48.dp),
            )
        }
        Spacer(Modifier.height(22.dp))
        Text(
            stringResource(Res.string.vault_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(Res.string.vault_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LockedCard(
    profile: VpnProfile,
    active: Boolean,
    onUse: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        onClick = onUse,
        shape = RoundedCornerShape(22.dp),

        color = if (active) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(Res.drawable.ic_encrypted),
                null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    profile.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(if (active) Res.string.config_active else Res.string.locked_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                profile.note?.takeIf { it.isNotBlank() }?.let { note ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (active) {
                Icon(
                    painterResource(Res.drawable.ic_check_circle),
                    contentDescription = stringResource(Res.string.config_active),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(4.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(
                    painterResource(Res.drawable.ic_delete),
                    contentDescription = stringResource(Res.string.action_delete),
                )
            }
        }
    }
}

@Composable
private fun CreateLockedSheet(
    sources: List<VpnProfile>,
    sealing: Boolean,
    onDismiss: () -> Unit,
    onCreate: (List<VpnProfile>, ZsxSealRequest, Boolean, Boolean) -> Unit,
) {
    var selected by remember { mutableStateOf(sources.take(1).map { it.id }.toSet()) }
    var combine by remember { mutableStateOf(false) }
    var pack by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var expiryDays by remember { mutableStateOf("0") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberAppSheetState()) {
        Column(
            Modifier
                .fillMaxWidth()
                .appVerticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(Res.string.vault_create_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(Res.string.vault_pick_servers),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            Row {
                TextButton(onClick = { selected = sources.mapTo(LinkedHashSet()) { it.id } }) {
                    Text(stringResource(Res.string.select_all))
                }
                TextButton(onClick = { selected = emptySet() }, enabled = selected.isNotEmpty()) {
                    Text(stringResource(Res.string.select_none))
                }
            }
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).mouseDragScrollVertical(listState),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              items(sources, key = { it.id }) { profile ->
                val checked = profile.id in selected
                Surface(
                    onClick = { selected = if (checked) selected - profile.id else selected + profile.id },
                    shape = RoundedCornerShape(16.dp),
                    color = if (checked) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = checked, onCheckedChange = null)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                profile.name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                profile.transportLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
              }
            }
            if (selected.size > 1) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = combine, onCheckedChange = {
                        combine = it
                        if (it) pack = false
                    })
                    Text(stringResource(Res.string.vault_combine_one), style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = pack, onCheckedChange = {
                        pack = it
                        if (it) combine = false
                    })
                    Column {
                        Text("یک فایل شامل همه کانفیگ‌ها", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "گیرنده با یک‌بار بازکردن، همه موارد را دریافت می‌کند",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text(stringResource(Res.string.vault_form_name)) },
                supportingText = {
                    if (selected.size > 1 && !combine) Text(stringResource(Res.string.vault_name_numbered))
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= NOTE_MAX_LEN) note = it },
                label = { Text(stringResource(Res.string.vault_form_note)) },
                supportingText = { Text("${note.length} / $NOTE_MAX_LEN") },
                minLines = 2,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(Res.string.vault_form_password_optional)) },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            painterResource(
                                if (passwordVisible) Res.drawable.ic_visibility_off
                                else Res.drawable.ic_key,
                            ),
                            contentDescription = stringResource(Res.string.vault_toggle_password),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = expiryDays,
                onValueChange = { expiryDays = it.filter(Char::isDigit).take(4) },
                label = { Text(stringResource(Res.string.vault_form_expiry_days)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = { showPreview = true },
                enabled = !sealing && selected.isNotEmpty(),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                if (sealing) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp)
                } else {
                    Text("پیش‌نمایش و ساخت فایل قفل‌شده", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showPreview) {
        val chosen = sources.filter { it.id in selected }
        val days = expiryDays.toIntOrNull() ?: 0
        val finalName = displayName.trim().ifBlank { chosen.firstOrNull()?.name.orEmpty() }
        AlertDialog(
            onDismissRequest = { showPreview = false },
            title = { Text("پیش‌نمایش بسته") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(finalName.ifBlank { "کانفیگ قفل‌شده" }, fontWeight = FontWeight.Bold)
                    Text("تعداد کانفیگ‌ها: ${chosen.size}")
                    chosen.forEach { profile ->
                        Text("• ${profile.name}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(if (pack && chosen.size > 1) "خروجی: یک فایل چندکانفیگی" else if (combine && chosen.size > 1) "خروجی: یک کانفیگ Auto Select" else "خروجی: فایل جداگانه برای هر کانفیگ")
                    Text(if (password.isBlank()) "محافظت با رمز عبور: غیرفعال" else "محافظت با رمز عبور: فعال")
                    Text(if (days <= 0) "انقضا: بدون محدودیت زمانی" else "انقضا: ${days} روز پس از ساخت")
                    if (note.isNotBlank()) Text("یادداشت: $note")
                    Text(
                        "پس از بازکردن فایل، گیرنده باید رمز را وارد کند (در صورت فعال‌بودن) و کانفیگ‌ها به فهرست او افزوده می‌شوند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !sealing && chosen.isNotEmpty(),
                    onClick = {
                        val form = ZsxSealRequest(
                            configPayload = "",
                            nameEn = displayName.trim(),
                            nameFa = displayName.trim(),
                            note = note,
                            expiresAt = if (days > 0) System.currentTimeMillis() + days * 86_400_000L else null,
                            password = password.ifBlank { null },
                        )
                        showPreview = false
                        onCreate(chosen, form, combine && chosen.size > 1, pack && chosen.size > 1)
                    },
                ) { Text("تأیید و اشتراک‌گذاری") }
            },
            dismissButton = {
                TextButton(onClick = { showPreview = false }) { Text("ویرایش") }
            },
        )
    }
}
