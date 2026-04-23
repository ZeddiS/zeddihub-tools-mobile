package com.zeddihub.mobile.ui.helpers

import java.time.LocalDate
import java.time.Month
import java.time.MonthDay

/**
 * Static data for the holidays / name-days helper.
 *
 * `NameDays.forMonthDay(MonthDay)` returns the Czech name-day that falls
 * on a given day (ignoring year). The list matches the official Czech
 * občanský kalendář.
 *
 * `Holidays.forDate(LocalDate)` returns the state holiday for the given
 * date (or `null`). Easter-dependent holidays are computed each year
 * using the Gregorian Gauss algorithm.
 */
object NameDays {

    /**
     * Map MonthDay → Czech name-day string.
     * February 29 is included; the official calendar has no entry there
     * (we return "—" for that leap day).
     */
    private val table: Map<MonthDay, String> = buildMap {
        // January
        put(MonthDay.of(1, 1),  "—")
        put(MonthDay.of(1, 2),  "Karina")
        put(MonthDay.of(1, 3),  "Radmila")
        put(MonthDay.of(1, 4),  "Diana")
        put(MonthDay.of(1, 5),  "Dalimil")
        put(MonthDay.of(1, 6),  "Tři králové")
        put(MonthDay.of(1, 7),  "Vilma")
        put(MonthDay.of(1, 8),  "Čestmír")
        put(MonthDay.of(1, 9),  "Vladan")
        put(MonthDay.of(1, 10), "Břetislav")
        put(MonthDay.of(1, 11), "Bohdana")
        put(MonthDay.of(1, 12), "Pravoslav")
        put(MonthDay.of(1, 13), "Edita")
        put(MonthDay.of(1, 14), "Radovan")
        put(MonthDay.of(1, 15), "Alice")
        put(MonthDay.of(1, 16), "Ctirad")
        put(MonthDay.of(1, 17), "Drahoslav")
        put(MonthDay.of(1, 18), "Vladislav")
        put(MonthDay.of(1, 19), "Doubravka")
        put(MonthDay.of(1, 20), "Ilona")
        put(MonthDay.of(1, 21), "Běla")
        put(MonthDay.of(1, 22), "Slavomír")
        put(MonthDay.of(1, 23), "Zdeněk")
        put(MonthDay.of(1, 24), "Milena")
        put(MonthDay.of(1, 25), "Miloš")
        put(MonthDay.of(1, 26), "Zora")
        put(MonthDay.of(1, 27), "Ingrid")
        put(MonthDay.of(1, 28), "Otýlie")
        put(MonthDay.of(1, 29), "Zdislava")
        put(MonthDay.of(1, 30), "Robin")
        put(MonthDay.of(1, 31), "Marika")
        // February
        put(MonthDay.of(2, 1),  "Hynek")
        put(MonthDay.of(2, 2),  "Nela")
        put(MonthDay.of(2, 3),  "Blažej")
        put(MonthDay.of(2, 4),  "Jarmila")
        put(MonthDay.of(2, 5),  "Dobromila")
        put(MonthDay.of(2, 6),  "Vanda")
        put(MonthDay.of(2, 7),  "Veronika")
        put(MonthDay.of(2, 8),  "Milada")
        put(MonthDay.of(2, 9),  "Apolena")
        put(MonthDay.of(2, 10), "Mojmír")
        put(MonthDay.of(2, 11), "Božena")
        put(MonthDay.of(2, 12), "Slavěna")
        put(MonthDay.of(2, 13), "Věnceslav")
        put(MonthDay.of(2, 14), "Valentýn")
        put(MonthDay.of(2, 15), "Jiřina")
        put(MonthDay.of(2, 16), "Ljuba")
        put(MonthDay.of(2, 17), "Miloslava")
        put(MonthDay.of(2, 18), "Gizela")
        put(MonthDay.of(2, 19), "Patrik")
        put(MonthDay.of(2, 20), "Oldřich")
        put(MonthDay.of(2, 21), "Lenka")
        put(MonthDay.of(2, 22), "Petr")
        put(MonthDay.of(2, 23), "Svatopluk")
        put(MonthDay.of(2, 24), "Matěj")
        put(MonthDay.of(2, 25), "Liliana")
        put(MonthDay.of(2, 26), "Dorota")
        put(MonthDay.of(2, 27), "Alexandr")
        put(MonthDay.of(2, 28), "Lumír")
        put(MonthDay.of(2, 29), "—")
        // March
        put(MonthDay.of(3, 1),  "Bedřich")
        put(MonthDay.of(3, 2),  "Anežka")
        put(MonthDay.of(3, 3),  "Kamil")
        put(MonthDay.of(3, 4),  "Stela")
        put(MonthDay.of(3, 5),  "Kazimír")
        put(MonthDay.of(3, 6),  "Miroslav")
        put(MonthDay.of(3, 7),  "Tomáš")
        put(MonthDay.of(3, 8),  "Gabriela")
        put(MonthDay.of(3, 9),  "Františka")
        put(MonthDay.of(3, 10), "Viktorie")
        put(MonthDay.of(3, 11), "Anděla")
        put(MonthDay.of(3, 12), "Řehoř")
        put(MonthDay.of(3, 13), "Růžena")
        put(MonthDay.of(3, 14), "Rút, Matylda")
        put(MonthDay.of(3, 15), "Ida")
        put(MonthDay.of(3, 16), "Elena, Herbert")
        put(MonthDay.of(3, 17), "Vlastimil")
        put(MonthDay.of(3, 18), "Eduard")
        put(MonthDay.of(3, 19), "Josef")
        put(MonthDay.of(3, 20), "Světlana")
        put(MonthDay.of(3, 21), "Radek")
        put(MonthDay.of(3, 22), "Leona")
        put(MonthDay.of(3, 23), "Ivona")
        put(MonthDay.of(3, 24), "Gabriel")
        put(MonthDay.of(3, 25), "Marián")
        put(MonthDay.of(3, 26), "Emanuel")
        put(MonthDay.of(3, 27), "Dita")
        put(MonthDay.of(3, 28), "Soňa")
        put(MonthDay.of(3, 29), "Taťána")
        put(MonthDay.of(3, 30), "Arnošt")
        put(MonthDay.of(3, 31), "Kvido")
        // April
        put(MonthDay.of(4, 1),  "Hugo")
        put(MonthDay.of(4, 2),  "Erika")
        put(MonthDay.of(4, 3),  "Richard")
        put(MonthDay.of(4, 4),  "Ivana")
        put(MonthDay.of(4, 5),  "Miroslava")
        put(MonthDay.of(4, 6),  "Vendula")
        put(MonthDay.of(4, 7),  "Heřman, Hermína")
        put(MonthDay.of(4, 8),  "Ema")
        put(MonthDay.of(4, 9),  "Dušan")
        put(MonthDay.of(4, 10), "Darja")
        put(MonthDay.of(4, 11), "Izabela")
        put(MonthDay.of(4, 12), "Julius")
        put(MonthDay.of(4, 13), "Aleš")
        put(MonthDay.of(4, 14), "Vincenc")
        put(MonthDay.of(4, 15), "Anastázie")
        put(MonthDay.of(4, 16), "Irena")
        put(MonthDay.of(4, 17), "Rudolf")
        put(MonthDay.of(4, 18), "Valérie")
        put(MonthDay.of(4, 19), "Rostislav")
        put(MonthDay.of(4, 20), "Marcela")
        put(MonthDay.of(4, 21), "Alexandra")
        put(MonthDay.of(4, 22), "Evženie")
        put(MonthDay.of(4, 23), "Vojtěch")
        put(MonthDay.of(4, 24), "Jiří")
        put(MonthDay.of(4, 25), "Marek")
        put(MonthDay.of(4, 26), "Oto")
        put(MonthDay.of(4, 27), "Jaroslav")
        put(MonthDay.of(4, 28), "Vlastislav")
        put(MonthDay.of(4, 29), "Robert")
        put(MonthDay.of(4, 30), "Blahoslav")
        // May
        put(MonthDay.of(5, 1),  "—")
        put(MonthDay.of(5, 2),  "Zikmund")
        put(MonthDay.of(5, 3),  "Alexej")
        put(MonthDay.of(5, 4),  "Květoslav")
        put(MonthDay.of(5, 5),  "Klaudie")
        put(MonthDay.of(5, 6),  "Radoslav")
        put(MonthDay.of(5, 7),  "Stanislav")
        put(MonthDay.of(5, 8),  "—")
        put(MonthDay.of(5, 9),  "Ctibor")
        put(MonthDay.of(5, 10), "Blažena")
        put(MonthDay.of(5, 11), "Svatava")
        put(MonthDay.of(5, 12), "Pankrác")
        put(MonthDay.of(5, 13), "Servác")
        put(MonthDay.of(5, 14), "Bonifác")
        put(MonthDay.of(5, 15), "Žofie")
        put(MonthDay.of(5, 16), "Přemysl")
        put(MonthDay.of(5, 17), "Aneta")
        put(MonthDay.of(5, 18), "Nataša")
        put(MonthDay.of(5, 19), "Ivo")
        put(MonthDay.of(5, 20), "Zbyšek")
        put(MonthDay.of(5, 21), "Monika")
        put(MonthDay.of(5, 22), "Emil")
        put(MonthDay.of(5, 23), "Vladimír")
        put(MonthDay.of(5, 24), "Jana")
        put(MonthDay.of(5, 25), "Viola")
        put(MonthDay.of(5, 26), "Filip")
        put(MonthDay.of(5, 27), "Valdemar")
        put(MonthDay.of(5, 28), "Vilém")
        put(MonthDay.of(5, 29), "Maxmilián")
        put(MonthDay.of(5, 30), "Ferdinand")
        put(MonthDay.of(5, 31), "Kamila")
        // June
        put(MonthDay.of(6, 1),  "Laura")
        put(MonthDay.of(6, 2),  "Jarmil")
        put(MonthDay.of(6, 3),  "Tamara")
        put(MonthDay.of(6, 4),  "Dalibor")
        put(MonthDay.of(6, 5),  "Dobroslav")
        put(MonthDay.of(6, 6),  "Norbert")
        put(MonthDay.of(6, 7),  "Iveta, Slavoj")
        put(MonthDay.of(6, 8),  "Medard")
        put(MonthDay.of(6, 9),  "Stanislava")
        put(MonthDay.of(6, 10), "Gita")
        put(MonthDay.of(6, 11), "Bruno")
        put(MonthDay.of(6, 12), "Antonie")
        put(MonthDay.of(6, 13), "Antonín")
        put(MonthDay.of(6, 14), "Roland")
        put(MonthDay.of(6, 15), "Vít")
        put(MonthDay.of(6, 16), "Zbyněk")
        put(MonthDay.of(6, 17), "Adolf")
        put(MonthDay.of(6, 18), "Milan")
        put(MonthDay.of(6, 19), "Leoš")
        put(MonthDay.of(6, 20), "Květa")
        put(MonthDay.of(6, 21), "Alois")
        put(MonthDay.of(6, 22), "Pavla")
        put(MonthDay.of(6, 23), "Zdeňka")
        put(MonthDay.of(6, 24), "Jan")
        put(MonthDay.of(6, 25), "Ivan")
        put(MonthDay.of(6, 26), "Adriana")
        put(MonthDay.of(6, 27), "Ladislav")
        put(MonthDay.of(6, 28), "Lubomír")
        put(MonthDay.of(6, 29), "Petr a Pavel")
        put(MonthDay.of(6, 30), "Šárka")
        // July
        put(MonthDay.of(7, 1),  "Jaroslava")
        put(MonthDay.of(7, 2),  "Patricie")
        put(MonthDay.of(7, 3),  "Radomír")
        put(MonthDay.of(7, 4),  "Prokop")
        put(MonthDay.of(7, 5),  "—")
        put(MonthDay.of(7, 6),  "—")
        put(MonthDay.of(7, 7),  "Bohuslava")
        put(MonthDay.of(7, 8),  "Nora")
        put(MonthDay.of(7, 9),  "Drahoslava")
        put(MonthDay.of(7, 10), "Libuše, Amálie")
        put(MonthDay.of(7, 11), "Olga")
        put(MonthDay.of(7, 12), "Bořek")
        put(MonthDay.of(7, 13), "Markéta")
        put(MonthDay.of(7, 14), "Karolína")
        put(MonthDay.of(7, 15), "Jindřich")
        put(MonthDay.of(7, 16), "Luboš")
        put(MonthDay.of(7, 17), "Martina")
        put(MonthDay.of(7, 18), "Drahomíra")
        put(MonthDay.of(7, 19), "Čeněk")
        put(MonthDay.of(7, 20), "Ilja")
        put(MonthDay.of(7, 21), "Vítězslav")
        put(MonthDay.of(7, 22), "Magdaléna")
        put(MonthDay.of(7, 23), "Libor")
        put(MonthDay.of(7, 24), "Kristýna")
        put(MonthDay.of(7, 25), "Jakub")
        put(MonthDay.of(7, 26), "Anna")
        put(MonthDay.of(7, 27), "Věroslav")
        put(MonthDay.of(7, 28), "Viktor")
        put(MonthDay.of(7, 29), "Marta")
        put(MonthDay.of(7, 30), "Bořivoj")
        put(MonthDay.of(7, 31), "Ignác")
        // August
        put(MonthDay.of(8, 1),  "Oskar")
        put(MonthDay.of(8, 2),  "Gustav")
        put(MonthDay.of(8, 3),  "Miluše")
        put(MonthDay.of(8, 4),  "Dominik")
        put(MonthDay.of(8, 5),  "Kristián")
        put(MonthDay.of(8, 6),  "Oldřiška")
        put(MonthDay.of(8, 7),  "Lada")
        put(MonthDay.of(8, 8),  "Soběslav")
        put(MonthDay.of(8, 9),  "Roman")
        put(MonthDay.of(8, 10), "Vavřinec")
        put(MonthDay.of(8, 11), "Zuzana")
        put(MonthDay.of(8, 12), "Klára")
        put(MonthDay.of(8, 13), "Alena")
        put(MonthDay.of(8, 14), "Alan")
        put(MonthDay.of(8, 15), "Hana")
        put(MonthDay.of(8, 16), "Jáchym")
        put(MonthDay.of(8, 17), "Petra")
        put(MonthDay.of(8, 18), "Helena")
        put(MonthDay.of(8, 19), "Ludvík")
        put(MonthDay.of(8, 20), "Bernard")
        put(MonthDay.of(8, 21), "Johana")
        put(MonthDay.of(8, 22), "Bohuslav")
        put(MonthDay.of(8, 23), "Sandra")
        put(MonthDay.of(8, 24), "Bartoloměj")
        put(MonthDay.of(8, 25), "Radim")
        put(MonthDay.of(8, 26), "Luděk")
        put(MonthDay.of(8, 27), "Otakar")
        put(MonthDay.of(8, 28), "Augustýn")
        put(MonthDay.of(8, 29), "Evelína")
        put(MonthDay.of(8, 30), "Vladěna")
        put(MonthDay.of(8, 31), "Pavlína")
        // September
        put(MonthDay.of(9, 1),  "Linda, Samuel")
        put(MonthDay.of(9, 2),  "Adéla")
        put(MonthDay.of(9, 3),  "Bronislav")
        put(MonthDay.of(9, 4),  "Jindřiška")
        put(MonthDay.of(9, 5),  "Boris")
        put(MonthDay.of(9, 6),  "Boleslav")
        put(MonthDay.of(9, 7),  "Regína")
        put(MonthDay.of(9, 8),  "Mariana")
        put(MonthDay.of(9, 9),  "Daniela")
        put(MonthDay.of(9, 10), "Irma")
        put(MonthDay.of(9, 11), "Denisa")
        put(MonthDay.of(9, 12), "Marie")
        put(MonthDay.of(9, 13), "Lubor")
        put(MonthDay.of(9, 14), "Radka")
        put(MonthDay.of(9, 15), "Jolana")
        put(MonthDay.of(9, 16), "Ludmila")
        put(MonthDay.of(9, 17), "Naděžda")
        put(MonthDay.of(9, 18), "Kryštof")
        put(MonthDay.of(9, 19), "Zita")
        put(MonthDay.of(9, 20), "Oleg")
        put(MonthDay.of(9, 21), "Matouš")
        put(MonthDay.of(9, 22), "Darina")
        put(MonthDay.of(9, 23), "Berta")
        put(MonthDay.of(9, 24), "Jaromír")
        put(MonthDay.of(9, 25), "Zlata")
        put(MonthDay.of(9, 26), "Andrea")
        put(MonthDay.of(9, 27), "Jonáš")
        put(MonthDay.of(9, 28), "—")
        put(MonthDay.of(9, 29), "Michal")
        put(MonthDay.of(9, 30), "Jeroným")
        // October
        put(MonthDay.of(10, 1),  "Igor")
        put(MonthDay.of(10, 2),  "Olívie, Oliver")
        put(MonthDay.of(10, 3),  "Bohumil")
        put(MonthDay.of(10, 4),  "František")
        put(MonthDay.of(10, 5),  "Eliška")
        put(MonthDay.of(10, 6),  "Hanuš")
        put(MonthDay.of(10, 7),  "Justýna")
        put(MonthDay.of(10, 8),  "Věra")
        put(MonthDay.of(10, 9),  "Štefan, Sára")
        put(MonthDay.of(10, 10), "Marina")
        put(MonthDay.of(10, 11), "Andrej")
        put(MonthDay.of(10, 12), "Marcel")
        put(MonthDay.of(10, 13), "Renáta")
        put(MonthDay.of(10, 14), "Agáta")
        put(MonthDay.of(10, 15), "Tereza")
        put(MonthDay.of(10, 16), "Havel")
        put(MonthDay.of(10, 17), "Hedvika")
        put(MonthDay.of(10, 18), "Lukáš")
        put(MonthDay.of(10, 19), "Michaela")
        put(MonthDay.of(10, 20), "Vendelín")
        put(MonthDay.of(10, 21), "Brigita")
        put(MonthDay.of(10, 22), "Sabina")
        put(MonthDay.of(10, 23), "Teodor")
        put(MonthDay.of(10, 24), "Nina")
        put(MonthDay.of(10, 25), "Beáta")
        put(MonthDay.of(10, 26), "Erik")
        put(MonthDay.of(10, 27), "Šarlota, Zoe")
        put(MonthDay.of(10, 28), "—")
        put(MonthDay.of(10, 29), "Silvie")
        put(MonthDay.of(10, 30), "Tadeáš")
        put(MonthDay.of(10, 31), "Štěpánka")
        // November
        put(MonthDay.of(11, 1),  "Felix")
        put(MonthDay.of(11, 2),  "Památka zesnulých")
        put(MonthDay.of(11, 3),  "Hubert")
        put(MonthDay.of(11, 4),  "Karel")
        put(MonthDay.of(11, 5),  "Miriam")
        put(MonthDay.of(11, 6),  "Liběna")
        put(MonthDay.of(11, 7),  "Saskie")
        put(MonthDay.of(11, 8),  "Bohumír")
        put(MonthDay.of(11, 9),  "Bohdan")
        put(MonthDay.of(11, 10), "Evžen")
        put(MonthDay.of(11, 11), "Martin")
        put(MonthDay.of(11, 12), "Benedikt")
        put(MonthDay.of(11, 13), "Tibor")
        put(MonthDay.of(11, 14), "Sáva")
        put(MonthDay.of(11, 15), "Leopold")
        put(MonthDay.of(11, 16), "Otmar")
        put(MonthDay.of(11, 17), "—")
        put(MonthDay.of(11, 18), "Romana")
        put(MonthDay.of(11, 19), "Alžběta")
        put(MonthDay.of(11, 20), "Nikola")
        put(MonthDay.of(11, 21), "Albert")
        put(MonthDay.of(11, 22), "Cecílie")
        put(MonthDay.of(11, 23), "Klement")
        put(MonthDay.of(11, 24), "Emílie")
        put(MonthDay.of(11, 25), "Kateřina")
        put(MonthDay.of(11, 26), "Artur")
        put(MonthDay.of(11, 27), "Xenie")
        put(MonthDay.of(11, 28), "René")
        put(MonthDay.of(11, 29), "Zina")
        put(MonthDay.of(11, 30), "Ondřej")
        // December
        put(MonthDay.of(12, 1),  "Iva")
        put(MonthDay.of(12, 2),  "Blanka")
        put(MonthDay.of(12, 3),  "Svatoslav")
        put(MonthDay.of(12, 4),  "Barbora")
        put(MonthDay.of(12, 5),  "Jitka")
        put(MonthDay.of(12, 6),  "Mikuláš")
        put(MonthDay.of(12, 7),  "Ambrož, Benjamín")
        put(MonthDay.of(12, 8),  "Květoslava")
        put(MonthDay.of(12, 9),  "Vratislav")
        put(MonthDay.of(12, 10), "Julie")
        put(MonthDay.of(12, 11), "Dana")
        put(MonthDay.of(12, 12), "Simona")
        put(MonthDay.of(12, 13), "Lucie")
        put(MonthDay.of(12, 14), "Lýdie")
        put(MonthDay.of(12, 15), "Radana")
        put(MonthDay.of(12, 16), "Albína")
        put(MonthDay.of(12, 17), "Daniel")
        put(MonthDay.of(12, 18), "Miloslav")
        put(MonthDay.of(12, 19), "Ester")
        put(MonthDay.of(12, 20), "Dagmar")
        put(MonthDay.of(12, 21), "Natálie")
        put(MonthDay.of(12, 22), "Šimon")
        put(MonthDay.of(12, 23), "Vlasta")
        put(MonthDay.of(12, 24), "Adam a Eva")
        put(MonthDay.of(12, 25), "—")
        put(MonthDay.of(12, 26), "Štěpán")
        put(MonthDay.of(12, 27), "Žaneta")
        put(MonthDay.of(12, 28), "Bohumila")
        put(MonthDay.of(12, 29), "Judita")
        put(MonthDay.of(12, 30), "David")
        put(MonthDay.of(12, 31), "Silvestr")
    }

    fun forMonthDay(md: MonthDay): String = table[md] ?: "—"

    /** All entries ordered Jan 1 → Dec 31, used for the calendar tab. */
    val orderedEntries: List<Pair<MonthDay, String>> = table.entries
        .map { it.key to it.value }
        .sortedWith(compareBy({ it.first.monthValue }, { it.first.dayOfMonth }))
}

/**
 * Czech state holidays (both public holidays and "significant days"
 * that grant a day off). The `free` flag follows Act No. 245/2000 Sb.
 */
data class Holiday(
    val name: String,
    val date: LocalDate,
    val free: Boolean
)

object Holidays {

    /** Easter Sunday using Gauss's Gregorian algorithm. */
    fun easterSunday(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1
        return LocalDate.of(year, month, day)
    }

    /** All state holidays for a given year. */
    fun forYear(year: Int): List<Holiday> {
        val easter = easterSunday(year)
        val goodFriday = easter.minusDays(2)
        val easterMonday = easter.plusDays(1)
        return listOf(
            Holiday("Nový rok / Den obnovy samostatného českého státu", LocalDate.of(year, Month.JANUARY, 1), true),
            Holiday("Velký pátek", goodFriday, true),
            Holiday("Velikonoční pondělí", easterMonday, true),
            Holiday("Svátek práce", LocalDate.of(year, Month.MAY, 1), true),
            Holiday("Den vítězství", LocalDate.of(year, Month.MAY, 8), true),
            Holiday("Den slovanských věrozvěstů Cyrila a Metoděje", LocalDate.of(year, Month.JULY, 5), true),
            Holiday("Den upálení mistra Jana Husa", LocalDate.of(year, Month.JULY, 6), true),
            Holiday("Den české státnosti", LocalDate.of(year, Month.SEPTEMBER, 28), true),
            Holiday("Den vzniku samostatného československého státu", LocalDate.of(year, Month.OCTOBER, 28), true),
            Holiday("Den boje za svobodu a demokracii", LocalDate.of(year, Month.NOVEMBER, 17), true),
            Holiday("Štědrý den", LocalDate.of(year, Month.DECEMBER, 24), true),
            Holiday("1. svátek vánoční", LocalDate.of(year, Month.DECEMBER, 25), true),
            Holiday("2. svátek vánoční", LocalDate.of(year, Month.DECEMBER, 26), true)
        ).sortedBy { it.date }
    }

    /** Return the state holiday for a given date or null. */
    fun forDate(date: LocalDate): Holiday? =
        forYear(date.year).firstOrNull { it.date == date }
}
