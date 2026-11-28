# Ghost Mode Advanced — ZX VPN (شاخه GP)

**نسخه فوق‌العاده قوی ضد DPI (۲۰۲۶)**

این سند مشخصات کامل تقویت **فقط** بخش GHOST MOD است.  
هیچ فایل یا قابلیت دیگری دست نخورده باقی می‌ماند.

## لایه‌های دفاعی

| لایه | نام | توضیح |
|------|-----|-------|
| 0 | Detection & Status | DPI زنده + رنگ وضعیت + وقت ایران |
| 1 | Soft Camouflage | Reality + uTLS + XHTTP + Fragment |
| 2 | Hard Desync | fake / multi-split / wrong-seq / TTL |
| 3 | Statistical Shaping | noise + padding |
| 4 | Adaptive Ladder | Auto-Tune هر ۳۰–۶۰ ثانیه |
| 5 | Fallback | Hysteria2 / AmneziaWG / DNS / Psiphon / Tor |

## Auto-Tune

با `onEnabled()` حلقه شروع می‌شود؛ فاصله از پروفایل (۳۰–۶۰ث). هر tick دتکتور DPI را صدا می‌زند و در صورت فشار لایه را بالا می‌برد. `onDisabled()` حلقه را لغو می‌کند.

## قابلیت خفن: اسکن اینترنت / پهنای باند کشورها

`GhostCountryNetProbe` + `GhostStatusManager.probeCountries(codes)`

- endpointهای عمومی در DE, NL, US, TR, AE, SG, JP, KR, …
- خروجی: reachable، latencyMs، downloadKbps تقریبی، بهترین کشور، خلاصه به وقت ایران
- الهام از اندازه‌گیری سبک Cloudflare-style بدون وابستگی اضافه

```kotlin
val report = GhostStatusManager.probeCountries(listOf("DE", "NL", "US", "TR"))
println(report.summaryFa)
println(report.bestCountryCode)
```

## فایل‌ها

`GhostStatusAdvanced`, `GhostDpiDetector`, `GhostLayerProfiles`, `GhostStatusManager`, `GhostCountryNetProbe` + هسته قبلی GhostEngine.

Anti-Porn و بقیه قابلیت‌ها بدون تغییر.
