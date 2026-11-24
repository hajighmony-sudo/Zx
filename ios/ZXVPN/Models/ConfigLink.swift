import Foundation

/// A proxy link reduced to what the Explorer needs: protocol, display name and the server endpoint.
struct ParsedLink: Equatable {
    var link: String
    var proto: String
    var name: String
    var host: String
    var port: Int
    /// Connection-oriented over TCP, so a plain TCP connect says something about reachability.
    var usesTCP: Bool { !["hysteria", "hysteria2", "hy2", "tuic"].contains(proto) }
}

enum ConfigLink {
    static let schemes = ["vless", "vmess", "trojan", "ss", "hysteria", "hysteria2", "hy2", "tuic"]

    /// Pulls every proxy link out of a feed: plain text lines, or a base64 subscription body.
    static func extractLinks(_ body: String) -> [String] {
        var text = body
        if !text.contains("://"), let data = Base64Loose.decode(text), let decoded = String(data: data, encoding: .utf8) {
            text = decoded
        }
        var seen = Set<String>()
        var out: [String] = []
        for raw in text.components(separatedBy: .newlines) {
            let line = raw.trimmingCharacters(in: .whitespacesAndNewlines)
            guard let scheme = line.range(of: "://").map({ String(line[line.startIndex..<$0.lowerBound]).lowercased() }),
                  schemes.contains(scheme), seen.insert(line).inserted else { continue }
            out.append(line)
        }
        return out
    }

    static func parse(_ link: String) -> ParsedLink? {
        let trimmed = link.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let range = trimmed.range(of: "://") else { return nil }
        let scheme = String(trimmed[trimmed.startIndex..<range.lowerBound]).lowercased()
        guard schemes.contains(scheme) else { return nil }
        if scheme == "vmess" { return parseVmess(trimmed) }
        if scheme == "ss", let parsed = parseShadowsocks(trimmed) { return parsed }
        guard let components = URLComponents(string: trimmed), let host = components.host, !host.isEmpty else { return nil }
        let port = components.port ?? 443
        let name = components.fragment?.removingPercentEncoding ?? ""
        return ParsedLink(link: trimmed, proto: scheme, name: name, host: host, port: port)
    }

    private static func parseVmess(_ link: String) -> ParsedLink? {
        let payload = String(link.dropFirst("vmess://".count)).split(separator: "#").first.map(String.init) ?? ""
        guard let data = Base64Loose.decode(payload),
              let object = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any],
              let host = object["add"] as? String, !host.isEmpty else { return nil }
        let portValue = object["port"]
        let port = (portValue as? Int) ?? Int((portValue as? String) ?? "") ?? 443
        return ParsedLink(link: link, proto: "vmess", name: (object["ps"] as? String) ?? "", host: host, port: port)
    }

    /// ss://base64(method:pass)@host:port#name   or   ss://base64(method:pass@host:port)#name
    private static func parseShadowsocks(_ link: String) -> ParsedLink? {
        let name = link.split(separator: "#", maxSplits: 1, omittingEmptySubsequences: false).dropFirst().first
            .flatMap { String($0).removingPercentEncoding } ?? ""
        let body = String(link.dropFirst("ss://".count)).split(separator: "#").first.map(String.init) ?? ""
        let withoutQuery = body.split(separator: "?").first.map(String.init) ?? body
        var endpoint = withoutQuery
        if let at = withoutQuery.lastIndex(of: "@") {
            endpoint = String(withoutQuery[withoutQuery.index(after: at)...])
        } else if let data = Base64Loose.decode(withoutQuery), let decoded = String(data: data, encoding: .utf8),
                  let at = decoded.lastIndex(of: "@") {
            endpoint = String(decoded[decoded.index(after: at)...])
        } else {
            return nil
        }
        guard let colon = endpoint.lastIndex(of: ":"), let port = Int(endpoint[endpoint.index(after: colon)...]) else { return nil }
        let host = String(endpoint[endpoint.startIndex..<colon]).trimmingCharacters(in: CharacterSet(charactersIn: "[]"))
        guard !host.isEmpty else { return nil }
        return ParsedLink(link: link, proto: "ss", name: name, host: host, port: port)
    }
}
