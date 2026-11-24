# ZX VPN for iOS (SwiftUI)

Native iPhone version of ZX VPN, written in Swift / SwiftUI. Generated with [XcodeGen](https://github.com/yonaskolb/XcodeGen):

```
brew install xcodegen
cd ios && xcodegen generate && open ZXVPN.xcodeproj
```

## What is in the app

| Area | Status |
| --- | --- |
| ZX cyberpunk UI, floating glass tab bar, Persian + English | done |
| Home, Servers, Settings, GHOST Anti-Porn lists | done |
| Smart config (builds an Aether profile from your network) | done |
| Explorer: live discovery, TCP latency, country by IP, countries tab (US/DE/TH/ES…), starred | done |
| ZX Link Hunter (GitHub repo / folder / blob / raw / Gist / https) | done |
| Per-config details (IP, country, city, ISP, ASN, coordinates) | done |
| Swipe right = rename, swipe left = export (renamed link) | done |
| Packet-tunnel extension | scaffold — the Xray / Aether cores are **not** embedded yet |

## Honest limits

* iOS cannot run `libaether.so` / `libv2ray`. Real tunnelling needs the cores built as xcframeworks (gomobile for Xray, a Rust static lib for Aether) and linked into `ZXTunnel`.
* The Explorer on iOS tests **TCP reachability**, not the HTTPS-through-core test of the Android app. UDP protocols (Hysteria, TUIC) are listed but marked "UDP".
* A VPN app needs the Network Extension entitlement and an Apple Developer account to run on a device. CI builds an **unsigned** `.ipa` (artifact `ZXVPN-unsigned-ipa`) that can be re-signed with AltStore / Sideloadly.
