package dev.cluvex.zedsecure.core.ghost

/**
 * Domain policy for GHOST content protection.
 *
 * The built-in list supplements the Family DNS resolver. The engine applies these rules
 * before user routing rules, so a configured direct/proxy ruleset cannot accidentally
 * shadow the protection policy.
 */
object AdultBlocklist {
    private val domains = listOf(
        "pornhub.com", "pornhub.org", "phncdn.com", "pornhubpremium.com", "pornhubpremium.net",
        "xvideos.com", "xvideos-cdn.com", "xvideos2.com", "xvideos3.com",
        "xnxx.com", "xnxx-cdn.com", "xnxx.tv", "xnxx.health",
        "xhamster.com", "xhcdn.com", "xhamsterlive.com", "xhamster.desi", "xhamster2.com",
        "redtube.com", "youporn.com", "tube8.com", "spankbang.com", "spankwire.com",
        "eporner.com", "porntrex.com", "beeg.com", "motherless.com", "thumbzilla.com",
        "tnaflix.com", "txxx.com", "hclips.com", "fapello.com", "sex.com", "fuq.com",
        "porn.com", "redgifs.com", "4tube.com", "empflix.com", "drtuber.com",
        "slutload.com", "keezmovies.com", "vporn.com", "youjizz.com", "javhd.com",
        "javmost.com", "jav.guru", "javbus.com", "adulttime.com", "teamskeet.com",
        "mofos.com", "digitalplayground.com", "wicked.com", "vixen.com", "deeper.com",
        "blacked.com", "tushy.com", "bellesa.co", "manyvids.com", "pornone.com",
        "pornhd.com", "pornhat.com", "porndig.com", "porn300.com", "sxyprn.com",
        "daftsex.com", "hqporner.com", "nuvid.com", "sunporno.com", "extremetube.com",
        "gotporn.com", "analdin.com", "pornpics.com", "imagefap.com", "luscious.net",
        "hentai2read.com", "hitomi.la", "e-hentai.org", "nhentai.net", "hanime.tv",
        "rule34.xxx", "gelbooru.com", "danbooru.donmai.us", "rule34.paheal.net",
        "e621.net", "chaturbate.com", "stripchat.com", "bongacams.com", "livejasmin.com",
        "cam4.com", "camsoda.com", "myfreecams.com", "erome.com", "ixxx.com",
        "fansly.com", "clips4sale.com", "iwara.tv", "missav.com", "supjav.com",
        "jable.tv", "flirt4free.com", "imlive.com", "streamate.com", "xlovecam.com",
        "jerkmate.com", "adultfriendfinder.com", "nubiles.net", "fakehub.com",
        "evilangel.com", "hotmovies.com", "adultempire.com", "naughtyamerica.com",
        "realitykings.com", "bangbros.com", "javlibrary.com", "javdatabase.com",
    )

    /** Public encrypted-DNS hostnames commonly used to bypass device DNS policies. */
    private val encryptedDnsHosts = listOf(
        "dns.google", "dns.google.com", "cloudflare-dns.com", "mozilla.cloudflare-dns.com",
        "chrome.cloudflare-dns.com", "one.one.one.one", "dns.quad9.net", "doh.opendns.com",
        "dns.nextdns.io", "doh.dns.sb", "dns.adguard.com", "dns.adguard-dns.com",
        "dns0.eu", "security.cloudflare-dns.com", "family.cloudflare-dns.com",
        "dns.mullvad.net", "base.dns.mullvad.net", "adblock.dns.mullvad.net",
        "dns.controld.com", "freedns.controld.com", "doh.li", "dnsforge.de",
    )

    /**
     * Xray's geosite database adds a maintained category when the matching geo assets
     * are installed. Explicit domains remain as a fallback when geo assets are absent.
     */
    val rules: List<String> = buildList {
        add("geosite:category-porn")
        domains.forEach { add("domain:$it") }
        encryptedDnsHosts.forEach { add("domain:$it") }
    }.distinct()

    /**
     * Accepts a hostname or a full URL (scheme, path, query and port are stripped).
     * A domain rule covers the apex and its subdomains. Invalid hosts and IP literals
     * are ignored instead of creating a misleading rule.
     */
    fun userRules(raw: String): List<String> = normalizeHosts(raw)
        .map { "domain:$it" }

    fun normalizeHosts(raw: String): List<String> = raw
        .lineSequence()
        .flatMap { it.split(',', ';', ' ').asSequence() }
        .mapNotNull(::normalizeHost)
        .distinct()
        .take(500)
        .toList()

    private fun normalizeHost(value: String): String? {
        var host = value.trim().lowercase()
        if (host.isEmpty() || host.startsWith("#")) return null
        if (host.startsWith("//")) host = host.removePrefix("//")
        if ("://" in host) host = host.substringAfter("://")
        host = host.substringBefore('#').substringBefore('?').substringBefore('/')
        host = host.substringAfterLast('@').trim().trimEnd('.')
        if (host.startsWith("[") || host.contains(':')) {
            if (host.count { it == ':' } == 1) host = host.substringBefore(':') else return null
        }
        if (host.startsWith("www.")) host = host.removePrefix("www.")
        if (host.length !in 4..253 || '.' !in host || host == "localhost") return null
        val labels = host.split('.')
        if (labels.any { label ->
                label.isEmpty() || label.length > 63 ||
                    !label.first().isLetterOrDigit() || !label.last().isLetterOrDigit() ||
                    label.any { !(it in 'a'..'z' || it in '0'..'9' || it == '-') }
            }
        ) return null
        val tld = labels.last()
        if (tld.length < 2 || tld.all(Char::isDigit)) return null
        if (labels.all { label -> label.all(Char::isDigit) }) return null
        return host
    }
}
