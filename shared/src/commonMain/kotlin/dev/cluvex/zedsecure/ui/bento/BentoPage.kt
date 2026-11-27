package dev.cluvex.zedsecure.ui.bento

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.cluvex.zedsecure.domain.model.AppDesign
import dev.cluvex.zedsecure.ui.theme.LocalAppDesign
import dev.cluvex.zedsecure.ui.components.LocalPageBack
import dev.cluvex.zedsecure.ui.settings.SettingsArt
import dev.cluvex.zedsecure.ui.settings.sharedArt

@Composable
fun BentoPageHeader(
    title: String,
    subtitle: String?,
    art: SettingsArt?,
    modifier: Modifier = Modifier,
    scroll: ScrollState? = null,
) {
    val c = Bento.colors
    val back = LocalPageBack.current
    Column(
        modifier.fillMaxWidth().padding(horizontal = BentoMetrics.gutter),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (back != null) BentoBackButton(back)
        if (art == null) {
            Text(title, style = BentoType.display(), color = c.ink, modifier = Modifier.padding(horizontal = 4.dp))
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = BentoType.body(), color = c.muted, modifier = Modifier.padding(horizontal = 4.dp))
            }
            return@Column
        }
        val prominent = BentoTiles.isProminent(art.key)
        val tone = c.tone(BentoTiles.toneOf(art.key))
        val accent = if (prominent) c.lime else tone.content
        val accentInk = if (prominent) c.onLime else tone.container
        val spin = (scroll?.value ?: 0) * 0.12f
        val zxGlass = LocalAppDesign.current == AppDesign.Zx
        val heroShape = if (zxGlass) RoundedCornerShape(8.dp, 22.dp, 8.dp, 22.dp) else BentoShapes.hero()
        val heroFill = if (zxGlass) {
            Brush.linearGradient(
                listOf(
                    androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.99f),
                    androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.98f),
                ),
            )
        } else SolidColor(tone.container)
        val heroRim = Brush.linearGradient(
            listOf(
                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.58f),
                tone.content.copy(alpha = 0.40f),
                androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.32f),
                androidx.compose.material3.MaterialTheme.colorScheme.tertiary.copy(alpha = 0.28f),
            ),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 148.dp)
                .then(
                    if (zxGlass) Modifier.shadow(
                        20.dp,
                        heroShape,
                        clip = false,
                        ambientColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.26f),
                        spotColor = tone.content.copy(alpha = 0.18f),
                    ) else Modifier,
                )
                .clip(heroShape)
                .background(heroFill, heroShape)
                .then(
                    if (zxGlass) {
                        Modifier
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        androidx.compose.ui.graphics.Color.White.copy(alpha = 0.16f),
                                        androidx.compose.ui.graphics.Color.Transparent,
                                        androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.03f),
                                    ),
                                ),
                                heroShape,
                            )
                            .border(1.15.dp, heroRim, heroShape)
                    } else Modifier,
                )
                .bentoMotif(art.shape, tone.content, 170.dp, 18f + spin, enabled = !zxGlass && Bento.prefs.motifs, alpha = 0.1f, bleedX = 36.dp, bleedY = 44.dp),
        ) {
            CompositionLocalProvider(LocalContentColor provides if (zxGlass) androidx.compose.material3.MaterialTheme.colorScheme.onSurface else tone.content) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        if (!zxGlass && Bento.prefs.indexes) BentoIndex(BentoTiles.ordinalOf(art.key), tone.content)
                        Spacer(Modifier.weight(1f))
                        BentoBadge(
                            shape = art.shape,
                            container = accent,
                            content = accentInk,
                            icon = art.icon,
                            size = 48.dp,
                            rotation = 12f + spin,
                            modifier = Modifier.sharedArt(art),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            title,
                            style = BentoType.title(if (title.length > 18) 28.sp else 36.sp),
                            color = if (zxGlass) androidx.compose.material3.MaterialTheme.colorScheme.onSurface else tone.content,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                subtitle,
                                style = BentoType.body(),
                                color = if (zxGlass) androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant else tone.content.copy(alpha = 0.82f),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BentoGroupLabel(title: String, modifier: Modifier = Modifier) {
    BentoCaption(title, modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 4.dp))
}
