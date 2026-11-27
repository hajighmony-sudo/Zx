package dev.cluvex.zedsecure.ui.zx

import androidx.compose.ui.graphics.Color
import dev.cluvex.zedsecure.ui.bento.BentoColors
import dev.cluvex.zedsecure.ui.bento.BentoTone

/**
 * Dedicated visual language for AppDesign.Zx.
 *
 * The rest of the app keeps its own palettes; this palette is intentionally
 * scoped to ZX so the redesign does not leak into other appearance modes.
 */
fun zxFutureColors(dark: Boolean, amoled: Boolean = false): BentoColors {
    if (dark) {
        val ground = if (amoled) Color.Black else Color(0xFF080B18)
        return BentoColors(
            dark = true,
            ground = ground,
            surface = Color(0xFF121B31),
            surface2 = Color(0xFF202D4A),
            line = Color(0xFF435477),
            ink = Color(0xFFF2F4FF),
            muted = Color(0xFFAAB5D2),
            stage = Color(0xFF2B2863),
            onStage = Color(0xFFF1ECFF),
            chipOnStage = Color(0x24FFFFFF),
            goodOnStage = Color(0xFF9CF3D6),
            lime = Color(0xFF8CEAF0),
            onLime = Color(0xFF002B36),
            blue = BentoTone(Color(0xFF16385B), Color(0xFFD8EEFF)),
            violet = BentoTone(Color(0xFF30235F), Color(0xFFE9D9FF)),
            cyan = BentoTone(Color(0xFF124653), Color(0xFFC4F5FA)),
            pink = BentoTone(Color(0xFF48233F), Color(0xFFFFD0EE)),
            coral = BentoTone(Color(0xFF482B36), Color(0xFFFFD8E5)),
            good = Color(0xFF60DDB7),
            mid = Color(0xFFFFC878),
            bad = Color(0xFFFF8DA8),
            selected = Color(0xFF292451),
            dock = Color(0xFF121A30),
            dockLine = Color(0x45FFFFFF),
            danger = Color(0xFFFFB4C6),
            onDanger = Color(0xFF5C102C),
            colorful = true,
        )
    }

    return BentoColors(
        dark = false,
        ground = Color(0xFFF2F5FC),
        surface = Color(0xFFFCFDFF),
        surface2 = Color(0xFFE9EDF8),
        line = Color(0xFFC5D0E8),
        ink = Color(0xFF151A31),
        muted = Color(0xFF5B6685),
        stage = Color(0xFFE6E0FF),
        onStage = Color(0xFF30226B),
        chipOnStage = Color(0x1AFFFFFF),
        goodOnStage = Color(0xFF075D48),
        lime = Color(0xFFB4E9EF),
        onLime = Color(0xFF00313A),
        blue = BentoTone(Color(0xFFDCEAFF), Color(0xFF174273)),
        violet = BentoTone(Color(0xFFE8E0FF), Color(0xFF44318C)),
        cyan = BentoTone(Color(0xFFD4F2F5), Color(0xFF155665)),
        pink = BentoTone(Color(0xFFFFE0F1), Color(0xFF76365F)),
        coral = BentoTone(Color(0xFFFFE1E9), Color(0xFF7A3D52)),
        good = Color(0xFF087C66),
        mid = Color(0xFF865700),
        bad = Color(0xFFB32655),
        selected = Color(0xFFE0E7FF),
        dock = Color(0xFFFDFEFF),
        dockLine = Color(0x26151A31),
        danger = Color(0xFFFFD9E2),
        onDanger = Color(0xFF650D33),
        colorful = true,
    )
}
