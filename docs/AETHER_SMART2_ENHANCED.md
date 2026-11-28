# Aether Smart 2 – Enhanced (Full Network-Driven)

## What changed (only Smart 2 path)

All enhancements are isolated to the Aether Smart 2 feature.
No other engines, Ghost mode, Xray profiles, sing-box, Psiphon, Tor, or UI pages were modified.

### New / Updated files

| File | Action |
|------|--------|
| `domain/config/AntiDpiStack.kt` | **NEW** – 11 anti-DPI techniques + Dual-Attack (Bullet + Mortar) |
| `domain/config/IranianAssets.kt` | **NEW** – Iranian DNS + famous Iranian SNIs |
| `domain/config/AetherSmart2Profile.kt` | **REPLACE** – protocol choice (only manual field), full probe-driven build, Iranian DNS priority in Xray front |
| `ui/servers/AetherSmart2Dialog.kt` | **REPLACE** – protocol chips (خودکار / MASQUE H3 / H2 / WG / Gool / MASQUE×2 / Hybrid) |

### Design rules respected

1. **Only manual control** = Protocol selection.  
   Option `AUTOMATIC` chooses the best protocol from live network evidence.
2. **Everything else** (DNS, SNI, Noize, Fragment, ECH, Scan depth, API address, Anti-DPI stack) is derived from the user’s current internet via `AetherSmartProbe`.
3. **Iranian assets** are preferred when country = IR or OONI anomaly is elevated; otherwise they remain as strong fallbacks.
4. **Dual-Attack mode** (Method 2 metaphor):
   - **Bullet** = Interactive Reality Punch + direct high-power path that tries to go *through* the DPI wall.
   - **Mortar** = newest techniques that climb *over* the wall; warhead is a hybrid of newest + oldest methods.
5. Backward compatible marker `ZX_AETHER_SMART_2` kept so existing saved profiles still load.

### Anti-DPI – 11 methods

**Newest (2025-2026)**  
1. Interactive Reality Punch  
2. ECH + Adaptive Padding  
3. Multi-Path MASQUE  
4. Statistical Traffic Shaping  
5. Live DPI Fingerprint Mutation  

**Proven older**  
6. Noize (light → aggressive)  
7. ClientHello Fragmentation  
8. SNI Spoof / Domain Fronting (Iranian + global cover SNIs)  
9. TLS Record Padding  
10. UDP/TCP Hybrid Desync  

**Combined**  
11. Full Combo (last-resort ladder step)

When the probe reports hostile conditions the stack automatically enables **Dual-Attack** (Bullet + Mortar).

### Integration steps

1. Copy the four files into the corresponding packages of the `GP` branch.
2. Rebuild the Android target.
3. Existing call sites that invoke `AetherSmart2Profile.build(network, hasIpv6, probe)` continue to work (new parameter has default `AUTOMATIC`).
4. Optional: update unit tests under `AetherSmart2ProfileTest` if they assert exact field sets.

### Non-goals (intentionally untouched)

- Ghost MOD / GhostDpiDetector / GhostLayerProfiles
- Aether Smart (v1) path
- Core Xray / sing-box / Psiphon / Tor engines
- Desktop / iOS specific code (except shared domain models)
- Any existing manual Aether profile editor

---

Ready for drop-in on branch `GP`.
