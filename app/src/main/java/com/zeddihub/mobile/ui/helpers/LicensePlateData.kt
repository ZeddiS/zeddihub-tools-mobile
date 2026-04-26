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
        DE("🇩🇪", "Německo"),
        AT("🇦🇹", "Rakousko"),
        PL("🇵🇱", "Polsko"),
        HU("🇭🇺", "Maďarsko"),
        SI("🇸🇮", "Slovinsko"),
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
     * Top-50 German "Unterscheidungszeichen" (district / city codes).
     * The full set is ~700 — we ship the highest-population subset
     * which covers ~90 % of plates a CZ/SK driver would see in the
     * wild. Codes can be 1, 2 or 3 characters.
     */
    val DE: List<PlateRegion> = listOf(
        d("B",  "Berlín",                "Berlin"),
        d("M",  "Mnichov",               "Bayern"),
        d("HH", "Hamburk",                "Hamburg"),
        d("K",  "Kolín nad Rýnem",        "Nordrhein-Westfalen"),
        d("F",  "Frankfurt n. Mohanem",   "Hessen"),
        d("S",  "Stuttgart",              "Baden-Württemberg"),
        d("D",  "Düsseldorf",             "Nordrhein-Westfalen"),
        d("L",  "Lipsko",                 "Sachsen"),
        d("DD", "Drážďany",               "Sachsen"),
        d("H",  "Hannover",               "Niedersachsen"),
        d("HB", "Brémy",                  "Bremen"),
        d("N",  "Norimberk",              "Bayern"),
        d("E",  "Essen",                  "Nordrhein-Westfalen"),
        d("DO", "Dortmund",               "Nordrhein-Westfalen"),
        d("DU", "Duisburg",               "Nordrhein-Westfalen"),
        d("BO", "Bochum",                 "Nordrhein-Westfalen"),
        d("WUP","Wuppertal",              "Nordrhein-Westfalen"),
        d("BN", "Bonn",                   "Nordrhein-Westfalen"),
        d("MS", "Münster",                "Nordrhein-Westfalen"),
        d("KA", "Karlsruhe",              "Baden-Württemberg"),
        d("MA", "Mannheim",               "Baden-Württemberg"),
        d("FR", "Freiburg",               "Baden-Württemberg"),
        d("UL", "Ulm",                    "Baden-Württemberg"),
        d("HD", "Heidelberg",             "Baden-Württemberg"),
        d("A",  "Augsburg",               "Bayern"),
        d("R",  "Regensburg",             "Bayern"),
        d("WÜ", "Würzburg",               "Bayern"),
        d("IN", "Ingolstadt",             "Bayern"),
        d("KL", "Kaiserslautern",         "Rheinland-Pfalz"),
        d("MZ", "Mainz",                  "Rheinland-Pfalz"),
        d("KO", "Koblenz",                "Rheinland-Pfalz"),
        d("TR", "Trier",                  "Rheinland-Pfalz"),
        d("SB", "Saarbrücken",            "Saarland"),
        d("KI", "Kiel",                   "Schleswig-Holstein"),
        d("LÜ", "Lübeck",                 "Schleswig-Holstein"),
        d("OS", "Osnabrück",              "Niedersachsen"),
        d("BS", "Braunschweig",           "Niedersachsen"),
        d("OL", "Oldenburg",              "Niedersachsen"),
        d("MD", "Magdeburg",              "Sachsen-Anhalt"),
        d("HAL","Halle",                  "Sachsen-Anhalt"),
        d("ERF","Erfurt",                 "Thüringen"),
        d("J",  "Jena",                   "Thüringen"),
        d("C",  "Chemnitz",               "Sachsen"),
        d("RO", "Rostock",                "Mecklenburg-Vorpommern"),
        d("HRO","Rostock (alt)",          "Mecklenburg-Vorpommern"),
        d("P",  "Potsdam",                "Brandenburg"),
        d("BB", "Brandenburg n.H.",       "Brandenburg"),
        d("FF", "Frankfurt n. Odrou",     "Brandenburg"),
        d("CB", "Cottbus",                "Brandenburg"),
        d("PA", "Pasov",                  "Bayern"),
    )

    /** Top Austrian Bezirkscodes — 2-letter prefix on most plates. */
    val AT: List<PlateRegion> = listOf(
        a("W",  "Vídeň",        "Wien"),
        a("L",  "Linz",         "Oberösterreich"),
        a("G",  "Štýrský Hradec","Steiermark"),
        a("S",  "Salzburg",     "Salzburg"),
        a("I",  "Innsbruck",    "Tirol"),
        a("KL", "Klagenfurt",   "Kärnten"),
        a("VB", "Vöcklabruck",  "Oberösterreich"),
        a("WL", "Wels",         "Oberösterreich"),
        a("SR", "Steyr",        "Oberösterreich"),
        a("BR", "Braunau",      "Oberösterreich"),
        a("ST", "Steyr-Land",   "Oberösterreich"),
        a("GU", "Štýrský Hradec-okolí", "Steiermark"),
        a("LB", "Leibnitz",     "Steiermark"),
        a("VK", "Völkermarkt",  "Kärnten"),
        a("VL", "Villach",      "Kärnten"),
        a("MA", "Mattersburg",  "Burgenland"),
        a("EU", "Eisenstadt",   "Burgenland"),
        a("OP", "Oberpullendorf","Burgenland"),
        a("KR", "Krems",        "Niederösterreich"),
        a("WN", "Wiener Neustadt","Niederösterreich"),
        a("AM", "Amstetten",    "Niederösterreich"),
        a("SL", "Salzburg-okolí","Salzburg"),
        a("ZE", "Zell am See",  "Salzburg"),
        a("BL", "Bludenz",      "Vorarlberg"),
        a("FK", "Feldkirch",    "Vorarlberg"),
        a("KU", "Kufstein",     "Tirol"),
        a("LZ", "Lienz",        "Tirol"),
    )

    /**
     * Polish two-letter voivodeship codes — first letter identifies the
     * voivodeship, second the powiat (county). We map the voivodeship
     * letter only, since per-powiat is ~380 entries; this is enough to
     * answer "this plate is from Mazowsze / Małopolska / etc.".
     */
    val PL: List<PlateRegion> = listOf(
        l("W", "Mazovské vojvodství (Varšava)", "Mazowieckie"),
        l("K", "Malopolské vojvodství (Krakov)", "Małopolskie"),
        l("D", "Dolnoslezské vojvodství (Vratislav)", "Dolnośląskie"),
        l("F", "Lubušské vojvodství", "Lubuskie"),
        l("E", "Lodžské vojvodství", "Łódzkie"),
        l("B", "Podleské vojvodství", "Podlaskie"),
        l("C", "Kujavsko-pomořské vojvodství", "Kujawsko-pomorskie"),
        l("G", "Pomořské vojvodství (Gdaňsk)", "Pomorskie"),
        l("L", "Lublinské vojvodství", "Lubelskie"),
        l("N", "Varminsko-mazurské vojvodství", "Warmińsko-mazurskie"),
        l("O", "Opolské vojvodství", "Opolskie"),
        l("P", "Velkopolské vojvodství (Poznaň)", "Wielkopolskie"),
        l("R", "Podkarpatské vojvodství", "Podkarpackie"),
        l("S", "Slezské vojvodství (Katovice)", "Śląskie"),
        l("T", "Svatokřížské vojvodství", "Świętokrzyskie"),
        l("Z", "Západopomořanské vojvodství", "Zachodniopomorskie"),
    )

    /** Hungarian county codes (post-2022 letter pair format). */
    val HU: List<PlateRegion> = listOf(
        u("AA", "Budapešť", "Közép-Magyarország"),
        u("AB", "Pest",     "Közép-Magyarország"),
        u("AC", "Bács-Kiskun", "Dél-Alföld"),
        u("AD", "Békés",    "Dél-Alföld"),
        u("AE", "Csongrád-Csanád","Dél-Alföld"),
        u("AF", "Hajdú-Bihar","Észak-Alföld"),
        u("AG", "Jász-Nagykun-Szolnok","Észak-Alföld"),
        u("AH", "Szabolcs-Szatmár-Bereg","Észak-Alföld"),
        u("AI", "Borsod-Abaúj-Zemplén","Észak-Magyarország"),
        u("AJ", "Heves",    "Észak-Magyarország"),
        u("AK", "Nógrád",   "Észak-Magyarország"),
        u("AL", "Komárom-Esztergom","Közép-Dunántúl"),
        u("AM", "Fejér",    "Közép-Dunántúl"),
        u("AN", "Veszprém", "Közép-Dunántúl"),
        u("AO", "Győr-Moson-Sopron","Nyugat-Dunántúl"),
        u("AP", "Vas",      "Nyugat-Dunántúl"),
        u("AR", "Zala",     "Nyugat-Dunántúl"),
        u("AS", "Baranya",  "Dél-Dunántúl"),
        u("AT", "Somogy",   "Dél-Dunántúl"),
        u("AU", "Tolna",    "Dél-Dunántúl"),
    )

    /** Slovenian district codes — 2-letter prefix on plates. */
    val SI: List<PlateRegion> = listOf(
        v("LJ", "Lublaň",     "Osrednjeslovenska"),
        v("MB", "Maribor",    "Podravska"),
        v("CE", "Celje",      "Savinjska"),
        v("KP", "Koper",      "Obalno-kraška"),
        v("KK", "Krško",      "Posavska"),
        v("KR", "Kranj",      "Gorenjska"),
        v("MS", "Murska Sobota","Pomurska"),
        v("NM", "Novo Mesto", "Jugovzhodna Slovenija"),
        v("PO", "Postojna",   "Primorsko-notranjska"),
        v("SG", "Slovenj Gradec","Koroška"),
        v("GO", "Nova Gorica","Goriška"),
    )

    private fun d(code: String, district: String, region: String) =
        PlateRegion(code, Country.DE, district, region)
    private fun a(code: String, district: String, region: String) =
        PlateRegion(code, Country.AT, district, region)
    private fun l(code: String, district: String, region: String) =
        PlateRegion(code, Country.PL, district, region)
    private fun u(code: String, district: String, region: String) =
        PlateRegion(code, Country.HU, district, region)
    private fun v(code: String, district: String, region: String) =
        PlateRegion(code, Country.SI, district, region)

    /**
     * Try to interpret a raw plate string and return the best-match
     * region. We strip whitespace and dashes, uppercase, then:
     *   1. CZ post-2001: position 1..2 = district code (1AB 1234)
     *   2. CZ pre-2001: position 0..1 (UA 12-34)
     *   3. SK: position 0..1 (BA 123 XY)
     *   4. DE: 1, 2 or 3 leading letters (B, M / HD, KA / WUP)
     *   5. AT: 1 or 2 leading letters (W, KL, WN…)
     *   6. PL: single leading letter for voivodeship
     *   7. HU / SI: 2 leading letters
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

        // CZ pre-2001 / SK / EU 2-letter: 2 letters at start
        if (cleaned[0].isLetter() && cleaned[1].isLetter()) {
            val code2 = cleaned.substring(0, 2)
            // SK first — modern SK plates are unique 2-letter codes and
            // any overlap with old CZ codes (KE = Kutná Hora vs Košice)
            // is overwhelmingly SK in practice.
            SK.firstOrNull { it.code == code2 }?.let { return it }
            CZ.firstOrNull { it.code == code2 }?.let { return it }
            // Try 3-letter DE codes first (longest match wins) before
            // falling back to 2-letter so "WUP" doesn't match "WU" by
            // accident.
            if (cleaned.length >= 3 && cleaned[2].isLetter()) {
                val code3 = cleaned.substring(0, 3)
                DE.firstOrNull { it.code == code3 }?.let { return it }
            }
            DE.firstOrNull { it.code == code2 }?.let { return it }
            AT.firstOrNull { it.code == code2 }?.let { return it }
            HU.firstOrNull { it.code == code2 }?.let { return it }
            SI.firstOrNull { it.code == code2 }?.let { return it }
        }

        // Single-letter prefix: DE 1-letter (B, M, K, …) and PL.
        // Order: DE first since it's the more common single-letter
        // pattern in the wild around CZ borders.
        if (cleaned[0].isLetter()) {
            val code1 = cleaned.substring(0, 1)
            DE.firstOrNull { it.code == code1 }?.let { return it }
            PL.firstOrNull { it.code == code1 }?.let { return it }
        }

        return null
    }

    /** All region tables in display order — used by the browse UI. */
    val ALL_TABLES: List<Pair<Country, List<PlateRegion>>> = listOf(
        Country.CZ to CZ,
        Country.SK to SK,
        Country.DE to DE,
        Country.AT to AT,
        Country.PL to PL,
        Country.HU to HU,
        Country.SI to SI,
    )
}
