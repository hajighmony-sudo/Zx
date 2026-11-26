@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package dev.cluvex.zedsecure.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.cluvex.zedsecure.shared.resources.Res
import dev.cluvex.zedsecure.shared.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource


@Composable
fun PrivacyPage(
    contentPadding: PaddingValues,
    modifier: Modifier,
) {
    SettingsPageScaffold(
        title = stringResource(Res.string.privacy_title),
        subtitle = stringResource(Res.string.privacy_subtitle),
        contentPadding = contentPadding,
        modifier = modifier,
        art = SettingsArts.Privacy,
    ) {
        Surface(
            shape = MaterialTheme.shapes.largeIncreased,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(
                    painterResource(Res.drawable.ic_policy),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    stringResource(Res.string.privacy_headline),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Section(
                stringResource(Res.string.privacy_client_title),
                stringResource(Res.string.privacy_client_body),
            )
            Section(
                stringResource(Res.string.privacy_device_title),
                stringResource(Res.string.privacy_device_body),
            )
            Section(
                stringResource(Res.string.privacy_nocollect_title),
                stringResource(Res.string.privacy_nocollect_body),
            )
            Section(
                stringResource(Res.string.privacy_third_title),
                stringResource(Res.string.privacy_third_body),
            )
            Section(
                stringResource(Res.string.privacy_perm_title),
                stringResource(Res.string.privacy_perm_body),
            )
            Section(
                stringResource(Res.string.privacy_deviceid_title),
                stringResource(Res.string.privacy_deviceid_body),
            )
            Section(
                stringResource(Res.string.privacy_limits_title),
                stringResource(Res.string.privacy_limits_body),
            )
        }

        Spacer(Modifier.height(14.dp))

        Section(
            "دستیار هوش مصنوعی و ارسال داده",
            "اگر دستیار هوش مصنوعی را فعال کنید، متن پیام‌ها، زمینه‌ای که برای پاسخ لازم است و اطلاعاتی که خودتان وارد می‌کنید برای پردازش به ارائه‌دهنده‌ای که انتخاب کرده‌اید ارسال می‌شود. کلید API برای همان ارائه‌دهنده استفاده می‌شود. اطلاعات محرمانه، رمزها و داده‌های حساس را در گفتگو وارد نکنید؛ نگهداری و استفاده از داده‌ها تابع سیاست همان ارائه‌دهنده است."
        )
        Section(
            "اتصال‌های بیرونی و نشانی IP",
            "برای برقراری VPN یا پراکسی، دستگاه مستقیماً به سرور یا سرویس انتخابی شما متصل می‌شود. ارائه‌دهنده اینترنت، سرور مقصد، سرویس DNS، سرویس تست سرعت و منابعی مانند مخزن قوانین یا سرویس بررسی نسخه ممکن است نشانی IP و فراداده‌های لازم برای پاسخ‌گویی درخواست را مشاهده کنند. این ارتباط‌ها به معنی ارسال همه داده‌ها به سازنده برنامه نیست."
        )
        Section(
            "مجوزها و دسترسی‌ها",
            "مجوز VPN برای ساخت تونل شبکه، اعلان برای نمایش وضعیت اتصال و دسترسی فایل برای واردکردن یا ذخیره‌کردن مواردی که خودتان انتخاب می‌کنید به کار می‌رود. فهرست برنامه‌های نصب‌شده در قابلیت پراکسی برنامه‌ای برای انتخاب محلی استفاده می‌شود. دسترسی‌های اختیاری را می‌توانید از تنظیمات اندروید مدیریت کنید."
        )
        Section(
            "پشتیبان‌گیری، حذف و مسئولیت کاربر",
            "تنظیمات و پیکربندی‌ها ممکن است در پشتیبان‌گیری سیستم‌عامل قرار بگیرند؛ رفتار آن به تنظیمات Android و سازنده دستگاه بستگی دارد. حذف برنامه داده‌های خصوصی محلی را پاک می‌کند، اما داده‌هایی را که قبلاً به سرویس‌های ثالث فرستاده شده‌اند حذف نمی‌کند. پیش از اشتراک‌گذاری فایل کانفیگ یا گزارش خطا، اطلاعات شناسایی‌کننده و کلیدها را بررسی و حذف کنید."
        )
        Section(
            "محدودیت‌های امنیت و ناشناس‌ماندن",
            "VPN جای خروج ترافیک را تغییر می‌دهد و به‌تنهایی ناشناس‌بودن، امنیت کامل یا جلوگیری از ردیابی توسط وب‌سایت‌ها و برنامه‌ها را تضمین نمی‌کند. امنیت نهایی به سرور، ارائه‌دهنده، DNS، دستگاه، نسخه سیستم‌عامل و تنظیمات شما وابسته است. از سرورهای ناشناس و کانفیگ‌هایی که به آن‌ها اعتماد ندارید استفاده نکنید."
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Section(title: String, body: String) {
    Surface(
        shape = MaterialTheme.shapes.largeIncreased,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
