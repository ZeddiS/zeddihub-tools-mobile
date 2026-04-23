package com.zeddihub.mobile.ui.helpers

/**
 * Static data for the periodic-table helper.
 *
 *   z        atomic number (1..118)
 *   symbol   element symbol (H, He, …)
 *   nameCs   Czech name
 *   nameEn   English name
 *   mass     standard atomic weight (IUPAC 2021, u). `Double.NaN` for
 *            unstable synthetic elements where no conventional value
 *            is published (we render "—" instead).
 *   group    one of [PeriodicGroup]
 *   period   table row, 1..7
 *   col      column in the 18-wide main grid (1..18) — lanthanides /
 *            actinides use col = 0 and are laid out below the main
 *            block; column tag still distinguishes them by period.
 *   phase    "s" solid, "l" liquid, "g" gas, "?" unknown at 25 °C
 *   en       Pauling electronegativity (0.0 if unknown)
 *   melt     melting point in °C (NaN if unknown)
 *   boil     boiling point in °C (NaN if unknown)
 *   density  g/cm³ at STP (NaN if unknown)
 *
 * All values are public-domain data compiled from the IUPAC tables
 * and NIST; we use them just to render the helper, not as a
 * scientific reference.
 */
enum class PeriodicGroup {
    ALKALI_METAL, ALKALINE_EARTH, TRANSITION, POST_TRANSITION,
    METALLOID, NONMETAL, HALOGEN, NOBLE_GAS,
    LANTHANIDE, ACTINIDE, UNKNOWN
}

data class Element(
    val z: Int,
    val symbol: String,
    val nameCs: String,
    val nameEn: String,
    val mass: Double,
    val group: PeriodicGroup,
    val period: Int,
    val col: Int,
    val phase: String,
    val en: Double,
    val melt: Double,
    val boil: Double,
    val density: Double
)

object PeriodicTable {

    /** Indexed 0..117 by [Element.z] − 1. */
    val elements: List<Element> = listOf(
        Element(1,   "H",  "Vodík",        "Hydrogen",     1.008,   PeriodicGroup.NONMETAL,        1, 1,  "g",  2.20, -259.16, -252.87, 0.00008988),
        Element(2,   "He", "Helium",       "Helium",       4.0026,  PeriodicGroup.NOBLE_GAS,       1, 18, "g",  0.0,  Double.NaN, -268.93, 0.0001785),
        Element(3,   "Li", "Lithium",      "Lithium",      6.94,    PeriodicGroup.ALKALI_METAL,    2, 1,  "s",  0.98, 180.54, 1342.0, 0.534),
        Element(4,   "Be", "Beryllium",    "Beryllium",    9.0122,  PeriodicGroup.ALKALINE_EARTH,  2, 2,  "s",  1.57, 1287.0, 2469.0, 1.85),
        Element(5,   "B",  "Bor",          "Boron",        10.81,   PeriodicGroup.METALLOID,       2, 13, "s",  2.04, 2076.0, 3927.0, 2.34),
        Element(6,   "C",  "Uhlík",        "Carbon",       12.011,  PeriodicGroup.NONMETAL,        2, 14, "s",  2.55, 3550.0, 4027.0, 2.267),
        Element(7,   "N",  "Dusík",        "Nitrogen",     14.007,  PeriodicGroup.NONMETAL,        2, 15, "g",  3.04, -210.1, -195.79, 0.001251),
        Element(8,   "O",  "Kyslík",       "Oxygen",       15.999,  PeriodicGroup.NONMETAL,        2, 16, "g",  3.44, -218.79, -182.95, 0.001429),
        Element(9,   "F",  "Fluor",        "Fluorine",     18.998,  PeriodicGroup.HALOGEN,         2, 17, "g",  3.98, -219.67, -188.11, 0.001696),
        Element(10,  "Ne", "Neon",         "Neon",         20.180,  PeriodicGroup.NOBLE_GAS,       2, 18, "g",  0.0,  -248.59, -246.05, 0.0008999),
        Element(11,  "Na", "Sodík",        "Sodium",       22.990,  PeriodicGroup.ALKALI_METAL,    3, 1,  "s",  0.93, 97.72, 883.0, 0.971),
        Element(12,  "Mg", "Hořčík",       "Magnesium",    24.305,  PeriodicGroup.ALKALINE_EARTH,  3, 2,  "s",  1.31, 650.0, 1090.0, 1.738),
        Element(13,  "Al", "Hliník",       "Aluminum",     26.982,  PeriodicGroup.POST_TRANSITION, 3, 13, "s",  1.61, 660.32, 2470.0, 2.70),
        Element(14,  "Si", "Křemík",       "Silicon",      28.085,  PeriodicGroup.METALLOID,       3, 14, "s",  1.90, 1414.0, 3265.0, 2.3296),
        Element(15,  "P",  "Fosfor",       "Phosphorus",   30.974,  PeriodicGroup.NONMETAL,        3, 15, "s",  2.19, 44.15, 280.5, 1.82),
        Element(16,  "S",  "Síra",         "Sulfur",       32.06,   PeriodicGroup.NONMETAL,        3, 16, "s",  2.58, 115.21, 444.61, 2.067),
        Element(17,  "Cl", "Chlor",        "Chlorine",     35.45,   PeriodicGroup.HALOGEN,         3, 17, "g",  3.16, -101.5, -34.04, 0.003214),
        Element(18,  "Ar", "Argon",        "Argon",        39.95,   PeriodicGroup.NOBLE_GAS,       3, 18, "g",  0.0,  -189.34, -185.85, 0.0017837),
        Element(19,  "K",  "Draslík",      "Potassium",    39.098,  PeriodicGroup.ALKALI_METAL,    4, 1,  "s",  0.82, 63.38, 759.0, 0.862),
        Element(20,  "Ca", "Vápník",       "Calcium",      40.078,  PeriodicGroup.ALKALINE_EARTH,  4, 2,  "s",  1.00, 842.0, 1484.0, 1.54),
        Element(21,  "Sc", "Skandium",     "Scandium",     44.956,  PeriodicGroup.TRANSITION,      4, 3,  "s",  1.36, 1541.0, 2836.0, 2.985),
        Element(22,  "Ti", "Titan",        "Titanium",     47.867,  PeriodicGroup.TRANSITION,      4, 4,  "s",  1.54, 1668.0, 3287.0, 4.506),
        Element(23,  "V",  "Vanad",        "Vanadium",     50.942,  PeriodicGroup.TRANSITION,      4, 5,  "s",  1.63, 1910.0, 3407.0, 6.11),
        Element(24,  "Cr", "Chrom",        "Chromium",     51.996,  PeriodicGroup.TRANSITION,      4, 6,  "s",  1.66, 1907.0, 2671.0, 7.15),
        Element(25,  "Mn", "Mangan",       "Manganese",    54.938,  PeriodicGroup.TRANSITION,      4, 7,  "s",  1.55, 1246.0, 2061.0, 7.21),
        Element(26,  "Fe", "Železo",       "Iron",         55.845,  PeriodicGroup.TRANSITION,      4, 8,  "s",  1.83, 1538.0, 2862.0, 7.874),
        Element(27,  "Co", "Kobalt",       "Cobalt",       58.933,  PeriodicGroup.TRANSITION,      4, 9,  "s",  1.88, 1495.0, 2927.0, 8.90),
        Element(28,  "Ni", "Nikl",         "Nickel",       58.693,  PeriodicGroup.TRANSITION,      4, 10, "s",  1.91, 1455.0, 2913.0, 8.908),
        Element(29,  "Cu", "Měď",          "Copper",       63.546,  PeriodicGroup.TRANSITION,      4, 11, "s",  1.90, 1084.62, 2562.0, 8.96),
        Element(30,  "Zn", "Zinek",        "Zinc",         65.38,   PeriodicGroup.TRANSITION,      4, 12, "s",  1.65, 419.53, 907.0, 7.14),
        Element(31,  "Ga", "Gallium",      "Gallium",      69.723,  PeriodicGroup.POST_TRANSITION, 4, 13, "s",  1.81, 29.76, 2204.0, 5.91),
        Element(32,  "Ge", "Germanium",    "Germanium",    72.630,  PeriodicGroup.METALLOID,       4, 14, "s",  2.01, 938.25, 2833.0, 5.323),
        Element(33,  "As", "Arsen",        "Arsenic",      74.922,  PeriodicGroup.METALLOID,       4, 15, "s",  2.18, 817.0, 614.0, 5.727),
        Element(34,  "Se", "Selen",        "Selenium",     78.971,  PeriodicGroup.NONMETAL,        4, 16, "s",  2.55, 221.0, 685.0, 4.819),
        Element(35,  "Br", "Brom",         "Bromine",      79.904,  PeriodicGroup.HALOGEN,         4, 17, "l",  2.96, -7.2, 58.8, 3.1028),
        Element(36,  "Kr", "Krypton",      "Krypton",      83.798,  PeriodicGroup.NOBLE_GAS,       4, 18, "g",  3.0,  -157.36, -153.22, 0.003733),
        Element(37,  "Rb", "Rubidium",     "Rubidium",     85.468,  PeriodicGroup.ALKALI_METAL,    5, 1,  "s",  0.82, 39.31, 688.0, 1.532),
        Element(38,  "Sr", "Stroncium",    "Strontium",    87.62,   PeriodicGroup.ALKALINE_EARTH,  5, 2,  "s",  0.95, 777.0, 1382.0, 2.64),
        Element(39,  "Y",  "Yttrium",      "Yttrium",      88.906,  PeriodicGroup.TRANSITION,      5, 3,  "s",  1.22, 1526.0, 3345.0, 4.472),
        Element(40,  "Zr", "Zirkonium",    "Zirconium",    91.224,  PeriodicGroup.TRANSITION,      5, 4,  "s",  1.33, 1855.0, 4409.0, 6.52),
        Element(41,  "Nb", "Niob",         "Niobium",      92.906,  PeriodicGroup.TRANSITION,      5, 5,  "s",  1.6,  2477.0, 4744.0, 8.57),
        Element(42,  "Mo", "Molybden",     "Molybdenum",   95.95,   PeriodicGroup.TRANSITION,      5, 6,  "s",  2.16, 2623.0, 4639.0, 10.28),
        Element(43,  "Tc", "Technecium",   "Technetium",   98.0,    PeriodicGroup.TRANSITION,      5, 7,  "s",  1.9,  2157.0, 4265.0, 11.5),
        Element(44,  "Ru", "Ruthenium",    "Ruthenium",    101.07,  PeriodicGroup.TRANSITION,      5, 8,  "s",  2.2,  2334.0, 4150.0, 12.45),
        Element(45,  "Rh", "Rhodium",      "Rhodium",      102.91,  PeriodicGroup.TRANSITION,      5, 9,  "s",  2.28, 1964.0, 3695.0, 12.41),
        Element(46,  "Pd", "Palladium",    "Palladium",    106.42,  PeriodicGroup.TRANSITION,      5, 10, "s",  2.20, 1554.9, 2963.0, 12.023),
        Element(47,  "Ag", "Stříbro",      "Silver",       107.87,  PeriodicGroup.TRANSITION,      5, 11, "s",  1.93, 961.78, 2162.0, 10.49),
        Element(48,  "Cd", "Kadmium",      "Cadmium",      112.41,  PeriodicGroup.TRANSITION,      5, 12, "s",  1.69, 321.07, 767.0, 8.65),
        Element(49,  "In", "Indium",       "Indium",       114.82,  PeriodicGroup.POST_TRANSITION, 5, 13, "s",  1.78, 156.60, 2072.0, 7.31),
        Element(50,  "Sn", "Cín",          "Tin",          118.71,  PeriodicGroup.POST_TRANSITION, 5, 14, "s",  1.96, 231.93, 2602.0, 7.287),
        Element(51,  "Sb", "Antimon",      "Antimony",     121.76,  PeriodicGroup.METALLOID,       5, 15, "s",  2.05, 630.63, 1587.0, 6.685),
        Element(52,  "Te", "Tellur",       "Tellurium",    127.60,  PeriodicGroup.METALLOID,       5, 16, "s",  2.1,  449.51, 988.0, 6.232),
        Element(53,  "I",  "Jod",          "Iodine",       126.90,  PeriodicGroup.HALOGEN,         5, 17, "s",  2.66, 113.7, 184.3, 4.93),
        Element(54,  "Xe", "Xenon",        "Xenon",        131.29,  PeriodicGroup.NOBLE_GAS,       5, 18, "g",  2.6,  -111.8, -108.1, 0.005887),
        Element(55,  "Cs", "Cesium",       "Caesium",      132.91,  PeriodicGroup.ALKALI_METAL,    6, 1,  "s",  0.79, 28.44, 671.0, 1.873),
        Element(56,  "Ba", "Baryum",       "Barium",       137.33,  PeriodicGroup.ALKALINE_EARTH,  6, 2,  "s",  0.89, 727.0, 1897.0, 3.62),
        Element(57,  "La", "Lanthan",      "Lanthanum",    138.91,  PeriodicGroup.LANTHANIDE,      6, 0,  "s",  1.10, 920.0, 3464.0, 6.145),
        Element(58,  "Ce", "Cer",          "Cerium",       140.12,  PeriodicGroup.LANTHANIDE,      6, 0,  "s",  1.12, 795.0, 3443.0, 6.770),
        Element(59,  "Pr", "Praseodym",    "Praseodymium", 140.91,  PeriodicGroup.LANTHANIDE,      6, 0,  "s",  1.13, 935.0, 3520.0, 6.773),
        Element(60,  "Nd", "Neodym",       "Neodymium",    144.24,  PeriodicGroup.LANTHANIDE,      6, 0,  "s",  1.14, 1024.0, 3074.0, 7.007),
        Element(61,  "Pm", "Promethium",   "Promethium",   145.0,   PeriodicGroup.LANTHANIDE,      6, 0,  "s",  1.13, 1042.0, 3000.0, 7.26),
        Element(62,  "Sm", "Samarium",     "Samarium",     150.36,  PeriodicGroup.LANTHANIDE,      6, 0,  "s",  1.17, 1072.0, 1794.0, 7.52),
        Element(63,  "Eu", "Europium",     "Europium",     151.96,  PeriodicGroup.LANTHANIDE,      6, 0,  "s",  1.2,  822.0, 1529.0, 5.243),
        Element(64,  "Gd", "Gadolinium",   "Gadolinium",   157.25,  PeriodicGroup.LANTHANIDE,      6, 0,  "s",  1.20, 1313.0, 3273.0, 7.895),
        Element(65,  "Tb", "Terbium",      "Terbium",      158.93,  PeriodicGroup.LANTHANIDE,      6, 0,  "s",  1.2,  1356.0, 3230.0, 8.229),
        Element(66,  "Dy", "Dysprosium",   "Dysprosium",   162.50,  PeriodicGroup.LANTHANIDE,      6, 0,  "s",  1.22, 1412.0, 2567.0, 8.55),
        Element(67,  "Ho", "Holmium",      "Holmium",      164.93,  PeriodicGroup.LANTHANIDE,      6, 0,  "s",  1.23, 1474.0, 2700.0, 8.795),
        Element(68,  "Er", "Erbium",       "Erbium",       167.26,  PeriodicGroup.LANTHANIDE,      6, 0,  "s",  1.24, 1529.0, 2868.0, 9.066),
        Element(69,  "Tm", "Thulium",      "Thulium",      168.93,  PeriodicGroup.LANTHANIDE,      6, 0,  "s",  1.25, 1545.0, 1950.0, 9.321),
        Element(70,  "Yb", "Ytterbium",    "Ytterbium",    173.05,  PeriodicGroup.LANTHANIDE,      6, 0,  "s",  1.1,  819.0, 1196.0, 6.965),
        Element(71,  "Lu", "Lutecium",     "Lutetium",     174.97,  PeriodicGroup.LANTHANIDE,      6, 0,  "s",  1.27, 1663.0, 3402.0, 9.840),
        Element(72,  "Hf", "Hafnium",      "Hafnium",      178.49,  PeriodicGroup.TRANSITION,      6, 4,  "s",  1.3,  2233.0, 4603.0, 13.31),
        Element(73,  "Ta", "Tantal",       "Tantalum",     180.95,  PeriodicGroup.TRANSITION,      6, 5,  "s",  1.5,  3017.0, 5458.0, 16.69),
        Element(74,  "W",  "Wolfram",      "Tungsten",     183.84,  PeriodicGroup.TRANSITION,      6, 6,  "s",  2.36, 3422.0, 5555.0, 19.25),
        Element(75,  "Re", "Rhenium",      "Rhenium",      186.21,  PeriodicGroup.TRANSITION,      6, 7,  "s",  1.9,  3186.0, 5596.0, 21.02),
        Element(76,  "Os", "Osmium",       "Osmium",       190.23,  PeriodicGroup.TRANSITION,      6, 8,  "s",  2.2,  3033.0, 5012.0, 22.59),
        Element(77,  "Ir", "Iridium",      "Iridium",      192.22,  PeriodicGroup.TRANSITION,      6, 9,  "s",  2.20, 2466.0, 4428.0, 22.56),
        Element(78,  "Pt", "Platina",      "Platinum",     195.08,  PeriodicGroup.TRANSITION,      6, 10, "s",  2.28, 1768.3, 3825.0, 21.45),
        Element(79,  "Au", "Zlato",        "Gold",         196.97,  PeriodicGroup.TRANSITION,      6, 11, "s",  2.54, 1064.18, 2856.0, 19.3),
        Element(80,  "Hg", "Rtuť",         "Mercury",      200.59,  PeriodicGroup.TRANSITION,      6, 12, "l",  2.00, -38.83, 356.73, 13.534),
        Element(81,  "Tl", "Thallium",     "Thallium",     204.38,  PeriodicGroup.POST_TRANSITION, 6, 13, "s",  1.62, 304.0, 1473.0, 11.85),
        Element(82,  "Pb", "Olovo",        "Lead",         207.2,   PeriodicGroup.POST_TRANSITION, 6, 14, "s",  2.33, 327.46, 1749.0, 11.34),
        Element(83,  "Bi", "Bismut",       "Bismuth",      208.98,  PeriodicGroup.POST_TRANSITION, 6, 15, "s",  2.02, 271.3, 1564.0, 9.78),
        Element(84,  "Po", "Polonium",     "Polonium",     209.0,   PeriodicGroup.POST_TRANSITION, 6, 16, "s",  2.0,  254.0, 962.0, 9.196),
        Element(85,  "At", "Astat",        "Astatine",     210.0,   PeriodicGroup.HALOGEN,         6, 17, "s",  2.2,  302.0, 337.0, 6.35),
        Element(86,  "Rn", "Radon",        "Radon",        222.0,   PeriodicGroup.NOBLE_GAS,       6, 18, "g",  2.2,  -71.0, -61.7, 0.00973),
        Element(87,  "Fr", "Francium",     "Francium",     223.0,   PeriodicGroup.ALKALI_METAL,    7, 1,  "s",  0.7,  27.0, 677.0, 1.87),
        Element(88,  "Ra", "Radium",       "Radium",       226.0,   PeriodicGroup.ALKALINE_EARTH,  7, 2,  "s",  0.9,  696.0, 1737.0, 5.5),
        Element(89,  "Ac", "Aktinium",     "Actinium",     227.0,   PeriodicGroup.ACTINIDE,        7, 0,  "s",  1.1,  1050.0, 3200.0, 10.07),
        Element(90,  "Th", "Thorium",      "Thorium",      232.04,  PeriodicGroup.ACTINIDE,        7, 0,  "s",  1.3,  1750.0, 4788.0, 11.724),
        Element(91,  "Pa", "Protaktinium", "Protactinium", 231.04,  PeriodicGroup.ACTINIDE,        7, 0,  "s",  1.5,  1572.0, 4000.0, 15.37),
        Element(92,  "U",  "Uran",         "Uranium",      238.03,  PeriodicGroup.ACTINIDE,        7, 0,  "s",  1.38, 1135.0, 4131.0, 19.1),
        Element(93,  "Np", "Neptunium",    "Neptunium",    237.0,   PeriodicGroup.ACTINIDE,        7, 0,  "s",  1.36, 644.0, 3902.0, 20.45),
        Element(94,  "Pu", "Plutonium",    "Plutonium",    244.0,   PeriodicGroup.ACTINIDE,        7, 0,  "s",  1.28, 639.4, 3228.0, 19.816),
        Element(95,  "Am", "Americium",    "Americium",    243.0,   PeriodicGroup.ACTINIDE,        7, 0,  "s",  1.3,  1176.0, 2011.0, 12.0),
        Element(96,  "Cm", "Curium",       "Curium",       247.0,   PeriodicGroup.ACTINIDE,        7, 0,  "s",  1.3,  1345.0, 3100.0, 13.51),
        Element(97,  "Bk", "Berkelium",    "Berkelium",    247.0,   PeriodicGroup.ACTINIDE,        7, 0,  "s",  1.3,  986.0, Double.NaN, 14.78),
        Element(98,  "Cf", "Kalifornium",  "Californium",  251.0,   PeriodicGroup.ACTINIDE,        7, 0,  "s",  1.3,  900.0, Double.NaN, 15.1),
        Element(99,  "Es", "Einsteinium",  "Einsteinium",  252.0,   PeriodicGroup.ACTINIDE,        7, 0,  "s",  1.3,  860.0, Double.NaN, 8.84),
        Element(100, "Fm", "Fermium",      "Fermium",      257.0,   PeriodicGroup.ACTINIDE,        7, 0,  "s",  1.3,  1527.0, Double.NaN, Double.NaN),
        Element(101, "Md", "Mendelevium",  "Mendelevium",  258.0,   PeriodicGroup.ACTINIDE,        7, 0,  "s",  1.3,  827.0, Double.NaN, Double.NaN),
        Element(102, "No", "Nobelium",     "Nobelium",     259.0,   PeriodicGroup.ACTINIDE,        7, 0,  "s",  1.3,  827.0, Double.NaN, Double.NaN),
        Element(103, "Lr", "Lawrencium",   "Lawrencium",   262.0,   PeriodicGroup.ACTINIDE,        7, 0,  "s",  1.3,  1627.0, Double.NaN, Double.NaN),
        Element(104, "Rf", "Rutherfordium","Rutherfordium",267.0,   PeriodicGroup.TRANSITION,      7, 4,  "?",  0.0,  Double.NaN, Double.NaN, Double.NaN),
        Element(105, "Db", "Dubnium",      "Dubnium",      268.0,   PeriodicGroup.TRANSITION,      7, 5,  "?",  0.0,  Double.NaN, Double.NaN, Double.NaN),
        Element(106, "Sg", "Seaborgium",   "Seaborgium",   269.0,   PeriodicGroup.TRANSITION,      7, 6,  "?",  0.0,  Double.NaN, Double.NaN, Double.NaN),
        Element(107, "Bh", "Bohrium",      "Bohrium",      270.0,   PeriodicGroup.TRANSITION,      7, 7,  "?",  0.0,  Double.NaN, Double.NaN, Double.NaN),
        Element(108, "Hs", "Hassium",      "Hassium",      269.0,   PeriodicGroup.TRANSITION,      7, 8,  "?",  0.0,  Double.NaN, Double.NaN, Double.NaN),
        Element(109, "Mt", "Meitnerium",   "Meitnerium",   278.0,   PeriodicGroup.UNKNOWN,         7, 9,  "?",  0.0,  Double.NaN, Double.NaN, Double.NaN),
        Element(110, "Ds", "Darmstadtium", "Darmstadtium", 281.0,   PeriodicGroup.UNKNOWN,         7, 10, "?",  0.0,  Double.NaN, Double.NaN, Double.NaN),
        Element(111, "Rg", "Roentgenium",  "Roentgenium",  282.0,   PeriodicGroup.UNKNOWN,         7, 11, "?",  0.0,  Double.NaN, Double.NaN, Double.NaN),
        Element(112, "Cn", "Kopernicium",  "Copernicium",  285.0,   PeriodicGroup.POST_TRANSITION, 7, 12, "?",  0.0,  Double.NaN, Double.NaN, Double.NaN),
        Element(113, "Nh", "Nihonium",     "Nihonium",     286.0,   PeriodicGroup.UNKNOWN,         7, 13, "?",  0.0,  Double.NaN, Double.NaN, Double.NaN),
        Element(114, "Fl", "Flerovium",    "Flerovium",    289.0,   PeriodicGroup.UNKNOWN,         7, 14, "?",  0.0,  Double.NaN, Double.NaN, Double.NaN),
        Element(115, "Mc", "Moscovium",    "Moscovium",    290.0,   PeriodicGroup.UNKNOWN,         7, 15, "?",  0.0,  Double.NaN, Double.NaN, Double.NaN),
        Element(116, "Lv", "Livermorium",  "Livermorium",  293.0,   PeriodicGroup.UNKNOWN,         7, 16, "?",  0.0,  Double.NaN, Double.NaN, Double.NaN),
        Element(117, "Ts", "Tennessin",    "Tennessine",   294.0,   PeriodicGroup.UNKNOWN,         7, 17, "?",  0.0,  Double.NaN, Double.NaN, Double.NaN),
        Element(118, "Og", "Oganesson",    "Oganesson",    294.0,   PeriodicGroup.UNKNOWN,         7, 18, "?",  0.0,  Double.NaN, Double.NaN, Double.NaN)
    )
}
