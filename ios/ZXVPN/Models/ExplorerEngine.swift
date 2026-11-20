import Foundation
import Network

struct FoundConfig: Identifiable, Codable, Equatable {
    /// 0 testing · 1 reachable · 2 failed · 3 not testable from iOS without the tunnel core (UDP protocols)
    var link: String
    var name: String
    var proto: String
    var host: String
    var port: Int
    var source: String
    var status: Int
    var ms: Int
    var samples: [Int]
    var country: String?
    var id: String { link }
}

struct ExplorerSource: Identifiable, Equatable {
    var id: String
    var name: String
    var url: String
    var builtIn: Bool
}

/// ZX Explorer for iOS: gathers links from public feeds, shows each one the moment it is parsed, then
/// measures TCP connect latency and resolves the country by IP. (The real HTTPS-through-core test the
/// Android app does needs the Xray/Aether core inside the tunnel extension.)
@MainActor
final class ExplorerEngine: ObservableObject {
    static let candidateLimit = 1_500
    static let concurrency = 24
    static let maxCustomSources = 40

    static let builtIn: [ExplorerSource] = [
        ExplorerSource(id: "mahdibland", name: "V2RayAggregator", url: "https://raw.githubusercontent.com/mahdibland/V2RayAggregator/master/sub/airport_merge_base64.txt", builtIn: true),
        ExplorerSource(id: "barry-far", name: "Barry Far V2ray Config", url: "https://raw.githubusercontent.com/barry-far/V2ray-Config/main/Base64/Sub1_base64.txt", builtIn: true),
        ExplorerSource(id: "radikal", name: "Free-v2ray-Configs", url: "https://raw.githubusercontent.com/0xRadikal/Free-v2ray-Configs/main/verified/configs.txt", builtIn: true),
        ExplorerSource(id: "delta-kronecker", name: "Delta-Kronecker", url: "https://raw.githubusercontent.com/Delta-Kronecker/V2ray-Config/main/config/all_configs.txt", builtIn: true),
        ExplorerSource(id: "v2go", name: "Danialsamadi v2go", url: "https://raw.githubusercontent.com/Danialsamadi/v2go/main/AllConfigsSub.txt", builtIn: true),
        ExplorerSource(id: "morpheusadam", name: "morpheusadam", url: "https://raw.githubusercontent.com/morpheusadam/v2ray-config/main/subs/all.txt", builtIn: true),
        ExplorerSource(id: "collector", name: "V2ray-Collector", url: "https://raw.githubusercontent.com/MohammadBahemmat/V2ray-Collector/main/all_servers.txt", builtIn: true),
        ExplorerSource(id: "configpilot", name: "V2Root ConfigPilot", url: "https://raw.githubusercontent.com/V2RayRoot/V2Root-ConfigPilot/main/output/BestConfigs.txt", builtIn: true),
    ]

    @Published private(set) var found: [FoundConfig] = []
    @Published private(set) var running = false
    @Published private(set) var phase = ""
    @Published private(set) var done = 0
    @Published private(set) var total = 0
    @Published var customSources: [String] { didSet { save() } }
    @Published var starred: [FoundConfig] { didSet { save() } }
    @Published var renames: [String: String] { didSet { save() } }

    private var task: Task<Void, Never>?
    private var testingFinished = false
    private var geoTried = Set<String>()
    private let defaults = UserDefaults.standard

    init() {
        customSources = (defaults.array(forKey: "zx.explorer.sources") as? [String]) ?? []
        renames = (defaults.dictionary(forKey: "zx.explorer.renames") as? [String: String]) ?? [:]
        if let data = defaults.data(forKey: "zx.explorer.starred"),
           let list = try? JSONDecoder().decode([FoundConfig].self, from: data) { starred = list } else { starred = [] }
        if let data = defaults.data(forKey: "zx.explorer.found"),
           let list = try? JSONDecoder().decode([FoundConfig].self, from: data) { found = list }
    }

    var sources: [ExplorerSource] {
        Self.builtIn + customSources.map { ExplorerSource(id: "custom-\($0.hashValue)", name: Self.shortName($0), url: $0, builtIn: false) }
    }

    var aliveCount: Int { found.filter { $0.status == 1 }.count }

    func displayName(_ item: FoundConfig) -> String {
        if let custom = renames[item.link], !custom.isEmpty { return custom }
        return item.name.isEmpty ? "\(item.proto.uppercased()) · \(item.host)" : item.name
    }

    func exportLink(_ item: FoundConfig) -> String { LinkRename.apply(item.link, name: displayName(item)) }

    func rename(_ item: FoundConfig, to name: String) {
        let clean = name.trimmingCharacters(in: .whitespacesAndNewlines)
        if clean.isEmpty { renames.removeValue(forKey: item.link) } else { renames[item.link] = String(clean.prefix(80)) }
    }

    func isStarred(_ item: FoundConfig) -> Bool { starred.contains { $0.link == item.link } }

    func toggleStar(_ item: FoundConfig) {
        if isStarred(item) { starred.removeAll { $0.link == item.link } } else { starred.append(item) }
    }

    func addCustomSource(_ url: String) -> Bool {
        let clean = url.trimmingCharacters(in: .whitespacesAndNewlines)
        guard clean.hasPrefix("https://"), !customSources.contains(clean), customSources.count < Self.maxCustomSources else { return false }
        customSources.append(clean)
        return true
    }

    func removeCustomSource(_ url: String) { customSources.removeAll { $0 == url } }

    /// Link Hunter: resolves any GitHub / raw / gist / https link, verifies the feeds, adds them and scans.
    func hunt(_ input: String) async -> (ok: Bool, message: String) {
        let result = await LinkHunter.hunt(input)
        guard !result.feeds.isEmpty else { return (false, result.note) }
        var added = 0
        for feed in result.feeds where addCustomSource(feed.url) { added += 1 }
        if added > 0 && !running { start() }
        return (true, result.note + (added > 0 ? " · \(added) new sources added, scan started" : " · all were already added"))
    }

    func start() {
        guard !running else { return }
        running = true
        task = Task { await run() }
    }

    func stop() {
        task?.cancel()
    }

    // MARK: - Cycle

    private func run() async {
        defer { running = false; phase = ""; save() }
        testingFinished = false
        geoTried = []
        phase = "fetching"
        done = 0
        total = 0

        let feeds = sources
        var links: [(link: String, source: String)] = []
        await withTaskGroup(of: (String, [String]).self) { group in
            for source in feeds {
                group.addTask {
                    guard let text = await LinkHunter.fetchText(source.url) else { return (source.name, []) }
                    return (source.name, ConfigLink.extractLinks(text))
                }
            }
            for await (name, list) in group {
                links.append(contentsOf: list.map { ($0, name) })
                // Show whatever has been parsed so far while slower feeds are still downloading.
                publishParsed(links)
            }
        }
        if Task.isCancelled { return }
        publishParsed(links)

        phase = "testing"
        let pending = found.filter { $0.status == 0 }
        total = pending.count
        let geoTask = Task { await self.geoPass() }
        var index = 0
        await withTaskGroup(of: (String, [Int]).self) { group in
            while index < min(Self.concurrency, pending.count) {
                let item = pending[index]; index += 1
                group.addTask { (item.link, await Self.measure(host: item.host, port: item.port)) }
            }
            while let (link, samples) = await group.next() {
                apply(link: link, samples: samples)
                if Task.isCancelled { group.cancelAll(); continue }
                if index < pending.count {
                    let item = pending[index]; index += 1
                    group.addTask { (item.link, await Self.measure(host: item.host, port: item.port)) }
                }
            }
        }
        testingFinished = true
        phase = "geo"
        await geoTask.value
    }

    private func publishParsed(_ links: [(link: String, source: String)]) {
        var existing = Dictionary(uniqueKeysWithValues: found.map { ($0.link, $0) })
        var order = found.map { $0.link }
        var seenEndpoints = Set(found.map { "\($0.proto)|\($0.host)|\($0.port)" })
        var added = 0
        for (link, source) in links.shuffled() {
            if existing.count >= Self.candidateLimit { break }
            guard existing[link] == nil, let parsed = ConfigLink.parse(link),
                  seenEndpoints.insert("\(parsed.proto)|\(parsed.host)|\(parsed.port)").inserted else { continue }
            existing[link] = FoundConfig(link: link, name: parsed.name, proto: parsed.proto, host: parsed.host, port: parsed.port,
                                         source: source, status: parsed.usesTCP ? 0 : 3, ms: 0, samples: [], country: nil)
            order.append(link)
            added += 1
        }
        if added > 0 { found = order.compactMap { existing[$0] } }
    }

    private func apply(link: String, samples: [Int]) {
        done += 1
        guard let i = found.firstIndex(where: { $0.link == link }) else { return }
        if samples.isEmpty {
            found[i].status = 2
        } else {
            let sorted = samples.sorted()
            found[i].status = 1
            found[i].samples = samples
            found[i].ms = sorted[sorted.count / 2]
        }
    }

    private func geoPass() async {
        while !Task.isCancelled {
            let pending = found.filter { $0.status == 1 && $0.country == nil && !geoTried.contains($0.host) }
            if pending.isEmpty {
                if testingFinished { break }
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                continue
            }
            let batch = Array(pending.prefix(6))
            for item in batch { geoTried.insert(item.host) }
            await withTaskGroup(of: (String, String?).self) { group in
                for item in batch {
                    group.addTask { (item.host, await GeoLookup.shared.details(of: item.host)?.countryCode) }
                }
                for await (host, code) in group {
                    guard let code else { continue }
                    for i in found.indices where found[i].host == host { found[i].country = code }
                }
            }
        }
    }

    // MARK: - Measuring

    /// Two TCP connects; the successful round-trips in milliseconds (empty when the server never answered).
    nonisolated static func measure(host: String, port: Int) async -> [Int] {
        var out: [Int] = []
        for _ in 0..<2 {
            if let ms = await tcpLatency(host: host, port: port) { out.append(ms) }
        }
        return out
    }

    nonisolated static func tcpLatency(host: String, port: Int, timeout: TimeInterval = 4) async -> Int? {
        guard port > 0, port < 65_536, let endpointPort = NWEndpoint.Port(rawValue: UInt16(port)) else { return nil }
        return await withCheckedContinuation { continuation in
            let connection = NWConnection(host: NWEndpoint.Host(host), port: endpointPort, using: .tcp)
            let queue = DispatchQueue(label: "zx.explorer.tcp")
            let start = DispatchTime.now().uptimeNanoseconds
            var finished = false
            func finish(_ value: Int?) {
                if finished { return }
                finished = true
                connection.cancel()
                continuation.resume(returning: value)
            }
            connection.stateUpdateHandler = { state in
                switch state {
                case .ready:
                    finish(Int((DispatchTime.now().uptimeNanoseconds - start) / 1_000_000))
                case .failed, .cancelled:
                    finish(nil)
                default:
                    break
                }
            }
            connection.start(queue: queue)
            queue.asyncAfter(deadline: .now() + timeout) { finish(nil) }
        }
    }

    private static func shortName(_ url: String) -> String {
        let rest = url.replacingOccurrences(of: "https://", with: "")
        let parts = rest.split(separator: "/").map(String.init)
        if parts.first == "raw.githubusercontent.com", parts.count >= 5 { return "\(parts[1])/\(parts[2]) · \(parts.last ?? "")" }
        return String(parts.first ?? rest).prefix(40).description
    }

    private func save() {
        defaults.set(customSources, forKey: "zx.explorer.sources")
        defaults.set(renames, forKey: "zx.explorer.renames")
        if let data = try? JSONEncoder().encode(starred) { defaults.set(data, forKey: "zx.explorer.starred") }
        let alive = found.filter { $0.status == 1 }.sorted { $0.ms < $1.ms }.prefix(300)
        if let data = try? JSONEncoder().encode(Array(alive)) { defaults.set(data, forKey: "zx.explorer.found") }
    }
}
