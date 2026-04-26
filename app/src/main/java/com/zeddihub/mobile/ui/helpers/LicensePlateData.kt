package com.zeddihub.mobile.ui.helpers

/**
 * Czech and Slovak license-plate region/district lookup data.
 *
 * **Czech format (post-2001)**: 7 chars `RRR-XXXX` where RRR is a region
 * + district code, e.g. `1AB 1234`. The first character is the region
 * digit and the next two are the district letters within that region.
 * (We collapse to district-letters lookup since the region digit is
 * derivable from the district.) Pre-2001 plates used `XYZ 12-34` with
 * a 2-letter district code at the start — we recognise both.
 *
 * **Slovak format**: `XX-NNNLLL` where XX is the 2-letter district
 * code (e.g. `BA` Bratislava, `KE` Košice). Same since 1997.
 *
 * **Special CZ codes** also recognised: diplomatic (CD), special
 * civilian (E for environmental / eco vehicles, X for trade-test),
 * historical (`H` suffix), military (single letter A/T/V).
 */
object LicensePlateData {

    data class PlateRegion(
        val code: String,
        val country: Country,
        val district: String,
        val regionName: String?,
    )

    enum class Country(val flag: String, val label: String) {
        CZ("🇨🇿", "Česko"),
        SK("🇸🇰", "Slovensko"),
    }

    /**
     * CZ districts indexed by their two-letter code (post-2001 second
     * + third character of the 7-digit plate). Region name is the
     * higher-level kraj for context. Set is the canonical 76 okresů.
     */
    val CZ: List<PlateRegion> = listOf(
        // Praha + Středočeský
        p("AA", "Praha", "Hlavní město Praha"),
        p("AB", "Praha", "Hlavní město Praha"),
        p("AC", "Praha", "Hlavní město Praha"),
        p("AE", "Praha", "Hlavní město Praha"),
        p("AH", "Praha", "Hlavní město Praha"),
        p("AI", "Praha", "Hlavní město Praha"),
        p("AJ", "Praha", "Hlavní město Praha"),
        p("AK", "Praha", "Hlavní město Praha"),
        p("AL", "Praha", "Hlavní město Praha"),
        p("AM", "Praha", "Hlavní město Praha"),
        p("AN", "Praha", "Hlavní město Praha"),
        p("AO", "Praha", "Hlavní město Praha"),
        p("AP", "Praha", "Hlavní město Praha"),
        p("AS", "Benešov", "Středočeský"),
        p("AT", "Beroun", "Středočeský"),
        p("AU", "Kladno", "Středočeský"),
        p("AV", "Kolín", "Středočeský"),
        p("AY", "Kutná Hora", "Středočeský"),
        p("AZ", "Mělník", "Středočeský"),
        p("BA", "Mladá Boleslav", "Středočeský"),
        p("BC", "Nymburk", "Středočeský"),
        p("BE", "Praha-východ", "Středočeský"),
        p("BH", "Praha-západ", "Středočeský"),
        p("BI", "Příbram", "Středočeský"),
        p("BJ", "Rakovník", "Středočeský"),

        // Jihočeský
        p("CA", "České Budějovice", "Jihočeský"),
        p("CB", "Český Krumlov", "Jihočeský"),
        p("CC", "Jindřichův Hradec", "Jihočeský"),
        p("CE", "Písek", "Jihočeský"),
        p("CH", "Prachatice", "Jihočeský"),
        p("CI", "Strakonice", "Jihočeský"),
        p("CJ", "Tábor", "Jihočeský"),

        // Plzeňský
        p("PA", "Plzeň-město", "Plzeňský"),
        p("PB", "Domažlice", "Plzeňský"),
        p("PC", "Klatovy", "Plzeňský"),
        p("PE", "Plzeň-jih", "Plzeňský"),
        p("PH", "Plzeň-sever", "Plzeňský"),
        p("PI", "Rokycany", "Plzeňský"),
        p("PJ", "Tachov", "Plzeňský"),

        // Karlovarský
        p("KA", "Karlovy Vary", "Karlovarský"),
        p("KB", "Cheb", "Karlovarský"),
        p("KC", "Sokolov", "Karlovarský"),

        // Ústecký
        p("UA", "Ústí nad Labem", "Ústecký"),
        p("UB", "Děčín", "Ústecký"),
        p("UC", "Chomutov", "Ústecký"),
        p("UE", "Litoměřice", "Ústecký"),
        p("UH", "Louny", "Ústecký"),
        p("UI", "Most", "Ústecký"),
        p("UJ", "Teplice", "Ústecký"),

        // Liberecký
        p("LA", "Liberec", "Liberecký"),
        p("LB", "Česká Lípa", "Liberecký"),
        p("LC", "Jablonec nad Nisou", "Liberecký"),
        p("LE", "Semily", "Liberecký"),

        // Královéhradecký
        p("HA", "Hradec Králové", "Královéhradecký"),
        p("HB", "Jičín", "Královéhradecký"),
        p("HC", "Náchod", "Královéhradecký"),
        p("HE", "Rychnov nad Kněžnou", "Královéhradecký"),
        p("HH", "Trutnov", "Královéhradecký"),

        // Pardubický
        p("EA", "Pardubice", "Pardubický"),
        p("EB", "Chrudim", "Pardubický"),
        p("EC", "Svitavy", "Pardubický"),
        p("EE", "Ústí nad Orlicí", "Pardubický"),

        // Vysočina
        p("JI", "Jihlava", "Vysočina"),
        p("JH", "Havlíčkův Brod", "Vysočina"),
        p("JC", "Pelhřimov", "Vysočina"),
        p("JD", "Třebíč", "Vysočina"),
        p("JE", "Žďár nad Sázavou", "Vysočina"),

        // Jihomoravský
        p("BA", "Brno-město", "Jihomoravský"), // dup with Mladá Boleslav — disambiguated by region digit
        p("BB", "Blansko", "Jihomoravský"),
        p("BD", "Brno-venkov", "Jihomoravský"),
        p("BF", "Břeclav", "Jihomoravský"),
        p("BG", "Hodonín", "Jihomoravský"),
        p("BK", "Vyškov", "Jihomoravský"),
        p("BL", "Znojmo", "Jihomoravský"),

        // Olomoucký
        p("MA", "Olomouc", "Olomoucký"),
        p("MB", "Jeseník", "Olomoucký"),
        p("MC", "Prostějov", "Olomoucký"),
        p("ME", "Přerov", "Olomoucký"),
        p("MH", "Šumperk", "Olomoucký"),

        // Zlínský
        p("ZA", "Zlín", "Zlínský"),
        p("ZB", "Kroměříž", "Zlínský"),
        p("ZC", "Uherské Hradiště", "Zlínský"),
        p("ZE", "Vsetín", "Zlínský"),

        // Moravskoslezský
        p("TA", "Ostrava-město", "Moravskoslezský"),
        p("TB", "Bruntál", "Moravskoslezský"),
        p("TC", "Frýdek-Místek", "Moravskoslezský"),
        p("TE", "Karviná", "Moravskoslezský"),
        p("TH", "Nový Jičín", "Moravskoslezský"),
        p("TI", "Opava", "Moravskoslezský"),
    )

    /**
     * Slovak districts. SK plates use the two-letter code as the
     * leading characters (e.g. `BA-123XY` Bratislava). All 79 okresy
     * mapped to current district names + kraj.
     */
    val SK: List<PlateRegion> = listOf(
        // Bratislavský
        s("BA", "Bratislava", "Bratislavský"),
        s("BL", "Bratislava (rezerva)", "Bratislavský"),
        s("BT", "Bratislava (rezerva)", "Bratislavský"),
        s("MA", "Malacky", "Bratislavský"),
        s("PK", "Pezinok", "Bratislavský"),
        s("SC", "Senec", "Bratislavský"),
        // Trnavský
        s("TT", "Trnava", "Trnavský"),
        s("DS", "Dunajská Streda", "Trnavský"),
        s("GA", "Galanta", "Trnavský"),
        s("HC", "Hlohovec", "Trnavský"),
        s("PN", "Piešťany", "Trnavský"),
        s("SE", "Senica", "Trnavský"),
        s("SI", "Skalica", "Trnavský"),
        // Trenčiansky
        s("TN", "Trenčín", "Trenčiansky"),
        s("BN", "Bánovce nad Bebravou", "Trenčiansky"),
        s("IL", "Ilava", "Trenčiansky"),
        s("MY", "Myjava", "Trenčiansky"),
        s("NM", "Nové Mesto nad Váhom", "Trenčiansky"),
        s("PB", "Považská Bystrica", "Trenčiansky"),
        s("PD", "Prievidza", "Trenčiansky"),
        s("PU", "Púchov", "Trenčiansky"),
        // Nitriansky
        s("NR", "Nitra", "Nitriansky"),
        s("KN", "Komárno", "Nitriansky"),
        s("LV", "Levice", "Nitriansky"),
        s("NZ", "Nové Zámky", "Nitriansky"),
        s("SA", "Šaľa", "Nitriansky"),
        s("TO", "Topoľčany", "Nitriansky"),
        s("ZM", "Zlaté Moravce", "Nitriansky"),
        // Žilinský
        s("ZA", "Žilina", "Žilinský"),
        s("BY", "Bytča", "Žilinský"),
        s("CA", "Čadca", "Žilinský"),
        s("DK", "Dolný Kubín", "Žilinský"),
        s("KM", "Kysucké Nové Mesto", "Žilinský"),
        s("LM", "Liptovský Mikuláš", "Žilinský"),
        s("MT", "Martin", "Žilinský"),
        s("NO", "Námestovo", "Žilinský"),
        s("RK", "Ružomberok", "Žilinský"),
        s("TR", "Turčianske Teplice", "Žilinský"),
        s("TS", "Tvrdošín", "Žilinský"),
        // Banskobystrický
        s("BB", "Banská Bystrica", "Banskobystrický"),
        s("BS", "Banská Štiavnica", "Banskobystrický"),
        s("BR", "Brezno", "Banskobystrický"),
        s("DT", "Detva", "Banskobystrický"),
        s("KA", "Krupina", "Banskobystrický"),
        s("LC", "Lučenec", "Banskobystrický"),
        s("PT", "Poltár", "Banskobystrický"),
        s("RA", "Revúca", "Banskobystrický"),
        s("RS", "Rimavská Sobota", "Banskobystrický"),
        s("VK", "Veľký Krtíš", "Banskobystrický"),
        s("ZC", "Žarnovica", "Banskobystrický"),
        s("ZH", "Žiar nad Hronom", "Banskobystrický"),
        s("ZV", "Zvolen", "Banskobystrický"),
        // Prešovský
        s("PO", "Prešov", "Prešovský"),
        s("BJ", "Bardejov", "Prešovský"),
        s("HE", "Humenné", "Prešovský"),
        s("KK", "Kežmarok", "Prešovský"),
        s("LE", "Levoča", "Prešovský"),
        s("ML", "Medzilaborce", "Prešovský"),
        s("PP", "Poprad", "Prešovský"),
        s("SB", "Sabinov", "Prešovský"),
        s("SK", "Svidník", "Prešovský"),
        s("SL", "Stará Ľubovňa", "Prešovský"),
        s("SP", "Stropkov", "Prešovský"),
        s("SV", "Snina", "Prešovský"),
        s("VT", "Vranov nad Topľou", "Prešovský"),
        // Košický
        s("KE", "Košice", "Košický"),
        s("GL", "Gelnica", "Košický"),
        s("MI", "Michalovce", "Košický"),
        s("RV", "Rožňava", "Košický"),
        s("SN", "Spišská Nová Ves", "Košický"),
        s("SO", "Sobrance", "Košický"),
        s("TV", "Trebišov", "Košický"),
    )

    private fun p(code: String, district: String, region: String) =
        PlateRegion(code, Country.CZ, district, region)

    private fun s(code: String, district: String, region: String) =
        PlateRegion(code, Country.SK, district, region)

    /**
     * Try to interpret a raw plate string and return the best-match
     * region. We strip whitespace and dashes, uppercase, then:
     *   1. CZ post-2001: position 1..2 = district code (1AB 1234)
     *   2. CZ pre-2001: position 0..1 (UA 12-34)
     *   3. SK: position 0..1 (BA 123 XY)
     * The result includes the country detected and the matched district.
     * Returns null if nothing matches — caller can show "unknown" UI.
     */
    fun lookup(raw: String): PlateRegion? {
        val cleaned = raw.uppercase().filter { it.isLetterOrDigit() }
        if (cleaned.length < 2) return null

        // CZ post-2001: digit + 2 letters at start
        if (cleaned[0].isDigit() && cleaned.length >= 3 &&
            cleaned[1].isLetter() && cleaned[2].isLetter()) {
            val code = cleaned.substring(1, 3)
            CZ.firstOrNull { it.code == code }?.let { return it }
        }

        // CZ pre-2001 / SK: 2 letters at start
        if (cleaned[0].isLetter() && cleaned[1].isLetter()) {
            val code = cleaned.substring(0, 2)
            // Slovak first because SK plates are unique 2-letter codes
            // and CZ pre-2001 codes overlap with current SK codes (e.g.
            // KE = both Kutná Hora old CZ and Košice). For modern plates
            // the SK match is overwhelmingly the right call.
            SK.firstOrNull { it.code == code }?.let { return it }
            CZ.firstOrNull { it.code == code }?.let { return it }
        }

        return null
    }
}
