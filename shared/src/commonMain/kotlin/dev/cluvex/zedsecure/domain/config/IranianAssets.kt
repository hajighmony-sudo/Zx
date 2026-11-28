package dev.cluvex.zedsecure.domain.config

/**
 * Iranian DNS resolvers and well-known Iranian SNIs used exclusively by
 * Aether Smart 2. Other ZX engines and profiles remain untouched.
 */
object IranianAssets {

    /** Popular Iranian public DNS (IP + optional DoH). */
    data class DnsEntry(
        val name: String,
        val ips: List<String>,
        val doh: String? = null,
        val priority: Int = 50,
    )

    val IRANIAN_DNS = listOf(
        DnsEntry("Shecan", listOf("178.22.122.100", "185.51.200.2"), "https://free.shecan.ir/dns-query", 10),
        DnsEntry("Electro", listOf("10.202.10.10", "10.202.10.11"), null, 20),
        DnsEntry("Begzar", listOf("172.29.0.100", "172.29.2.100"), null, 30),
        DnsEntry("Pishgaman", listOf("185.55.225.25", "185.55.226.26"), null, 40),
        DnsEntry("Radar", listOf("10.202.10.202", "10.202.10.102"), null, 45),
        DnsEntry("403.online", listOf("10.202.10.10", "10.202.10.11"), null, 50),
    )

    /** Global resilient DNS used as secondary / tertiary. */
    val GLOBAL_DNS = listOf(
        DnsEntry("Cloudflare", listOf("1.1.1.1", "1.0.0.1"), "https://cloudflare-dns.com/dns-query", 60),
        DnsEntry("Google", listOf("8.8.8.8", "8.8.4.4"), "https://dns.google/dns-query", 70),
        DnsEntry("Quad9", listOf("9.9.9.9", "149.112.112.112"), "https://dns.quad9.net/dns-query", 80),
        DnsEntry("NextDNS", listOf("45.90.28.0", "45.90.30.0"), "https://dns.nextdns.io", 90),
    )

    /** Well-known Iranian hostnames suitable for SNI / domain-front cover. */
    val IRANIAN_SNIS = listOf(
        "www.digikala.com",
        "www.snapp.ir",
        "www.divar.ir",
        "www.aparat.com",
        "www.irancell.ir",
        "www.mci.ir",
        "www.telewebion.com",
        "www.sheypoor.com",
        "www.bankmellat.ir",
        "www.shaparak.ir",
        "www.digikala.com",
        "cdn.digikala.com",
        "www.tapsi.ir",
        "www.okala.com",
    )

    val GLOBAL_COVER_SNIS = listOf(
        "www.cloudflare.com",
        "cdnjs.cloudflare.com",
        "www.microsoft.com",
        "www.apple.com",
        "www.amazon.com",
        "ajax.googleapis.com",
    )

    /**
     * Returns ordered DNS list: Iranian first when country is IR or preferIran=true,
     * otherwise global first. Always keeps both families for fallback.
     */
    fun orderedDns(preferIran: Boolean): List<DnsEntry> =
        if (preferIran) IRANIAN_DNS + GLOBAL_DNS else GLOBAL_DNS + IRANIAN_DNS

    fun orderedSnis(preferIran: Boolean): List<String> =
        if (preferIran) (IRANIAN_SNIS + GLOBAL_COVER_SNIS).distinct()
        else (GLOBAL_COVER_SNIS + IRANIAN_SNIS).distinct()
}
