# Ghost Mode Advanced — ZX VPN (شاخه GP)

**فقط GHOST MOD** — بقیه قابلیت‌ها دست نخورده.

## کاهش وابستگی به Cloudflare

- `GhostDpiDetector` و `GhostCountryNetProbe` به `cloudflare.com` / `1.1.1.1` وابسته نیستند.
- donorها چندفروشنده: Microsoft, Apple, Hetzner, DigitalOcean, Samsung, Sony, …
- DNS health از resolver سیستم روی نام‌های عمومی غیر-CF استفاده می‌کند.

## ماژول‌های Ghost

| ماژول | نقش |
|--------|-----|
| GhostDpiDetector | probe موازی HTTPS چندفروشنده |
| GhostStatusManager | ladder + auto-tune ۳۰–۶۰ث + APIها |
| GhostCountryNetProbe | latency/پهنای باند کشورها |
| GhostExitAdvisor | پیشنهاد بهترین کشور خروج |
| GhostDnsHealth | سلامت DNS بدون DoH اجباری CF |
| GhostStealthKit | decoy چندفروشنده + fragment hint |
| GhostLayerProfiles | iran_mobile / home / russia / china / auto |

## APIهای مفید

```kotlin
GhostStatusManager.probeCountries(listOf("DE","NL","TR"))
GhostStatusManager.checkDns()
GhostStatusManager.stealthPlan()
GhostStatusManager.lastExitAdvice()
GhostStatusManager.buildEngineHints()
```
