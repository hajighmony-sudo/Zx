import Foundation

/// Everything the IP database knows about a server's address.
struct GeoDetails: Codable, Equatable {
    var ip: String
    var countryCode: String?
    var country: String?
    var region: String?
    var city: String?
    var isp: String?
    var asn: String?
    var latitude: Double?
    var longitude: Double?
}

/// Resolves a host and asks the IP database (api.ip.sb) about it. Results are cached per host.
actor GeoLookup {
    static let shared = GeoLookup()
    private var cache: [String: GeoDetails] = [:]
    private var failedAt: [String: Date] = [:]

    func details(of host: String) async -> GeoDetails? {
        if host.isEmpty { return nil }
        if let hit = cache[host] { return hit }
        if let failed = failedAt[host], Date().timeIntervalSince(failed) < 600 { return nil }
        guard let ip = await Self.resolve(host), let url = URL(string: "https://api.ip.sb/geoip/\(ip)") else {
            failedAt[host] = Date()
            return nil
        }
        var request = URLRequest(url: url, timeoutInterval: 8)
        request.setValue("ZX-VPN-iOS/1.0", forHTTPHeaderField: "User-Agent")
        guard let (data, _) = try? await URLSession.shared.data(for: request),
              let object = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] else {
            failedAt[host] = Date()
            return nil
        }
        func text(_ keys: String...) -> String? {
            for key in keys {
                if let s = object[key] as? String, !s.isEmpty { return s }
                if let n = object[key] as? NSNumber { return n.stringValue }
            }
            return nil
        }
        func number(_ keys: String...) -> Double? {
            for key in keys {
                if let d = object[key] as? Double { return d }
                if let s = object[key] as? String, let d = Double(s) { return d }
            }
            return nil
        }
        let code = text("country_code", "countryCode").flatMap { $0.count == 2 ? $0.uppercased() : nil }
        let found = GeoDetails(
            ip: ip, countryCode: code, country: text("country"), region: text("region", "region_name"),
            city: text("city"), isp: text("organization", "isp", "org"), asn: text("asn", "as"),
            latitude: number("latitude", "lat"), longitude: number("longitude", "lon")
        )
        if code != nil { cache[host] = found } else { failedAt[host] = Date() }
        return found
    }

    /// First IPv4/IPv6 address of a host (or the host itself when it already is an IP).
    private static func resolve(_ host: String) async -> String? {
        await withCheckedContinuation { continuation in
            DispatchQueue.global(qos: .utility).async {
                var hints = addrinfo(ai_flags: 0, ai_family: AF_UNSPEC, ai_socktype: SOCK_STREAM, ai_protocol: 0,
                                     ai_addrlen: 0, ai_canonname: nil, ai_addr: nil, ai_next: nil)
                var result: UnsafeMutablePointer<addrinfo>?
                guard getaddrinfo(host, nil, &hints, &result) == 0, let first = result else {
                    continuation.resume(returning: nil)
                    return
                }
                defer { freeaddrinfo(result) }
                var buffer = [CChar](repeating: 0, count: Int(NI_MAXHOST))
                let status = getnameinfo(first.pointee.ai_addr, first.pointee.ai_addrlen, &buffer, socklen_t(buffer.count),
                                         nil, 0, NI_NUMERICHOST)
                continuation.resume(returning: status == 0 ? String(cString: buffer) : nil)
            }
        }
    }
}

enum Country {
    /// Flag emoji from a two-letter region code.
    static func flag(_ code: String?) -> String {
        guard let code, code.count == 2 else { return "🌐" }
        let base: UInt32 = 0x1F1E6
        return code.uppercased().unicodeScalars.compactMap { UnicodeScalar(base + $0.value - 65) }
            .map { String($0) }.joined()
    }

    /// Localised country name (Persian when the app language is Persian).
    static func name(_ code: String?) -> String {
        guard let code else { return "—" }
        return Locale.current.localizedString(forRegionCode: code) ?? code
    }

    /// Countries users ask for first; they always get a card in the Countries tab.
    static let priority = ["US", "DE", "TH", "ES", "NL", "GB", "FR", "TR", "JP", "SG", "CA", "FI", "SE", "CH", "IT", "AE"]
}
