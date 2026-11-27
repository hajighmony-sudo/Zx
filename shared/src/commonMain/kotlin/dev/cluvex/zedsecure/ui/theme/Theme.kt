package dev.cluvex.zedsecure.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.ktx.animateColorScheme
import dev.cluvex.zedsecure.domain.model.AppDesign
import dev.cluvex.zedsecure.domain.model.ZxAnim
import dev.cluvex.zedsecure.domain.model.BentoPrefs
import dev.cluvex.zedsecure.ui.bento.BentoPalettes
import dev.cluvex.zedsecure.ui.bento.BentoStyle
import dev.cluvex.zedsecure.ui.bento.LocalBentoStyle
import dev.cluvex.zedsecure.ui.bento.bentoColorScheme
import dev.cluvex.zedsecure.domain.model.ZxPsPrefs
import dev.cluvex.zedsecure.ui.zxps.LocalZxPsStyle
import dev.cluvex.zedsecure.ui.zxps.ZxPsPalettes
import dev.cluvex.zedsecure.ui.zxps.ZxPsStyle
import dev.cluvex.zedsecure.ui.zxps.zxPsColorScheme
import dev.cluvex.zedsecure.ui.zxps.zxPsShapes
import dev.cluvex.zedsecure.ui.zx.zxFutureColors
import dev.cluvex.zedsecure.domain.model.ColorContrast
import dev.cluvex.zedsecure.domain.model.ColorStyle

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ZedSecureTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,

    accentColor: Int = 0,
    amoledBlack: Boolean = false,

    colorStyle: ColorStyle = ColorStyle.ZedSecure,
    colorContrast: ColorContrast = ColorContrast.Standard,
    animateColors: Boolean = true,

    design: AppDesign = AppDesign.Bento,

    bento: BentoPrefs = BentoPrefs(),

    zxAnim: ZxAnim = ZxAnim.Auto,

    zxPs: ZxPsPrefs = ZxPsPrefs(),

    languageTag: String = "en",

    fontScale: Float = 1f,

    font: dev.cluvex.zedsecure.domain.model.UiFont = dev.cluvex.zedsecure.domain.model.UiFont.Auto,

    uiScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val system = if (dynamicColor) dynamicColorSchemeOrNull(darkTheme) else null
    val bentoSeed = system?.primary ?: accentSeed(accentColor)
    val bentoColors = remember(bento.palette, darkTheme, bentoSeed, amoledBlack, design) {
        if (design == AppDesign.Zx) zxFutureColors(darkTheme, amoledBlack)
        else BentoPalettes.of(bento.palette, darkTheme, bentoSeed, amoledBlack)
    }
    val zxPsColors = remember(zxPs.palette, darkTheme) { ZxPsPalettes.of(zxPs.palette, darkTheme) }
    val scheme = if (design == AppDesign.ZxPs) {
        remember(zxPsColors) { zxPsColorScheme(zxPsColors) }
    } else if (design == AppDesign.Zx) {
        remember(bentoColors) { zxAccessibleColorScheme(bentoColors) }
    } else if (design == AppDesign.Bento) {
        remember(bentoColors) { bentoColorScheme(bentoColors) }
    } else {
        (system ?: rememberZedColorScheme(darkTheme, accentColor, colorStyle, colorContrast))
            .let { if (amoledBlack) it.amoled(darkTheme) else it }
    }
    val zxPsStyle = remember(zxPsColors, zxPs) { ZxPsStyle(zxPsColors, zxPs) }
    val bentoStyle = remember(bentoColors, bento, languageTag) { BentoStyle(bentoColors, bento, latin = languageTag != "fa") }
    val colorScheme = if (animateColors) {
        animateColorScheme(scheme, animationSpec = { spring(stiffness = Spring.StiffnessMediumLow) })
    } else {
        scheme
    }

    val density = LocalDensity.current
    val scale = dev.cluvex.zedsecure.domain.model.UiScale.clamp(uiScale)
    val scaled = remember(density, fontScale, scale) {
        if (fontScale == 1f && scale == 1f) density else Density(density.density * scale, density.fontScale * fontScale)
    }
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = when (design) {
            AppDesign.Zx -> ZxFutureShapes
            AppDesign.ZxPs -> remember(zxPs.tile) { zxPsShapes(zxPs.tile) }
            else -> ZedShapes
        },
        typography = typographyFor(languageTag, design, font),
    ) {
        CompositionLocalProvider(
            LocalDensity provides scaled,
            LocalAppDesign provides design,
            LocalBentoStyle provides bentoStyle,
            LocalZxPsStyle provides zxPsStyle,
            content = content,
        )
    }
}


private fun zxAccessibleColorScheme(colors: dev.cluvex.zedsecure.ui.bento.BentoColors): ColorScheme {
    // ZX owns a neutral graphite / porcelain surface system. Bento's palette is
    // only used as a source of accent swatches; its color roles are not reused.
    val primary = if (colors.dark) colors.cyan.content else colors.violet.content
    val secondary = if (colors.dark) colors.violet.content else colors.cyan.content
    val tertiary = if (colors.dark) colors.pink.content else colors.coral.content
    val outline = if (colors.dark) Color(0xFF3B4655) else Color(0xFFB9C3D0)
    return if (colors.dark) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color(0xFF07131A),
            primaryContainer = Color(0xFF17313A),
            onPrimaryContainer = Color(0xFFD7F5F4),
            secondary = secondary,
            onSecondary = Color(0xFF171329),
            secondaryContainer = Color(0xFF28233D),
            onSecondaryContainer = Color(0xFFE8E1FF),
            tertiary = tertiary,
            onTertiary = Color(0xFF25101C),
            background = Color(0xFF0B1017),
            onBackground = Color(0xFFE8EDF4),
            surface = Color(0xFF101720),
            onSurface = Color(0xFFE8EDF4),
            surfaceVariant = Color(0xFF1A2430),
            onSurfaceVariant = Color(0xFFB5C0CE),
            surfaceTint = primary,
            inverseSurface = Color(0xFFE8EDF4),
            inverseOnSurface = Color(0xFF17202A),
            error = Color(0xFFFF6B78),
            onError = Color(0xFF350A12),
            errorContainer = Color(0xFF5A1E2A),
            onErrorContainer = Color(0xFFFFD9DC),
            outline = outline,
            outlineVariant = Color(0xFF293442),
            scrim = Color.Black,
            surfaceBright = Color(0xFF263341),
            surfaceDim = Color(0xFF090D13),
            surfaceContainerLowest = Color(0xFF0A0F15),
            surfaceContainerLow = Color(0xFF131C26),
            surfaceContainer = Color(0xFF17212C),
            surfaceContainerHigh = Color(0xFF1D2936),
            surfaceContainerHighest = Color(0xFF263442),
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE5E1FF),
            onPrimaryContainer = Color(0xFF21175D),
            secondary = secondary,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFD9F1F4),
            onSecondaryContainer = Color(0xFF123E48),
            tertiary = tertiary,
            onTertiary = Color.White,
            background = Color(0xFFF3F5F8),
            onBackground = Color(0xFF18212D),
            surface = Color(0xFFFAFBFD),
            onSurface = Color(0xFF18212D),
            surfaceVariant = Color(0xFFE9EDF2),
            onSurfaceVariant = Color(0xFF536171),
            surfaceTint = primary,
            inverseSurface = Color(0xFF27313D),
            inverseOnSurface = Color(0xFFF2F5F8),
            error = Color(0xFFBA1A35),
            onError = Color.White,
            errorContainer = Color(0xFFFFDADf),
            onErrorContainer = Color(0xFF41000B),
            outline = outline,
            outlineVariant = Color(0xFFD5DCE5),
            scrim = Color.Black,
            surfaceBright = Color.White,
            surfaceDim = Color(0xFFE1E6ED),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color(0xFFF4F6F9),
            surfaceContainer = Color(0xFFEEF1F5),
            surfaceContainerHigh = Color(0xFFE7ECF2),
            surfaceContainerHighest = Color(0xFFDDE4EC),
        )
    }
}

@Composable
fun rememberZedColorScheme(
    dark: Boolean,
    accent: Int,
    style: ColorStyle,
    contrast: ColorContrast,
): ColorScheme = remember(dark, accent, style, contrast) { zedColorScheme(dark, accent, style, contrast) }

fun zedColorScheme(dark: Boolean, accent: Int, style: ColorStyle, contrast: ColorContrast): ColorScheme {
    val seed = accentSeed(accent)
    return if (style == ColorStyle.ZedSecure) {
        dynamicColorScheme(
            seedColor = seed,
            isDark = dark,
            primary = seed,
            secondary = BrandTeal,
            tertiary = BrandRose,
            style = PaletteStyle.TonalSpot,
            contrastLevel = contrast.level,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
        )
    } else {
        dynamicColorScheme(
            seedColor = seed,
            isDark = dark,
            style = style.palette,
            contrastLevel = contrast.level,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
        )
    }
}

fun accentSeed(index: Int): Color = AccentPresets.getOrNull(index)?.light ?: BrandIris

val ColorStyle.palette: PaletteStyle
    get() = when (this) {
        ColorStyle.ZedSecure, ColorStyle.TonalSpot -> PaletteStyle.TonalSpot
        ColorStyle.Vibrant -> PaletteStyle.Vibrant
        ColorStyle.Expressive -> PaletteStyle.Expressive
        ColorStyle.Rainbow -> PaletteStyle.Rainbow
        ColorStyle.FruitSalad -> PaletteStyle.FruitSalad
        ColorStyle.Fidelity -> PaletteStyle.Fidelity
        ColorStyle.Neutral -> PaletteStyle.Neutral
        ColorStyle.Monochrome -> PaletteStyle.Monochrome
    }

private val ZxFutureShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(22.dp),
)

val LocalAppDesign = staticCompositionLocalOf { AppDesign.Bento }

@Composable
expect fun dynamicColorSchemeOrNull(darkTheme: Boolean): ColorScheme?
