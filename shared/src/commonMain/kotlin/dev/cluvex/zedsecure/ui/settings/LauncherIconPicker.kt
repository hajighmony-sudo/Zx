package dev.cluvex.zedsecure.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class LauncherIconOption(
    val id: String,
    val title: String,
    val background: List<Color>,
    val accent: Color = Color.White,
    val finish: IconFinish = IconFinish.Clean,
)

private enum class IconFinish { Clean, Steel, Blade, Silver, Distressed }

private val launcherIconOptions = listOf(
    LauncherIconOption("zx_orange", "نارنجی", listOf(Color(0xFF090909), Color(0xFF24201A)), Color(0xFFFF8A00)),
    LauncherIconOption("zx_steel", "فلزی", listOf(Color(0xFF08090B), Color(0xFF34383D)), Color(0xFFE1E4E8), IconFinish.Steel),
    LauncherIconOption("zx_blade", "تیغه‌ای", listOf(Color(0xFF050505), Color(0xFF17191D)), Color(0xFFF2F2F2), IconFinish.Blade),
    LauncherIconOption("zx_white", "سفید", listOf(Color(0xFF050505), Color(0xFF202020))),
    LauncherIconOption("zx_silver", "نقره‌ای", listOf(Color(0xFF111315), Color(0xFF4A4D50)), Color(0xFFD5D8DC), IconFinish.Silver),
    LauncherIconOption("zx_distressed", "فرسوده", listOf(Color(0xFF050505), Color(0xFF252525)), Color(0xFFE5E2DC), IconFinish.Distressed),
)

/** Large, high-contrast previews make the launcher artwork easy to compare before applying it. */
@Composable
fun LauncherIconPicker(
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("انتخاب آیکون", style = MaterialTheme.typography.titleMedium)
        Text(
            "پیش‌نمایش طرح‌ها · با انتخاب هر طرح، آیکون صفحه اصلی تغییر می‌کند",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            launcherIconOptions.forEach { option ->
                val isSelected = selected == option.id
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onSelect(option.id) }
                        .padding(3.dp),
                ) {
                    Box(
                        Modifier
                            .size(104.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .background(Brush.linearGradient(option.background))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color.White.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(25.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (option.finish == IconFinish.Steel || option.finish == IconFinish.Silver) {
                            Box(
                                Modifier
                                    .size(88.dp)
                                    .clip(RoundedCornerShape(21.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color.White.copy(alpha = 0.08f), Color.Transparent, Color.Black.copy(alpha = 0.22f))
                                        )
                                    )
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Z",
                                color = option.accent,
                                fontSize = 39.sp,
                                lineHeight = 40.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-5).sp,
                            )
                            Text(
                                "X",
                                color = option.accent,
                                fontSize = 39.sp,
                                lineHeight = 40.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-4).sp,
                            )
                        }
                        if (option.finish == IconFinish.Blade) {
                            Box(
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp)
                                    .size(width = 31.dp, height = 2.dp)
                                    .background(Color.White.copy(alpha = 0.75f))
                            )
                        }
                        if (option.finish == IconFinish.Distressed) {
                            Text(
                                "✦",
                                modifier = Modifier.align(Alignment.TopEnd).padding(9.dp),
                                color = Color.White.copy(alpha = 0.42f),
                                fontSize = 10.sp,
                            )
                        }
                        if (isSelected) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(7.dp)
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("✓", color = MaterialTheme.colorScheme.onPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        option.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
