import Foundation

/// What a pasted link points at (same rules as the Android Link Hunter).
enum HuntTarget: Equatable {
    case feed(String)
    case folder(owner: String, repo: String, ref: String?, dir: String)
    case unsupported(String)
}

struct HuntedFeed: Equatable {
    var url: String
    var links: Int
}

struct HuntResult {
    var feeds: [HuntedFeed]
    var note: String
}

enum LinkHunter {
    private static let maxFiles = 14
    private static let goodExtensions: Set<String> = ["txt", "list", "conf", "sub", "yaml", "yml", "json", "b64", "base64", ""]
    private static let goodWords = ["sub", "config", "vless", "vmess", "trojan", "ss", "hysteria", "tuic", "all", "mix", "merge",
                                    "base64", "v2ray", "proxy", "server", "reality", "best", "verified", "normal"]
    private static let badNames = ["readme", "license", "changelog", "contributing", "package", "docker"]

    static func looksLikeLink(_ text: String) -> Bool {
        let t = text.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        if t.count < 8 || t.contains(where: { $0.isWhitespace }) { return false }
        return t.hasPrefix("http://") || t.hasPrefix("https://") || t.hasPrefix("github.com/") ||
            t.hasPrefix("www.github.com/") || t.hasPrefix("raw.githubusercontent.com/") || t.hasPrefix("gist.github.com/")
    }

    /// Pure parsing, no network.
    static func classify(_ input: String) -> HuntTarget {
        var text = input.trimmingCharacters(in: .whitespacesAndNewlines)
        text = String(text.split(separator: "#", maxSplits: 1, omittingEmptySubsequences: false).first ?? "")
        text = String(text.split(separator: "?", maxSplits: 1, omittingEmptySubsequences: false).first ?? "")
        if text.isEmpty || text.contains(where: { $0.isWhitespace }) { return .unsupported("Empty or invalid link") }
        if !text.hasPrefix("http://") && !text.hasPrefix("https://") { text = "https://" + text }
        text = text.replacingOccurrences(of: "http://", with: "https://")
        while text.hasSuffix("/") { text.removeLast() }
        let rest = String(text.dropFirst("https://".count))
        let host = String(rest.split(separator: "/", maxSplits: 1, omittingEmptySubsequences: false).first ?? "").lowercased()
            .replacingOccurrences(of: "www.", with: "")
        let parts = rest.split(separator: "/").dropFirst().map(String.init)
        switch host {
        case "raw.githubusercontent.com", "gist.githubusercontent.com":
            return .feed(text)
        case "gist.github.com":
            return parts.count >= 2 ? .feed("https://gist.githubusercontent.com/\(parts[0])/\(parts[1])/raw")
                                    : .unsupported("Incomplete gist link")
        case "github.com":
            return classifyGithub(parts)
        default:
            return host.contains(".") ? .feed(text) : .unsupported("Invalid address")
        }
    }

    private static func classifyGithub(_ parts: [String]) -> HuntTarget {
        guard parts.count >= 2 else { return .unsupported("Incomplete repository link") }
        let owner = parts[0]
        var repo = parts[1]
        if repo.hasSuffix(".git") { repo.removeLast(4) }
        if parts.count == 2 { return .folder(owner: owner, repo: repo, ref: nil, dir: "") }
        let kind = parts[2]
        let ref = parts.count > 3 ? parts[3] : nil
        let path = parts.dropFirst(4).joined(separator: "/")
        if (kind == "blob" || kind == "raw"), let ref, !path.isEmpty {
            return .feed("https://raw.githubusercontent.com/\(owner)/\(repo)/\(ref)/\(path)")
        }
        if kind == "tree", let ref { return .folder(owner: owner, repo: repo, ref: ref, dir: path) }
        return .folder(owner: owner, repo: repo, ref: nil, dir: "")
    }

    static func hunt(_ input: String) async -> HuntResult {
        switch classify(input) {
        case .unsupported(let reason):
            return HuntResult(feeds: [], note: reason)
        case .feed(let url):
            return await verify([url], prefix: "")
        case .folder(let owner, let repo, let ref, let dir):
            let files = await listFolder(owner: owner, repo: repo, ref: ref, dir: dir)
            if files.isEmpty { return HuntResult(feeds: [], note: "No config-like files found in this repository") }
            return await verify(files, prefix: "\(files.count) files checked · ")
        }
    }

    private static func verify(_ urls: [String], prefix: String) async -> HuntResult {
        let checked = await withTaskGroup(of: HuntedFeed.self) { group -> [HuntedFeed] in
            for url in urls { group.addTask { HuntedFeed(url: url, links: await countLinks(url)) } }
            var all: [HuntedFeed] = []
            for await item in group { all.append(item) }
            return all
        }
        let good = checked.filter { $0.links > 0 }.sorted { $0.links > $1.links }
        let total = good.reduce(0) { $0 + $1.links }
        let note = good.isEmpty ? prefix + "No compatible config links inside" : prefix + "\(good.count) valid feeds · \(total) links"
        return HuntResult(feeds: good, note: note)
    }

    static func fetchText(_ urlString: String, timeout: TimeInterval = 15) async -> String? {
        for candidate in mirrors(urlString) {
            guard let url = URL(string: candidate) else { continue }
            var request = URLRequest(url: url, timeoutInterval: timeout)
            request.setValue("ZX-Link-Hunter/1.0", forHTTPHeaderField: "User-Agent")
            if let (data, response) = try? await URLSession.shared.data(for: request),
               let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode), data.count < 6_000_000,
               let text = String(data: data, encoding: .utf8) {
                return text
            }
        }
        return nil
    }

    /// raw.githubusercontent.com first, then the jsDelivr mirror (reachable where GitHub raw is not).
    static func mirrors(_ url: String) -> [String] {
        let raw = "https://raw.githubusercontent.com/"
        guard url.hasPrefix(raw) else { return [url] }
        let parts = url.dropFirst(raw.count).split(separator: "/").map(String.init)
        guard parts.count >= 4 else { return [url] }
        return [url, "https://cdn.jsdelivr.net/gh/\(parts[0])/\(parts[1])@\(parts[2])/\(parts.dropFirst(3).joined(separator: "/"))"]
    }

    private static func countLinks(_ url: String) async -> Int {
        guard let text = await fetchText(url) else { return 0 }
        return ConfigLink.extractLinks(text).count
    }

    private static func acceptable(_ path: String, size: Int) -> Bool {
        let name = (path as NSString).lastPathComponent
        let lower = name.lowercased()
        if name.hasPrefix(".") || badNames.contains(where: { lower.hasPrefix($0) }) { return false }
        let ext = name.contains(".") ? (lower as NSString).pathExtension : ""
        return goodExtensions.contains(ext) && (size <= 0 || size <= 6_000_000)
    }

    private static func score(_ name: String) -> Int {
        let lower = name.lowercased()
        return goodWords.filter { lower.contains($0) }.count
    }

    private static func pick(_ files: [(url: String, path: String)]) -> [String] {
        let sorted = files.sorted { a, b in
            let sa = score((a.path as NSString).lastPathComponent), sb = score((b.path as NSString).lastPathComponent)
            if sa != sb { return sa > sb }
            return a.path.filter { $0 == "/" }.count < b.path.filter { $0 == "/" }.count
        }
        var seen = Set<String>()
        return sorted.map { $0.url }.filter { seen.insert($0).inserted }.prefix(maxFiles).map { $0 }
    }

    private static func getJSON(_ urlString: String) async -> Any? {
        guard let url = URL(string: urlString) else { return nil }
        var request = URLRequest(url: url, timeoutInterval: 15)
        request.setValue("ZX-Link-Hunter/1.0", forHTTPHeaderField: "User-Agent")
        guard let (data, response) = try? await URLSession.shared.data(for: request),
              let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else { return nil }
        return try? JSONSerialization.jsonObject(with: data)
    }

    private static func listFolder(owner: String, repo: String, ref: String?, dir: String) async -> [String] {
        // 1) GitHub contents API (exact, but rate limited and sometimes blocked).
        let suffix = dir.isEmpty ? "" : "/" + dir
        let refQuery = ref.map { "?ref=\($0)" } ?? ""
        if let array = await getJSON("https://api.github.com/repos/\(owner)/\(repo)/contents\(suffix)\(refQuery)") as? [[String: Any]] {
            let files: [(url: String, path: String)] = array.compactMap { item in
                guard (item["type"] as? String) == "file", let path = item["path"] as? String,
                      let url = item["download_url"] as? String, acceptable(path, size: (item["size"] as? Int) ?? 0) else { return nil }
                return (url, path)
            }
            if !files.isEmpty { return pick(files) }
        }
        // 2) jsDelivr's file index.
        let refs = ref.map { [$0] } ?? ["main", "master"]
        for candidate in refs {
            guard let object = await getJSON("https://data.jsdelivr.com/v1/package/gh/\(owner)/\(repo)@\(candidate)/flat") as? [String: Any],
                  let array = object["files"] as? [[String: Any]] else { continue }
            let prefix = dir.isEmpty ? "/" : "/" + dir.trimmingCharacters(in: CharacterSet(charactersIn: "/")) + "/"
            let files: [(url: String, path: String)] = array.compactMap { item in
                guard let path = item["name"] as? String, path.hasPrefix(prefix),
                      acceptable(path, size: (item["size"] as? Int) ?? 0) else { return nil }
                return ("https://raw.githubusercontent.com/\(owner)/\(repo)/\(candidate)\(path)", path)
            }
            if !files.isEmpty { return pick(files) }
        }
        return []
    }
}
