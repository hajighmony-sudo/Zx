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
    val bottomBarRadius = 30.dp
    val cardRadius = 28.dp
    val chipRadius = 16.dp
    val heroRadius = 32.dp

    /** Soft clay elevation for primary surfaces. */
    val clayElevation = 18.dp
    val clayElevationSoft = 10.dp
    val clayElevationChip = 6.dp

    @Composable
    @ReadOnlyComposable
    fun isActive(): Boolean = LocalAppDesign.current == AppDesign.Zx

    /** Holographic rim: white → primary → tertiary → outline. */
    @Composable
    fun holographicRim(strong: Boolean = false): Brush {
        val cs = MaterialTheme.colorScheme
        val a = if (strong) 1f else 0.72f
        return Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.55f * a),
                cs.primary.copy(alpha = 0.48f * a),
                cs.tertiary.copy(alpha = 0.36f * a),
                cs.secondary.copy(alpha = 0.22f * a),
                cs.outlineVariant.copy(alpha = 0.32f * a),
            ),
        )
    }

    /** Soft liquid-glass fill over a base surface. */
    @Composable
    fun liquidFill(base: Color, elevated: Color = MaterialTheme.colorScheme.surfaceContainer): Brush =
        Brush.linearGradient(
            listOf(
                base.copy(alpha = 0.82f),
                elevated.copy(alpha = 0.68f),
                base.copy(alpha = 0.72f),
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
