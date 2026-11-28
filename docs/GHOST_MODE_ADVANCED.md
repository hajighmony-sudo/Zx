# Ghost Mode Advanced — ZX VPN (شاخه GP)

**نسخه فوق‌العاده قوی ضد DPI (۲۰۲۶)**

این سند مشخصات کامل تقویت **فقط** بخش GHOST MOD است.  
هیچ فایل یا قابلیت دیگری دست نخورده باقی می‌ماند.

## لایه‌های دفاعی (از سبک به سنگین)

| لایه | نام | توضیح کوتاه |
|------|-----|-------------|
| 0 | Detection & Status | تشخیص وجود و شدت DPI + نمایش وضعیت زنده (سبز/زرد/قرمز) + زمان به وقت ایران |
| 1 | Soft Camouflage | Reality + uTLS + XHTTP + Vision + Fragment ClientHello |
| 2 | Hard Desync | packet-level desync (fake / multi-split / wrong-seq / TTL) الهام از zapret2 / sonicdpi |
| 3 | Statistical Shaping | noise + padding + ترافیک شبیه HTTPS واقعی |
| 4 | Adaptive Ladder | نردبان هوشمند — Auto-Tuner هر ۳۰–۶۰ ثانیه بر اساس موفقیت واقعی ترافیک |
| 5 | Fallback & Multi-Path | Hysteria2 / AmneziaWG / DNS tunnels / Psiphon / Tor |

## وضعیت‌ها (Status Dashboard)

- `NONE` / `LOW` / `MEDIUM` / `HIGH` / `EXTREME` — شدت DPI
- نوع تشخیص‌شده: SNI_BLOCKING, TLS_FINGERPRINT, ACTIVE_PROBING, STATISTICAL_ML, UDP_THROTTLING, …
- Visibility: INVISIBLE (سبز) / UNDER_PRESSURE (زرد) / ADAPTING (قرمز)
- آخرین تست با زمان ایران

## پروفایل‌های از پیش تعریف‌شده

- `iran_mobile` — ایران دیتای موبایل (قوی‌ترین تنظیمات موبایل)
- `iran_home` — ایران خانگی / ADSL
- `russia` — روسیه / TSPU
- `china` — چین / GFW
- `auto` — انتخاب خودکار
- `custom` — تنظیم دستی

## فایل‌های اضافه شده (فقط Ghost)

```
shared/src/commonMain/kotlin/dev/cluvex/zedsecure/core/ghost/
├── GhostStatusAdvanced.kt   # مدل وضعیت + DpiLevel + Visibility + GhostLayer
├── GhostDpiDetector.kt      # تشخیص DPI موازی
├── GhostLayerProfiles.kt    # پروفایل‌های قوی ۲۰۲۶
└── GhostStatusManager.kt    # داشبورد وضعیت زنده + Auto-Tune + Engine Hints
```

موجود قبلی (GhostEngine / GhostAutoTuner / GhostStrategy / …) بدون تغییر باقی مانده و این لایه روی آن سوار می‌شود.

## نکات

- از هسته‌های موجود Xray / sing-box / Aether استفاده می‌شود.
- Auto-Tuner بر اساس موفقیت واقعی ترافیک (نه فقط handshake) سطح را بالا می‌برد.
- Anti-Porn همچنان بخشی از Ghost Mode است و تغییری نکرده.
- زمان‌ها به وقت ایران نمایش داده می‌شوند.
