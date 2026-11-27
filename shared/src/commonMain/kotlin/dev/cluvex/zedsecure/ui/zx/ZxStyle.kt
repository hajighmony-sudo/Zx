package dev.cluvex.zedsecure.ui.zx

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.cluvex.zedsecure.domain.model.AppDesign
import dev.cluvex.zedsecure.ui.theme.LocalAppDesign

/**
 * Shared ZX visual language helpers.
 *
 * Combines Liquid Glass, Claymorphism depth, Holographic rims and
 * Apple-HIG calm hierarchy. Only active when [AppDesign.Zx] is selected
 * so other themes remain untouched.
 */
object ZxStyle {
    val topBarHeight = 56.dp
    val bottomBarRadius = 20.dp
    val cardRadius = 18.dp
    val chipRadius = 10.dp
    val heroRadius = 22.dp

    /** Restrained elevation: ZX uses precise layered surfaces, not inflated clay tiles. */
    val clayElevation = 9.dp
    val clayElevationSoft = 5.dp
    val clayElevationChip = 2.dp

    @Composable
    @ReadOnlyComposable
    fun isActive(): Boolean = LocalAppDesign.current == AppDesign.Zx

    /** Restrained technical rim: neutral metal with a small active-accent trace. */
    @Composable
    fun holographicRim(strong: Boolean = false): Brush {
        val cs = MaterialTheme.colorScheme
        val a = if (strong) 1f else 0.62f
        return Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.28f * a),
                cs.outlineVariant.copy(alpha = 0.82f * a),
                cs.primary.copy(alpha = 0.28f * a),
                cs.outline.copy(alpha = 0.52f * a),
            ),
        )
    }

    /** Soft liquid-glass fill over a base surface. */
    @Composable
    fun liquidFill(base: Color, elevated: Color = MaterialTheme.colorScheme.surfaceContainer): Brush =
        Brush.linearGradient(
            listOf(
                elevated.copy(alpha = 0.96f),
                base.copy(alpha = 0.92f),
            ),
        )

    /** Top sheen for frosted glass. */
    fun glassSheen(): Brush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.16f),
            Color.Transparent,
            Color.Black.copy(alpha = 0.04f),
        ),
    )
}

/** Apply ZX clay shadow + holographic border when ZX design is active. */
@Composable
fun Modifier.zxGlassSurface(
    shape: Shape = RoundedCornerShape(ZxStyle.cardRadius),
    elevation: Dp = ZxStyle.clayElevation,
    strongRim: Boolean = false,
): Modifier {
    if (!ZxStyle.isActive()) return this
    return this
        .shadow(
            elevation,
            shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.26f),
            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
        )
        .border(1.1.dp, ZxStyle.holographicRim(strongRim), shape)
}
