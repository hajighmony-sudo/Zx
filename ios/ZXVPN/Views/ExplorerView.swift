import SwiftUI
import UIKit

private enum ExplorerTab: String, CaseIterable, Identifiable {
    case found, countries, stars, sources
    var id: String { rawValue }
    var title: LocalizedStringKey {
        switch self {
        case .found: return "Discovered"
        case .countries: return "Countries"
        case .stars: return "Starred"
        case .sources: return "Hunter & sources"
        }
    }
}

struct ExplorerView: View {
    @EnvironmentObject var explorer: ExplorerEngine
    @State private var tab: ExplorerTab = .found
    @State private var query = ""
    @State private var expandedCountry: String?
    @State private var detail: FoundConfig?
    @State private var renameTarget: FoundConfig?
    @State private var renameText = ""
    @State private var huntInput = ""
    @State private var huntMessage: String?
    @State private var hunting = false
    @State private var toast: String?

    private func matches(_ item: FoundConfig) -> Bool {
        let q = query.trimmingCharacters(in: .whitespaces).lowercased()
        if q.isEmpty { return true }
        return explorer.displayName(item).lowercased().contains(q) || item.host.lowercased().contains(q) ||
            item.proto.lowercased().contains(q) || Country.name(item.country).lowercased().contains(q) ||
            (item.country?.lowercased() ?? "").contains(q) || item.source.lowercased().contains(q)
    }

    private var discovered: [FoundConfig] {
        explorer.found.filter(matches).sorted { a, b in
            func rank(_ s: Int) -> Int { s == 1 ? 0 : (s == 0 ? 1 : (s == 3 ? 2 : 3)) }
            if rank(a.status) != rank(b.status) { return rank(a.status) < rank(b.status) }
            return a.status == 1 ? a.ms < b.ms : false
        }
    }

    private var countryGroups: [CountryGroup] {
        let alive = explorer.found.filter { $0.status == 1 && matches($0) }
        let byCode = Dictionary(grouping: alive.filter { $0.country != nil }, by: { $0.country! })
        let priority = Country.priority.map { CountryGroup(code: $0, items: (byCode[$0] ?? []).sorted { $0.ms < $1.ms }) }
        let others = byCode.filter { !Country.priority.contains($0.key) }
            .map { CountryGroup(code: $0.key, items: $0.value.sorted { $0.ms < $1.ms }) }
            .sorted { $0.items.count > $1.items.count }
        return priority + others
    }

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 10, pinnedViews: [.sectionHeaders]) {
                header
                Section {
                    switch tab {
                    case .found: foundList
                    case .countries: countriesList
                    case .stars: starsList
                    case .sources: sourcesList
                    }
                } header: {
                    tabBar
                }
            }
            .padding(.horizontal, 20)
        }
        .overlay(alignment: .bottom) {
            if let toast {
                Text(toast).font(.footnote.bold()).padding(.horizontal, 14).padding(.vertical, 8)
                    .background(.ultraThinMaterial, in: Capsule()).padding(.bottom, 8)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .sheet(item: $detail) { item in
            ExplorerDetailView(item: item) { target in
                renameText = explorer.displayName(target)
                renameTarget = target
            }
            .environmentObject(explorer)
        }
        .alert("Rename config", isPresented: Binding(get: { renameTarget != nil }, set: { if !$0 { renameTarget = nil } })) {
            TextField("Name", text: $renameText)
            Button("Save") { if let target = renameTarget { explorer.rename(target, to: renameText); flash("Name saved") } }
            Button("Original name") { if let target = renameTarget { explorer.rename(target, to: "") } }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("The new name is also used when you export the link.")
        }
    }

    // MARK: Header

    private var header: some View {
        VStack(spacing: 10) {
            HStack(spacing: 12) {
                ZXRadar(running: explorer.running, blips: explorer.aliveCount)
                VStack(alignment: .leading, spacing: 3) {
                    Text("ZX Explorer").font(.system(size: 26, weight: .black, design: .rounded)).foregroundColor(.white)
                    Text(verbatim: statusLine).font(.caption).foregroundColor(ZX.dim)
                }
                Spacer()
            }
            .padding(.top, 8)

            HStack(spacing: 8) {
                metric("Found", "\(explorer.found.count)")
                metric("Reachable", "\(explorer.aliveCount)")
                metric("Countries", "\(Set(explorer.found.compactMap { $0.country }).count)")
            }

            Button {
                if explorer.running { explorer.stop() } else { explorer.start() }
            } label: {
                Text(explorer.running ? "Stop scan" : "Start scan")
                    .font(.system(size: 16, weight: .bold)).foregroundColor(.black)
                    .frame(maxWidth: .infinity).padding(.vertical, 13)
                    .background(explorer.running ? ZX.red : ZX.green, in: Capsule())
            }
            if explorer.running && explorer.total > 0 {
                ProgressView(value: Double(explorer.done), total: Double(max(explorer.total, 1))).tint(ZX.green)
            }
        }
    }

    private var statusLine: String {
        switch explorer.phase {
        case "fetching": return "Downloading feeds…"
        case "testing": return "Testing \(explorer.done)/\(explorer.total)"
        case "geo": return "Resolving countries…"
        default: return explorer.found.isEmpty ? "Tap start to discover configs" : "Idle"
        }
    }

    private func metric(_ title: LocalizedStringKey, _ value: String) -> some View {
        ZXPanel {
            VStack(alignment: .leading, spacing: 2) {
                Text(verbatim: value).font(.system(size: 22, weight: .heavy, design: .rounded)).foregroundColor(.white)
                Text(title).font(.caption2).foregroundColor(ZX.dim)
            }
        }
    }

    private var tabBar: some View {
        VStack(spacing: 8) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(ExplorerTab.allCases) { item in
                        Button { withAnimation(.easeOut(duration: 0.2)) { tab = item } } label: {
                            Text(item.title).font(.system(size: 13, weight: .bold))
                                .foregroundColor(tab == item ? .black : .white)
                                .padding(.horizontal, 14).padding(.vertical, 8)
                                .background(tab == item ? ZX.green : ZX.panel, in: Capsule())
                        }
                    }
                }
            }
            if tab != .sources {
                TextField("Search or paste a GitHub / raw link", text: $query)
                    .textInputAutocapitalization(.never).autocorrectionDisabled()
                    .padding(10).background(ZX.panel, in: RoundedRectangle(cornerRadius: 12))
                    .foregroundColor(.white)
                if LinkHunter.looksLikeLink(query) {
                    Button { startHunt(query) } label: {
                        Text(hunting ? "Hunting…" : "🎯 Hunt this link and scan")
                            .font(.system(size: 14, weight: .bold)).foregroundColor(.black)
                            .frame(maxWidth: .infinity).padding(.vertical, 10)
                            .background(ZX.cyan, in: Capsule())
                    }.disabled(hunting)
                }
            }
        }
        .padding(.vertical, 8)
        .background(ZX.bg.opacity(0.92))
    }

    // MARK: Lists

    @ViewBuilder private var foundList: some View {
        if discovered.isEmpty {
            emptyNote(explorer.running ? "Feeds are downloading; every config appears here the moment it is parsed."
                                       : "Nothing discovered yet. Tap start scan.")
        } else {
            ForEach(Array(discovered.enumerated()), id: \.element.id) { index, item in
                swipeRow(item) { row(index + 1, item) }
            }
        }
    }

    @ViewBuilder private var starsList: some View {
        let list = explorer.starred.filter(matches)
        if list.isEmpty {
            emptyNote("Open a config and tap the star to keep it here.")
        } else {
            ForEach(Array(list.enumerated()), id: \.element.id) { index, item in
                swipeRow(item) { row(index + 1, item) }
            }
        }
    }

    @ViewBuilder private var countriesList: some View {
        let groups = countryGroups
        let unknown = explorer.found.filter { $0.status == 1 && $0.country == nil }.count
        Text(verbatim: "\(groups.filter { !$0.items.isEmpty }.count) · " + (unknown > 0 ? "… \(unknown)" : ""))
            .font(.caption).foregroundColor(ZX.dim).frame(maxWidth: .infinity, alignment: .leading)
        ForEach(groups) { group in
            let open = expandedCountry == group.code
            Button {
                withAnimation(.easeOut(duration: 0.2)) { expandedCountry = open ? nil : group.code }
            } label: {
                ZXPanel(tint: open ? ZX.green : .white) {
                    HStack(spacing: 12) {
                        Text(Country.flag(group.code)).font(.system(size: 28))
                        VStack(alignment: .leading, spacing: 2) {
                            Text(verbatim: Country.name(group.code)).font(.headline).foregroundColor(.white)
                            Text(verbatim: group.items.isEmpty ? "—" : "\(group.items.count) · \(group.items.first?.ms ?? 0) ms")
                                .font(.caption).foregroundColor(ZX.dim)
                        }
                        Spacer()
                        Image(systemName: open ? "chevron.up" : "chevron.down").foregroundColor(ZX.dim)
                    }
                }
            }
            .buttonStyle(.plain)
            .disabled(group.items.isEmpty)
            if open {
                ForEach(Array(group.items.prefix(40).enumerated()), id: \.element.id) { index, item in
                    swipeRow(item) { row(index + 1, item) }
                }
            }
        }
    }

    @ViewBuilder private var sourcesList: some View {
        ZXPanel(tint: ZX.cyan) {
            VStack(alignment: .leading, spacing: 8) {
                Text("🎯 ZX Link Hunter").font(.headline).foregroundColor(.white)
                Text("Paste any link: a GitHub repository, a folder, a file page (blob), a raw link, a Gist or any https feed. The hunter finds config-like files, verifies them and starts a scan.")
                    .font(.caption).foregroundColor(ZX.dim)
                TextField("github.com/… or raw.githubusercontent.com/… or https://…", text: $huntInput)
                    .textInputAutocapitalization(.never).autocorrectionDisabled()
                    .padding(10).background(ZX.panel, in: RoundedRectangle(cornerRadius: 12)).foregroundColor(.white)
                Button { startHunt(huntInput) } label: {
                    Text(hunting ? "Hunting…" : "Hunt and add")
                        .font(.system(size: 15, weight: .bold)).foregroundColor(.black)
                        .frame(maxWidth: .infinity).padding(.vertical, 11).background(ZX.green, in: Capsule())
                }.disabled(hunting || huntInput.trimmingCharacters(in: .whitespaces).isEmpty)
                if let huntMessage { Text(verbatim: huntMessage).font(.caption.bold()).foregroundColor(ZX.cyan) }
            }
        }
        ForEach(explorer.sources) { source in
            ZXPanel {
                HStack {
                    VStack(alignment: .leading, spacing: 3) {
                        Text(verbatim: source.name).font(.subheadline.bold()).foregroundColor(.white)
                        Text(verbatim: source.url).font(.caption2).foregroundColor(ZX.dim).lineLimit(1)
                    }
                    Spacer()
                    if !source.builtIn {
                        Button(role: .destructive) { explorer.removeCustomSource(source.url) } label: { Image(systemName: "trash") }
                    }
                }
            }
        }
    }

    private func emptyNote(_ text: LocalizedStringKey) -> some View {
        Text(text).font(.footnote).foregroundColor(ZX.dim).frame(maxWidth: .infinity, alignment: .leading).padding(.vertical, 18)
    }

    private func row(_ index: Int, _ item: FoundConfig) -> some View {
        Button { detail = item } label: {
            HStack(spacing: 10) {
                Text(verbatim: String(format: "%02d", index)).font(.caption.monospaced()).foregroundColor(ZX.dim)
                VStack(alignment: .leading, spacing: 2) {
                    Text(verbatim: Country.flag(item.country) + "  " + explorer.displayName(item))
                        .font(.subheadline.bold()).foregroundColor(.white).lineLimit(1)
                    Text(verbatim: "\(item.proto) · \(item.host):\(item.port) · \(item.source)")
                        .font(.caption2).foregroundColor(ZX.dim).lineLimit(1)
                }
                Spacer()
                switch item.status {
                case 1: Text(verbatim: "\(item.ms) ms").font(.subheadline.bold()).foregroundColor(ZX.green)
                case 2: Text("Failed").font(.caption).foregroundColor(ZX.red)
                case 3: Text("UDP").font(.caption).foregroundColor(ZX.dim)
                default: ProgressView().scaleEffect(0.7)
                }
            }
            .padding(.vertical, 8)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private func swipeRow<Content: View>(_ item: FoundConfig, @ViewBuilder content: @escaping () -> Content) -> some View {
        SwipeActionRow(
            startLabel: "✏️ Rename", endLabel: "📤 Export",
            onRight: { renameText = explorer.displayName(item); renameTarget = item },
            onLeft: { export(item) },
            content: content
        )
    }

    private func export(_ item: FoundConfig) {
        UIPasteboard.general.string = explorer.exportLink(item)
        flash("Link exported and copied")
        Task { await shareLink(explorer.exportLink(item)) }
    }

    @MainActor private func shareLink(_ text: String) async {
        guard let scene = UIApplication.shared.connectedScenes.compactMap({ $0 as? UIWindowScene }).first,
              let root = scene.keyWindow?.rootViewController else { return }
        var top = root
        while let presented = top.presentedViewController { top = presented }
        let controller = UIActivityViewController(activityItems: [text], applicationActivities: nil)
        controller.popoverPresentationController?.sourceView = top.view
        top.present(controller, animated: true)
    }

    private func startHunt(_ input: String) {
        guard !hunting else { return }
        hunting = true
        huntMessage = nil
        Task {
            let result = await explorer.hunt(input)
            huntMessage = result.message
            hunting = false
            if result.ok { query = ""; huntInput = ""; tab = .found }
            flash(result.message)
        }
    }

    private func flash(_ text: String) {
        withAnimation { toast = NSLocalizedString(text, comment: "") }
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.2) { withAnimation { toast = nil } }
    }
}

private struct CountryGroup: Identifiable {
    var code: String
    var items: [FoundConfig]
    var id: String { code }
}
