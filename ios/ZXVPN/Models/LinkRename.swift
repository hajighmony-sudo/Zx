import Foundation

/// Same behaviour as the Android `LinkRename`: gives a config link a new display name so it can be
/// exported under that name. vmess:// keeps the name in its base64 JSON ("ps"); every other scheme
/// carries it in the URL fragment.
enum LinkRename {
    static func apply(_ link: String, name: String) -> String {
        let clean = name.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmed = link.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !clean.isEmpty else { return link }
        if trimmed.lowercased().hasPrefix("vmess://") {
            return renameVmess(trimmed, name: clean) ?? trimmed
        }
        guard trimmed.contains("://") else { return trimmed }
        let base = trimmed.split(separator: "#", maxSplits: 1, omittingEmptySubsequences: false).first.map(String.init) ?? trimmed
        return base + "#" + encode(clean)
    }

    private static func renameVmess(_ link: String, name: String) -> String? {
        let payload = String(link.dropFirst("vmess://".count)).split(separator: "#").first.map(String.init) ?? ""
        guard let data = Base64Loose.decode(payload),
              var object = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] else { return nil }
        object["ps"] = name
        guard let out = try? JSONSerialization.data(withJSONObject: object, options: [.sortedKeys]) else { return nil }
        return "vmess://" + out.base64EncodedString()
    }

    /// Percent-encodes everything except RFC 3986 unreserved characters (UTF-8).
    static func encode(_ text: String) -> String {
        var allowed = CharacterSet.alphanumerics
        allowed.insert(charactersIn: "-_.~")
        return text.addingPercentEncoding(withAllowedCharacters: allowed) ?? text
    }
}

/// Base64 that tolerates URL-safe alphabets and missing padding, as found in real subscription files.
enum Base64Loose {
    static func decode(_ text: String) -> Data? {
        var s = text.trimmingCharacters(in: .whitespacesAndNewlines)
            .replacingOccurrences(of: "-", with: "+")
            .replacingOccurrences(of: "_", with: "/")
        let remainder = s.count % 4
        if remainder > 0 { s += String(repeating: "=", count: 4 - remainder) }
        return Data(base64Encoded: s)
    }
}
