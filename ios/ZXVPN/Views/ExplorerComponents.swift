import SwiftUI
import UIKit

/// Small, cheap radar: a rotating sweep while scanning and one blip per reachable config.
struct ZXRadar: View {
    var running: Bool
    var blips: Int

    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 30.0, paused: !running)) { context in
            let seconds = context.date.timeIntervalSinceReferenceDate
            let angle = running ? (seconds.truncatingRemainder(dividingBy: 2.6) / 2.6) * 360 : -1
            let pulse = running ? seconds.truncatingRemainder(dividingBy: 1.8) / 1.8 : 0
            Canvas { gc, size in
                let radius = min(size.width, size.height) / 2
                let center = CGPoint(x: size.width / 2, y: size.height / 2)
                let ring = ZX.green.opacity(0.28)
                for factor in [1.0, 0.66, 0.33] {
                    let r = radius * factor - 1
                    gc.stroke(Path(ellipseIn: CGRect(x: center.x - r, y: center.y - r, width: r * 2, height: r * 2)),
                              with: .color(ring), lineWidth: 1.5)
                }
                if angle >= 0 {
                    var wedge = Path()
                    wedge.move(to: center)
                    wedge.addArc(center: center, radius: radius, startAngle: .degrees(angle - 70), endAngle: .degrees(angle), clockwise: false)
                    wedge.closeSubpath()
                    gc.fill(wedge, with: .color(ZX.green.opacity(0.32)))
                    var line = Path()
                    line.move(to: center)
                    line.addLine(to: CGPoint(x: center.x + radius * CGFloat(cos(angle * .pi / 180)),
                                             y: center.y + radius * CGFloat(sin(angle * .pi / 180))))
                    gc.stroke(line, with: .color(ZX.green), lineWidth: 2.5)
                    let pr = radius * CGFloat(pulse)
                    gc.stroke(Path(ellipseIn: CGRect(x: center.x - pr, y: center.y - pr, width: pr * 2, height: pr * 2)),
                              with: .color(ZX.green.opacity((1 - pulse) * 0.45)), lineWidth: 2)
                }
                for i in 0..<min(blips, 16) {
                    let theta = Double(i) * 137.5 * .pi / 180
                    let distance = radius * (0.22 + 0.68 * Double((i * 37) % 100) / 100)
                    let point = CGPoint(x: center.x + CGFloat(cos(theta) * distance), y: center.y + CGFloat(sin(theta) * distance))
                    gc.fill(Path(ellipseIn: CGRect(x: point.x - 3, y: point.y - 3, width: 6, height: 6)), with: .color(ZX.green))
                }
                gc.fill(Path(ellipseIn: CGRect(x: center.x - 4, y: center.y - 4, width: 8, height: 8)), with: .color(ZX.green))
            }
        }
        .frame(width: 64, height: 64)
    }
}

/// Contacts-style swipe: drag right → start action, drag left → end action. The row always springs back.
struct SwipeActionRow<Content: View>: View {
    var startLabel: LocalizedStringKey
    var endLabel: LocalizedStringKey
    var onRight: () -> Void
    var onLeft: () -> Void
    @ViewBuilder var content: () -> Content

    @Environment(\.layoutDirection) private var outerDirection
    @State private var offset: CGFloat = 0
    @State private var armed = false
    private let trigger: CGFloat = 84
    private let maxPull: CGFloat = 130

    var body: some View {
        ZStack {
            if offset != 0 {
                HStack {
                    if offset > 0 {
                        Text(startLabel).font(.system(size: abs(offset) >= trigger ? 16 : 14, weight: .heavy))
                        Spacer()
                    } else {
                        Spacer()
                        Text(endLabel).font(.system(size: abs(offset) >= trigger ? 16 : 14, weight: .heavy))
                    }
                }
                .foregroundColor(.white)
                .padding(.horizontal, 18)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(offset > 0 ? ZX.green.opacity(0.35) : ZX.cyan.opacity(0.35))
            }
            content()
                .environment(\.layoutDirection, outerDirection)
                .frame(maxWidth: .infinity)
                .background(ZX.bg)
                .offset(x: offset)
                .simultaneousGesture(
                    DragGesture(minimumDistance: 18)
                        .onChanged { value in
                            guard abs(value.translation.width) > abs(value.translation.height) else { return }
                            offset = max(-maxPull, min(maxPull, value.translation.width))
                            let nowArmed = abs(offset) >= trigger
                            if nowArmed && !armed { UIImpactFeedbackGenerator(style: .medium).impactOccurred() }
                            armed = nowArmed
                        }
                        .onEnded { _ in
                            let end = offset
                            armed = false
                            withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) { offset = 0 }
                            if end >= trigger { onRight() } else if end <= -trigger { onLeft() }
                        }
                )
        }
        .environment(\.layoutDirection, .leftToRight)   // gestures are absolute left/right
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

/// Full details of one config, including a live IP-database lookup.
struct ExplorerDetailView: View {
    @EnvironmentObject var explorer: ExplorerEngine
    @Environment(\.dismiss) private var dismiss
    let item: FoundConfig
    var onRename: (FoundConfig) -> Void

    @State private var geo: GeoDetails?
    @State private var loading = true

    private var current: FoundConfig { explorer.found.first { $0.link == item.link } ?? item }
    private var countryCode: String? { geo?.countryCode ?? current.country }

    var body: some View {
        ZStack {
            ZX.bg.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 12) {
                    HStack(spacing: 14) {
                        Text(Country.flag(countryCode)).font(.system(size: 46))
                        VStack(alignment: .leading, spacing: 3) {
                            Text(verbatim: explorer.displayName(current)).font(.title3.bold()).foregroundColor(.white).lineLimit(2)
                            Text(verbatim: headline).font(.subheadline).foregroundColor(ZX.dim)
                        }
                        Spacer()
                    }
                    if loading { ProgressView().tint(ZX.green) }

                    ZXPanel(tint: ZX.cyan) {
                        VStack(alignment: .leading, spacing: 4) {
                            ZXSectionTitle(text: "Location & network (by IP)")
                            line("IP", geo?.ip, copy: true)
                            line("Country", countryCode.map { Country.name($0) + " · " + ($0) })
                            line("Region", geo?.region)
                            line("City", geo?.city)
                            line("ISP", geo?.isp, copy: true)
                            line("ASN", geo?.asn, copy: true)
                            if let lat = geo?.latitude, let lon = geo?.longitude { line("Coordinates", "\(lat), \(lon)", copy: true) }
                        }
                    }

                    ZXPanel {
                        VStack(alignment: .leading, spacing: 6) {
                            ZXSectionTitle(text: "Performance")
                            if current.samples.isEmpty {
                                Text(current.status == 3 ? "UDP protocol: needs the tunnel core to test" : "No reachable sample yet")
                                    .font(.footnote).foregroundColor(ZX.dim)
                            } else {
                                let peak = max(current.samples.max() ?? 1, 1)
                                HStack(alignment: .bottom, spacing: 6) {
                                    ForEach(Array(current.samples.enumerated()), id: \.offset) { _, sample in
                                        RoundedRectangle(cornerRadius: 5).fill(ZX.green)
                                            .frame(width: 22, height: max(6, 46 * CGFloat(sample) / CGFloat(peak)))
                                    }
                                }.frame(height: 50, alignment: .bottom)
                                line("Latency", "\(current.ms) ms")
                                line("Samples", current.samples.map { "\($0) ms" }.joined(separator: ", "))
                            }
                            line("Status", statusText)
                        }
                    }

                    ZXPanel {
                        VStack(alignment: .leading, spacing: 4) {
                            ZXSectionTitle(text: "Config")
                            line("Name", explorer.displayName(current))
                            line("Protocol", current.proto)
                            line("Server", "\(current.host):\(current.port)", copy: true)
                            line("Source", current.source)
                            line("Link", String(current.link.prefix(120)) + (current.link.count > 120 ? "…" : ""), copyValue: current.link)
                        }
                    }

                    HStack(spacing: 8) {
                        action("✏️ Rename") { onRename(current); dismiss() }
                        action(explorer.isStarred(current) ? "★ Starred" : "☆ Star") { explorer.toggleStar(current) }
                        ShareLink(item: explorer.exportLink(current)) {
                            Text("📤 Export").font(.system(size: 14, weight: .bold)).foregroundColor(.white)
                                .frame(maxWidth: .infinity).padding(.vertical, 12)
                                .background(ZX.panel, in: Capsule()).overlay(Capsule().stroke(ZX.stroke))
                        }
                    }
                    Button { UIPasteboard.general.string = explorer.exportLink(current) } label: {
                        Text("Copy link").font(.system(size: 15, weight: .bold)).foregroundColor(.black)
                            .frame(maxWidth: .infinity).padding(.vertical, 13).background(ZX.green, in: Capsule())
                    }
                }
                .padding(20)
            }
        }
        .preferredColorScheme(.dark)
        .task {
            loading = true
            geo = await GeoLookup.shared.details(of: item.host)
            loading = false
        }
    }

    private var headline: String {
        if let code = countryCode {
            return Country.name(code) + " (" + code + ")" + (geo?.city.map { " · " + $0 } ?? "")
        }
        return loading ? "…" : "—"
    }

    private var statusText: String {
        switch current.status {
        case 1: return "✓ TCP reachable"
        case 2: return "✗ Failed"
        case 3: return "UDP"
        default: return "…"
        }
    }

    private func action(_ title: LocalizedStringKey, _ run: @escaping () -> Void) -> some View {
        Button(action: run) {
            Text(title).font(.system(size: 14, weight: .bold)).foregroundColor(.white)
                .frame(maxWidth: .infinity).padding(.vertical, 12)
                .background(ZX.panel, in: Capsule()).overlay(Capsule().stroke(ZX.stroke))
        }
    }

    @ViewBuilder
    private func line(_ label: LocalizedStringKey, _ value: String?, copy: Bool = false, copyValue: String? = nil) -> some View {
        if let value, !value.isEmpty {
            HStack(alignment: .top, spacing: 10) {
                Text(label).font(.caption).foregroundColor(ZX.dim).frame(width: 92, alignment: .leading)
                Text(verbatim: value).font(.subheadline).foregroundColor(.white)
                Spacer(minLength: 0)
            }
            .contentShape(Rectangle())
            .onTapGesture { if copy || copyValue != nil { UIPasteboard.general.string = copyValue ?? value } }
        }
    }
}
