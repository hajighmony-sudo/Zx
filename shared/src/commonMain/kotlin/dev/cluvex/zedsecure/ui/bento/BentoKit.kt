@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package dev.cluvex.zedsecure.ui.bento

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material3.toPath
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import dev.cluvex.zedsecure.domain.model.AppDesign
import dev.cluvex.zedsecure.domain.model.BentoCorners
import dev.cluvex.zedsecure.domain.model.BentoDensity
import dev.cluvex.zedsecure.domain.model.BentoTitle
import dev.cluvex.zedsecure.shared.resources.Res
import dev.cluvex.zedsecure.ui.theme.LocalAppDesign
import dev.cluvex.zedsecure.shared.resources.action_clear
import dev.cluvex.zedsecure.shared.resources.action_back
import dev.cluvex.zedsecure.shared.resources.ic_arrow_back
import dev.cluvex.zedsecure.shared.resources.ic_close
import dev.cluvex.zedsecure.shared.resources.ic_search
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

object BentoShapes {
    @Composable
    fun tile(prominent: Boolean): Shape = when (Bento.prefs.corners) {
        BentoCorners.Leaf -> if (prominent) RoundedCornerShape(36.dp, 36.dp, 36.dp, 12.dp) else RoundedCornerShape(30.dp)
        BentoCorners.Round -> RoundedCornerShape(if (prominent) 36.dp else 30.dp)
        BentoCorners.Soft -> RoundedCornerShape(if (prominent) 24.dp else 20.dp)
    }

    @Composable
    fun hero(): Shape = when (Bento.prefs.corners) {
        BentoCorners.Leaf -> RoundedCornerShape(34.dp, 34.dp, 34.dp, 12.dp)
        BentoCorners.Round -> RoundedCornerShape(34.dp)
        BentoCorners.Soft -> RoundedCornerShape(22.dp)
    }

    @Composable
    fun card(): Shape = if (Bento.prefs.corners == BentoCorners.Soft) RoundedCornerShape(20.dp) else RoundedCornerShape(30.dp)

    @Composable
    fun block(): Shape = if (Bento.prefs.corners == BentoCorners.Soft) RoundedCornerShape(18.dp) else RoundedCornerShape(26.dp)

    @Composable
    fun small(): Shape = if (Bento.prefs.corners == BentoCorners.Soft) RoundedCornerShape(16.dp) else RoundedCornerShape(22.dp)

    @Composable
    fun chip(): Shape = if (Bento.prefs.corners == BentoCorners.Soft) RoundedCornerShape(12.dp) else RoundedCornerShape(14.dp)

    @Composable
    fun row(index: Int, count: Int): Shape {
        val soft = Bento.prefs.corners == BentoCorners.Soft
        val outer = if (soft) 16.dp else 22.dp
        val inner = if (soft) 6.dp else 8.dp
        return when {
            count <= 1 -> RoundedCornerShape(outer)
            index == 0 -> RoundedCornerShape(outer, outer, inner, inner)
            index == count - 1 -> RoundedCornerShape(inner, inner, outer, outer)
            else -> RoundedCornerShape(inner)
        }
    }
}

object BentoMetrics {
    val gutter: Dp = 16.dp

    @Composable
    fun gap(): Dp = if (Bento.prefs.density == BentoDensity.Compact) 6.dp else 8.dp

    @Composable
    fun tileRow(): Dp = if (Bento.prefs.density == BentoDensity.Compact) 116.dp else 134.dp

    @Composable
    fun rowMin(): Dp = if (Bento.prefs.density == BentoDensity.Compact) 54.dp else 62.dp

    @Composable
    fun pad(): Dp = if (Bento.prefs.density == BentoDensity.Compact) 13.dp else 16.dp
}

object BentoType {
    @Composable
    fun display(): TextStyle {
        val (size, line) = when (Bento.prefs.title) {
            BentoTitle.Huge -> 50 to 56
            BentoTitle.Large -> 42 to 48
            BentoTitle.Compact -> 34 to 40
        }
        return MaterialTheme.typography.displayMedium.copy(
            fontSize = size.sp,
            lineHeight = line.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = if (Bento.latin) (-size * 0.033f).sp else 0.sp,
        )
    }

    @Composable
    fun title(size: TextUnit, tight: Boolean = true): TextStyle = MaterialTheme.typography.titleLarge.copy(
        fontSize = size,
        lineHeight = size * 1.18f,
        fontWeight = FontWeight.Bold,
        letterSpacing = if (Bento.latin && tight) (-size.value * 0.022f).sp else 0.sp,
    )

    @Composable
    fun caption(): TextStyle = MaterialTheme.typography.labelMedium.copy(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = if (Bento.latin) 0.9.sp else 0.sp,
    )

    @Composable
    fun number(size: TextUnit, weight: FontWeight = FontWeight.SemiBold): TextStyle = MaterialTheme.typography.titleLarge.copy(
        fontSize = size,
        lineHeight = size * 1.08f,
        fontWeight = weight,
        fontFeatureSettings = "tnum",
        letterSpacing = if (Bento.latin) (-size.value * 0.025f).sp else 0.sp,
    )

    @Composable
    fun body(): TextStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp)

    @Composable
    fun rowTitle(): TextStyle = MaterialTheme.typography.titleMedium.copy(fontSize = 15.5.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
}

fun Modifier.bentoMotif(
    shape: RoundedPolygon,
    color: Color,
    size: Dp,
    rotation: Float,
    enabled: Boolean = true,
    alpha: Float = 0.12f,
    bleedX: Dp = 30.dp,
    bleedY: Dp = 34.dp,
): Modifier = if (!enabled) this else drawWithCache {
    val px = size.toPx()
    val path = androidx.graphics.shapes.Morph(shape, shape).toPath(0f, androidx.compose.ui.graphics.Path()).apply {
        transform(Matrix().apply { scale(px, px) })
    }
    val bounds = path.getBounds()
    val bx = bleedX.toPx()
    val by = bleedY.toPx()
    val tint = color.copy(alpha = color.alpha * alpha)
    onDrawBehind {
        val rtl = layoutDirection == LayoutDirection.Rtl
        val center = Offset(
            if (rtl) px / 2f - bx else this.size.width - px / 2f + bx,
            this.size.height - px / 2f + by,
        )
        translate(center.x - bounds.center.x, center.y - bounds.center.y) {
            rotate(if (rtl) -rotation else rotation, pivot = bounds.center) {
                drawPath(path, tint)
            }
        }
    }
}

@Composable
fun BentoCard(
    tone: BentoTone,
    modifier: Modifier = Modifier,
    shape: Shape = BentoShapes.card(),
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
    motif: RoundedPolygon? = null,
    motifSize: Dp = 160.dp,
    motifRotation: Float = 16f,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val playful = Bento.prefs.playfulTouch
    val scale by animateFloatAsState(
        if (pressed && onClick != null) (if (playful) 0.965f else 0.985f) else 1f,
        spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMedium),
        label = "bento-card-press",
    )
    val zxGlass = LocalAppDesign.current == AppDesign.Zx
    val surfaceShape = if (zxGlass) RoundedCornerShape(8.dp, 22.dp, 8.dp, 22.dp) else shape
    val glassBorder = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.88f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
            MaterialTheme.colorScheme.outline.copy(alpha = 0.58f),
        ),
    )
    Box(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(if (zxGlass) 10.dp else 0.dp, surfaceShape, clip = false, ambientColor = Color.Black.copy(alpha = 0.28f), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
            .clip(surfaceShape)
            .background(
                if (zxGlass) {
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.98f),
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.98f),
                        ),
                    )
                } else {
                    androidx.compose.ui.graphics.SolidColor(tone.container)
                },
            )
            .then(
                if (zxGlass) {
                    Modifier.background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.16f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.04f),
                            ),
                        ),
                        surfaceShape,
                    )
                } else Modifier,
            )
            .then(if (zxGlass) Modifier.border(1.dp, glassBorder, surfaceShape) else Modifier)
            .bentoMotif(motif ?: NoMotif, tone.content, motifSize, motifRotation, enabled = !zxGlass && motif != null && Bento.prefs.motifs)
            .then(
                if (onClick != null) {
                    Modifier.combinedClickable(
                        interactionSource = source,
                        indication = ripple(color = if (zxGlass) MaterialTheme.colorScheme.primary else tone.content),
                        role = Role.Button,
                        onClickLabel = onClickLabel,
                        onLongClick = onLongClick,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        CompositionLocalProvider(LocalContentColor provides if (zxGlass) MaterialTheme.colorScheme.onSurface else tone.content) {
            Column(Modifier.fillMaxWidth().padding(contentPadding), verticalArrangement = verticalArrangement, content = content)
        }
    }
}

private val NoMotif: RoundedPolygon by lazy { androidx.compose.material3.MaterialShapes.Circle }

@Composable
fun MotifButton(
    shape: RoundedPolygon,
    container: Color,
    content: Color,
    icon: DrawableResource,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val playful = Bento.prefs.playfulTouch
    val spin by animateFloatAsState(
        if (pressed && playful) 34f else 0f,
        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label = "motif-spin",
    )
    val squash by animateFloatAsState(
        if (pressed) 0.9f else 1f,
        spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "motif-squash",
    )
    val clip = shape.toShape()
    Box(
        modifier
            .size(size)
            .semantics { contentDescription = label }
            .combinedClickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onLongClick = onLongClick,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    rotationZ = spin
                    scaleX = squash
                    scaleY = squash
                    alpha = if (enabled) 1f else 0.45f
                }
                .clip(clip)
                .background(container),
        )
        Icon(painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(size * 0.46f))
    }
}

@Composable
fun BentoIconButton(
    icon: DrawableResource,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    container: Color = Bento.colors.surface,
    content: Color = Bento.colors.ink,
    size: Dp = 46.dp,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val zx = LocalAppDesign.current == AppDesign.Zx
    val shape = if (zx) RoundedCornerShape(8.dp, 15.dp, 8.dp, 15.dp) else CircleShape
    val fill = if (zx) MaterialTheme.colorScheme.surfaceContainerHigh else container
    val ink = if (zx) MaterialTheme.colorScheme.onSurface else content
    Box(
        modifier
            .size(size)
            .graphicsLayer {
                scaleX = if (zx) 1f else if (pressed) 0.9f else 1f
                scaleY = if (zx) 1f else if (pressed) 0.9f else 1f
                alpha = if (enabled) 1f else 0.45f
            }
            .clip(shape)
            .background(fill)
            .then(if (zx) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape) else Modifier)
            .semantics { contentDescription = label }
            .clickable(source, ripple(color = ink), enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = ink, modifier = Modifier.size(size * 0.46f))
    }
}

@Composable
fun BentoBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    BentoIconButton(Res.drawable.ic_arrow_back, stringResource(Res.string.action_back), onClick, modifier)
}

@Composable
fun BentoTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onTitleClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val zx = LocalAppDesign.current == AppDesign.Zx
    Row(
        modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            title,
            style = if (zx) MaterialTheme.typography.headlineSmall else BentoType.display(),
            color = if (zx) MaterialTheme.colorScheme.onSurface else Bento.colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .then(
                    if (onTitleClick != null) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onTitleClick,
                        )
                    } else {
                        Modifier
                    },
                ),
        )
        actions()
    }
}

@Composable
fun BentoCaption(text: String, modifier: Modifier = Modifier, color: Color? = null) {
    val zx = LocalAppDesign.current == AppDesign.Zx
    Text(
        text.uppercase(),
        style = BentoType.caption(),
        color = color ?: if (zx) MaterialTheme.colorScheme.onSurfaceVariant else Bento.colors.muted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 2.dp),
    )
}

@Composable
fun BentoSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val c = Bento.colors
    val zxGlass = LocalAppDesign.current == AppDesign.Zx
    val shape = if (zxGlass) RoundedCornerShape(8.dp, 18.dp, 8.dp, 18.dp) else RoundedCornerShape(26.dp)
    val style = MaterialTheme.typography.bodyLarge.copy(color = if (zxGlass) MaterialTheme.colorScheme.onSurface else c.ink)
    val fill = if (zxGlass) {
        Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.98f), MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.98f)))
    } else androidx.compose.ui.graphics.SolidColor(c.surface)
    val rim = Brush.linearGradient(
        listOf(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f), MaterialTheme.colorScheme.outline),
    )
    Row(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(if (zxGlass) 10.dp else 0.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.24f))
            .clip(shape)
            .background(fill, shape)
            .then(if (zxGlass) Modifier.border(1.dp, rim, shape) else Modifier)
            .padding(start = 18.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(Res.drawable.ic_search), contentDescription = null, tint = if (zxGlass) MaterialTheme.colorScheme.onSurfaceVariant else c.muted, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text(placeholder, style = style, color = if (zxGlass) MaterialTheme.colorScheme.onSurfaceVariant else c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = style,
                cursorBrush = SolidColor(if (zxGlass) MaterialTheme.colorScheme.primary else c.lime),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            BentoIconButton(
                icon = Res.drawable.ic_close,
                label = stringResource(Res.string.action_clear),
                onClick = { onValueChange("") },
                size = 40.dp,
                container = Color.Transparent,
                content = c.muted,
            )
        }
    }
}

@Composable
fun BentoChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    trailing: String? = null,
) {
    val c = Bento.colors
    val zxGlass = LocalAppDesign.current == AppDesign.Zx
    val shape = if (zxGlass) RoundedCornerShape(8.dp, 14.dp, 8.dp, 14.dp) else BentoShapes.chip()
    val bg = if (selected) c.lime else Color.Transparent
    val fg = if (zxGlass) if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface else if (selected) c.onLime else c.ink
    val chipFill = when {
        zxGlass && selected -> androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
        zxGlass -> androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.surfaceContainerHigh)
        else -> androidx.compose.ui.graphics.SolidColor(bg)
    }
    val chipRim = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.outlineVariant,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
            MaterialTheme.colorScheme.outline,
        ),
    )
    val source = remember { MutableInteractionSource() }
    Row(
        modifier
            .heightIn(min = if (zxGlass) 48.dp else 40.dp)
             .shadow(if (zxGlass) 1.dp else 0.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.10f))
            .clip(shape)
            .background(chipFill, shape)
            .then(if (selected && !zxGlass) Modifier else Modifier.border(1.dp, if (zxGlass) chipRim else androidx.compose.ui.graphics.SolidColor(c.line), shape))
            .combinedClickable(
                interactionSource = source,
                indication = ripple(color = if (zxGlass) MaterialTheme.colorScheme.primary else fg),
                role = Role.Button,
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold),
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (trailing != null) {
            Text(
                trailing,
                style = MaterialTheme.typography.labelMedium,
                color = if (zxGlass) MaterialTheme.colorScheme.onSurfaceVariant else c.muted,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun BentoPill(text: String, container: Color, content: Color, modifier: Modifier = Modifier, dot: Color? = null, numeric: Boolean = false) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (dot != null) Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
        Text(
            text,
            style = if (numeric) BentoType.number(12.sp) else MaterialTheme.typography.labelMedium,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun BentoFlag(
    countryCode: String?,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    corner: Dp = 13.dp,
    fallback: String? = null,
    container: Color = Bento.colors.surface2,
    content: Color = Bento.colors.ink,
) {
    val code = countryCode?.trim()?.uppercase()?.takeIf { it.length == 2 && it.all { ch -> ch in 'A'..'Z' } }
    var bytes by remember(code) { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(code) {
        bytes = code?.let { runCatching { Res.readBytes("files/flags/${it.lowercase()}.svg") }.getOrNull() }
    }
    val shape = RoundedCornerShape(corner)
    val context = LocalPlatformContext.current
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(container)
            .border(1.dp, content.copy(alpha = 0.12f), shape),
        contentAlignment = Alignment.Center,
    ) {
        val data = bytes
        if (data != null) {
            AsyncImage(
                model = remember(data) { ImageRequest.Builder(context).data(data).build() },
                contentDescription = code,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Text(
                (fallback ?: code ?: "··").take(3),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = (size.value * 0.32f).sp, fontWeight = FontWeight.Bold),
                color = content,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun BentoIndex(index: Int, color: Color = LocalContentColor.current, modifier: Modifier = Modifier) {
    Text(
        "%02d".format(index),
        style = BentoType.number(12.sp, FontWeight.Medium),
        color = color.copy(alpha = color.alpha * 0.7f),
        modifier = modifier,
    )
}

@Composable
fun BentoBadge(
    shape: RoundedPolygon,
    container: Color,
    content: Color,
    icon: DrawableResource,
    size: Dp,
    modifier: Modifier = Modifier,
    rotation: Float = 10f,
) {
    val clip = shape.toShape()
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { rotationZ = rotation }
                .clip(clip)
                .background(container),
        )
        Icon(painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(size * 0.5f))
    }
}
