package com.ritesh.cashiro.presentation.common.icons

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import com.ritesh.cashiro.R
import com.ritesh.cashiro.utils.IconResolutionUtils
import com.ritesh.cashiro.data.database.entity.CategoryEntity
import com.ritesh.cashiro.data.database.entity.SubcategoryEntity
import java.util.Locale

/**
 * Centralized category mapping system for consistent categorization
 * across transactions and subscriptions
 */
object CategoryMapping {

    data class CategoryInfo(
        val displayName: String,
        val iconResId: Int,
        val color: Color,
        val fallbackIcon: ImageVector = Icons.Default.Category
    )

    // --- helpers ---
    private val regexCache = mutableMapOf<String, Regex>()

    private fun matches(
        merchantRaw: String,
        anyOf: Set<String>,
        noneOf: Set<String> = emptySet()
    ): Boolean {
        val m = merchantRaw.lowercase(Locale.ROOT)
        if (noneOf.any { m.contains(it) }) return false

        // Check for exact matches or word boundaries
        return anyOf.any { keyword ->
            // For single word keywords, check word boundaries
            if (!keyword.contains(" ")) {
                val regex = regexCache.getOrPut(keyword) {
                    "\\b${keyword.replace(".", "\\.")}\\b".toRegex()
                }
                regex.containsMatchIn(m)
            } else {
                // For multi-word keywords, use contains as before
                m.contains(keyword)
            }
        }
    }

    // --- keyword sets (single source of truth) ---
    private val FOOD = setOf(
        "swiggy",
        "zomato",
        "dominos",
        "pizza",
        "burger",
        "kfc",
        "mcdonalds",
        "restaurant",
        "cafe",
        "food",
        "starbucks",
        "haldiram",
        "barbeque",

        //Thailand
        "foodpanda thailand",
        "linepay",
        "arabica",
        "chatramue",
        "after you",
        "kyo roll en",
        "roast",
        "al saray",
        "oh my juice",

        //Careem delivery
        "careem food",
        "careem dineout",
        "deliveroo",
        "talabat",

        //Dubai / UAE foods
        "peets",
        "safadi",
        "gazebo",
        "al baik",
        "jollibee",
        "raising canes",
        "chipotle",
        "kitopi",
        "sangeetha",
        "maharaja bhog",
        "al beiruti",
        "malabar tiffin house",
        "calicut paragon",
        "bikanervala",
        "karak house",
        "puranmal",
        "chicking",
        "papa johns",
        "phosphorus",
        "rang indian",

        // --- Newly Added Merchants ---
        "moishi",
        "cravings",
        "the matcha tokyo",
        "shawarma emprator",
        "shawrma alemprator",
        "tabaq alhejazi",
        "mehfil biriyani",
        "desert shawarma",
        "mr tea",
        "papparoti",
        "samak alhejazi",
        "fresh cookies corner",
        "trucillo",
        "p.f. chang's",
        "neychor kada",
        "salt",
        "koob al gahwa",
        "tanuki",
        "asiankitchen",
        "bkry",
        "nguyen cimit",
        "miyabi",
        "tashas",
        "desi village",
        "vietnamese",
        "firas al diyafa",
        "manooshe",
        "awani",
        "sultan saray",
        "pincode",
        "commonground",
        "nala",
        "bombay bungalow",
        "punjab by amritsr",
        "the daily",
        "subway",
        "wagamama",
        "caffe nero",
        "fresh and tasty",
        "alkanz lakeshore",
        "cafe bateel",
        "bateel",
        "hutong",
        "asiakitchen",
        "asia kitchen",
        "tbk",
        "smoked meat world",
        "nandos",
        "nando's",
        "kyochon",
        "tarbush",
        "laderach",
        "chagee",
        "chica bonita",
        "saffron & spices",
        "al amar",
        "amici",
        "baan india",
        "ginger farm kitchen",
        "bianca",
        "biancaitalian",
        "plantiful",
        "pacamara",
        "pacamara-qsncc",
        "pacamara-one bangkok",
        "dean and deluca",
        "ohkajhu",
        "getfresh",
        "getfresh-exchange tower",
        "getfresh-samyan mitrtown",
        "getfresh-empire tower",
        "bowlito",
        "james boulangerie",
        "chester's",
        "bonchon",
        "the bibimbab",
        "guljak topokki",
        "dough bros",
        "oakberry acai",
        "wwa",
        "w w a",
        "shaloba",
        "jharoka",
        "ghljharoka",
        "ksher akaraskyroofto",
        "akaraskyrooftobangkok",
        "royce",
        "godiva",
        "benlai one bangkok", // confectionery/cafe style

        // Must-add from earlier (confirmed)
        "piri piri flaming grill",
        "naixue",
        "bn-icon siam",

        // New adds for the 133 unparsed (grouped)
        "pf changs",
        "p f changs",
        "al shayapf changs",
        "docg",
        "dark-emquartier",
        "chuan kitchen",
        "heytea",
        "jasons deli",
        "paris baguette",
        "pezzo",
        "origin+bloom",
        "origin and bloom",
        "punjab grill",
        "shaans north indian",
        "shaan north indian",
        "sanchos paragon",
        "sanchos",
        "mitsukoshi depachika",
        "oriental gourmet",
        "the oriental gourmet",
        "bosporus",
        "leto",

        // Google-prefixed eateries
        "google nomadtable",
        "google tantan asian",
        "google viki asian dra",
        "alsafadi",
        "alsafadi restaurant",
        "awfully chocolate",
        "boon coffee",
        "cafe bateel",
        "cafebateel",
        "cafobateel",
        "coffee club",
        "commonground",
        "cps coffee",
        "french spirit coffee",
        "hashmi bhatti",
        "hili coffee house",
        "i coffee",
        "kcal",
        "line man",
        "mado",
        "masala xpress",
        "mcdonald's",
        "muin sukhumvit 63",
        "nua tair",
        "origin+bloom",
        "pescado seafood grill",
        "piri piri flaming gril",
        "pincode",
        "roasters coffee house",
        "salkara",
        "the coffee lab",
        "top cafeteria",
        "bosporus sharjah"
    )

    private val GROCERY = setOf(
        "bigbasket",
        "blinkit",
        "zepto",
        "grofers",
        "jiomart",
        "dmart",
        "reliance fresh",
        "more",
        "grocery",
        "dunzo",
        "careem groceries",
        "careem quik",
        // Dubai / uae grocieries
        "carrefour",
        "spinneys",
        "lulu",
        "choithrams",
        "waitrose",
        "geant",
        "union coop",
        "abu dhabi co-op",
        "emirates cooperative",
        "nesto",
        "almaya",
        "rawabi",
        "safeer",

        // --- Newly Added Merchants ---
        "hippo box",
        "247 corner",
        "new era super market",
        "baqala",
        "lebanese fruit co",
        "al tayeb meat",
        "west zone fresh",
        "majid al futtaim hypermarket",
        "all day mini",
        "all day plus",
        "fresh good day",
        "al ghabat city",
        "zoom",
        "zoom site",
        "all day",
        "all day advantage mini",
        "noon minutes",
        "crrefour",
        "maf mkt",
        "jaya grocer",
        "jaya grocer-intermark",
        "villa market",
        "villa market-gaysorn",
        "villa market-lang suan",
        "gourmet market",
        "gourmet market emporium",
        "tops",
        "tops-central world",
        "mini big c",
        "mini big c-soi prachum",
        "7-11",
        "7-11 sukhumvit22",
        "7-11 one bangkok",
        "7-11 icon siam",
        "7-11 park venture",
        "7-11 maneeya chitlom",
        "donki",
        "donki mall",
        "donki mall thonglor",
        // New adds for the 133 unparsed (grouped)
        "guardian",
        "guardian paragon",
        "craft minimart",
        "foodland",
        "luluhypermarket",
        "majid al futtaim hypmk",
        "majid al futtaim hypm"

    )

    private val TRANSPORT = setOf(
        "uber", "ola", "rapido", "metro", "irctc", "redbus", "makemytrip",
        "goibibo", "petrol", "fuel", "parking", "toll", "fastag",
        "indigo", "air india", "spicejet", "vistara", "cleartrip",
        "careem ride", "careem hala ride", "yango",
        //Thailand
        "www.grab.com",

        //Malaysia
        "grab rides",
        "pyxbolt services",
        "bolt services",

        //Dubai gas stations
        "emarat", "adnoc", "enoc", "epc", "dolphin energy",

        // --- Newly Added Merchants ---
        "careem", // Generic fallback
        "farid car park",
        "big boss rent a car",
        "presidential transport",
        "q mobility",
        "valtrans",
        "eppco", "eppco site", "eppco-site", "tasjeel", "integrated transport",
        "grab-ec", "grabtaxi", "bolt", "mrt-bem", "airports of thailand",
        "sri rat expressway", "chalerm maha nakhon expressway", "expressway",
        "opntrueiservicetopup", "opn true iservice topup",
        "shell", "shell 0071f", "petaling jaya grab",

        // New adds for the 133 unparsed (grouped)
        "opvn bike tour",
        "acv noi bai",
        "noi bai",
        "grab a-",
        "grab 5-",
        "sats",
        "sats t1",
        "sats t1i4",
        "vnpay",
        "vnpaytram",
        "chalerm maha nakhon ex",
        "expressway",
        "point sbia",
        "ptt",
        "pttst.c",
        "smart auto"

    )

    private val SHOPPING = setOf(
        "amazon", "flipkart", "myntra", "ajio", "nykaa", "meesho",
        "snapdeal", "shopclues", "firstcry", "pepperfry", "urban ladder",
        "store", "mart"
    )
    private val SHOPPING_EXCLUDE = setOf("jiomart", "dmart")

    // --- Newly Added Merchants ---
    private val SHOPPING_EXTENDED = setOf(
        "paypal",
        "gmg consumer",
        "bloomingdales",
        "dubizzle",
        "veda inc investment",
        "al futtaim trading",
        "tipr tech",
        "jumbo electronics",
        "shake shack",
        "home centre",
        "qissat al oud",
        "citywalk retail",
        "ova accessories",
        "the sport shack",
        "dubai duty free",
        "level shoes",
        "balmain",
        "zara",
        "daikan",
        "king koil",
        "asas auto accessories",
        "easy blinds",
        "dubai furniture",
        "whsmith",
        "brands for less",
        "sharaf dg",
        "american eagle",
        "dufry",
        "a j a international",
        "aja international",
        "noon e-commerce",
        "noon",
        "m h al shaya",
        "beside trading",
        "intelligent oud",
        "qissat aloud perfumes",
        "uniqlo",
        "uniqlo trx",
        "h&m",
        "sephora",
        "louis vuitton",
        "gucci",
        "chanel",
        "prada",
        "tiffany & co",
        "burberry",
        "hermes",
        "cartier",
        "dior",
        "fendi",
        "versace",
        "michael kors",
        "coach",
        "tory burch",
        "ralph lauren",
        "armani",
        "adidas",
        "nike",
        "puma",
        "reebok",
        "under armour",
        "lululemon",
        "the north face",
        "godiva pavilion",
        "skechers",
        "urban revivo",
        "owndays",
        "it city",
        "it city centralworld",
        "crc sports",
        "sony (central world)",
        "apple central world",
        "lazada limited",
        "www.2c2p.comlazada",
        "2c2p lazada",
        "www.2c2p.com lazada",
        "sephora (thailand)",
        "eveandboy",
        "eveandboy/gaysorn",

        // New adds for the 133 unparsed (grouped)
        "istudio",
        "istudio-siam paragon",
        "wh smith singapore",
        "lyn",
        "fiverr",
        "select media",
        "adobe",
        "adobe.com",
        "lastpass",
        "lastpass.com",
        "openai chatgpt",
        "openai",
        "cursor",
        "cursor usage",
        "docg pte ltd",
        "blu intelligent solutions",
        "gajeto-23",
        "sukhumvit city mall",
        "the emsphere",
        "central world",
        "emquartier",
        "iconsiam",
        "central worl",
        "angelic aroma",
        "blu intelligent solut",
        "lril",
        "siam paragon dept",
        "siam paragon sup",
        "smartordering",
        "smartordering technolo",
        "space hub general ware",
        "virgin megastore",
        "empire tower",
        "the empire tower",

        )

    private val UTILITIES = setOf(
        "electricity",
        "water",
        "gas",
        "broadband",
        "wifi",
        "internet",
        "tata sky",
        "dish",
        "d2h",
        "bill",
        "tata power",
        "adani",
        "bses",
        "act fibernet",

        // --- Newly Added Merchants ---
        "sdgdubaipay",
        "careem plus",
        "noqejari",
        "saya",
        "aljada developments",
        "nshama",
        "tasleem",
        "tasleem address harbour",
        "greenland environment",
        "noq south energy",
        "noqsouth energy dwc",

        // New adds for the 133 unparsed (grouped)
        "tamdeed projects",
        "smart dubai government",
        "paysolut",


    )

    private val ENTERTAINMENT = setOf(
        "netflix", "spotify", "prime", "hotstar", "sony liv", "zee5",
        "voot", "youtube", "cinema", "pvr", "inox", "bookmyshow",
        "gaana", "jiosaavn", "apple music", "wynk",

        // --- Newly Added Merchants ---
        "global village",
        "major cineplex",
        "www.majorcineplex.com",
        "2c2pmajor cineplex",
        "2c2p major cineplex",
        "major 1066",
        "major 1126",
        "ticketmelon",
        "www.2c2p ticketmelon",
        "www.2cticketmelon",
        "sfcinemacity",
        "platinumlist.net"
    )

    private val HEALTHCARE = setOf(
        "1mg", "pharmeasy", "netmeds", "apollo", "pharmacy", "medical",
        "hospital", "clinic", "doctor", "practo", "healthkart", "truemeds",

        // --- Newly Added Merchants ---
        "ascent e n t",
        "watson",
        "dr nutrition",
        "boots",
        "supercare",
        "life pharmacy",
        "life phy",
        "life pharm",
        "deira life pharm",
        "medex",
        "bumrungrad", // (for getfresh-bumrungrad receipt context)
        "watsons",
        "lac nutrition for life",
        "mediclinic al sufouh",
        "boots_",
        "boots_4287 c.world 3 fbangkok th"
    )

    private val INVESTMENT = setOf(
        "groww", "zerodha", "upstox", "kuvera", "paytm money", "coin",
        "smallcase", "mutual fund", "sip", "angel", "5paisa", "etmoney"
    )

    private val BANKING = setOf(
        "hdfc", "icici", "axis", "sbi", "kotak", "bank", "loan", "emi",
        "credit card", "yes bank", "idfc", "indusind", "pnb", "canara", "union bank", "rbl", "prime commercial bank",

        // --- Newly Added Merchants ---
        "atm withdrawal",
        "bank transfer",
        "bank account operation",
        "bank cheque operation",
        "bank cash operation",
        "my fatoorah",
        "cash withdrawal",
        "cash deposit",
        "refund",
        "outward remittance",
        "inward remittance",
        "transfer to",
        "transfer from",
        "transfer: "
    )

    private val PERSONAL_CARE = setOf(
        "urban company", "salon", "spa", "barber", "beauty", "grooming", "housejoy",

        // --- Newly Added Merchants ---
        "green belt cleaning",
        "magic washer laundry",
        "stile di capelli",
        "q2 general cleaning",
        "ahmed sammie",
        "clean car washers",
        "italiano style men",
        "high level car wash",
        "home care",
        "final touch cleaning",
        "drip n dry",
        "careem homeservices",
        "skin iii",
        "truefitt and hill",

        // New adds for the 133 unparsed (grouped)
        "sultans of shave",
        "mandarin oriental spa",
        "black amber sathorn",
        "o.c.c.black amber sathorn",
        "easy way laundry",
        "splice barbershop",
        "mandarin oriental spa",
        "o.c.c.black amber sath",
        "o.c.c.public co.,ltd",
        "phetsathorn"

    )

    private val EDUCATION = setOf(
        "byju", "unacademy", "vedantu", "coursera", "udemy", "upgrade",
        "school", "college", "university", "toppr", "udacity", "simplilearn",
        "whitehat", "great learning"
    )

    private val MOBILE = setOf(
        "airtel", "jio", "vodafone", "idea", "bsnl", "recharge", "prepaid", "postpaid", "mobile",

        // --- Newly Added Merchants ---
        "etisalat"
    )

    private val FITNESS = setOf(
        "cult",
        "gym",
        "fitness",
        "yoga",
        "healthifyme",
        "fitternity",
        "gold's gym",
        "anytime fitness"
    )

    private val INSURANCE = setOf(
        "insurance", "lic", "policy", "hdfc life", "icici pru", "sbi life",
        "max life", "bajaj allianz", "policybazaar", "acko", "digit"
    )

    private val TAX = setOf(
        "tin", "tax information", "income tax", "gst", "tax payment", "challan",
        "direct tax", "indirect tax", "tax deducted", "tds", "advance tax", "self assessment",

        // --- Newly Added Merchants ---
        "abu dhabi judicial dept",
        "sharjah finance dept",
        "tassheel",
        "dubai courts",
        "ministry of interior",

        // Must-add from earlier (confirmed)
        "abu dhabi judicial dep",
        "sharjah finance depart",
        "smartdxbgov-ded"

    )

    private val BANK_CHARGE = setOf(
        "recovery", "charge", "fee", "penalty", "maintenance", "non-maintenance",
        "minimum balance", "sms charge", "atm recovery", "service charge",
        "annual fee", "processing fee", "convenience fee", "late payment",
        "cheque returned"
    )

    private val CC_PAYMENT = setOf(
        "bbps", "bill payment", "credit card payment", "cc payment", "card payment"
    )

    // Travel keywords (grouped for readability)
    private val TRAVEL = setOf(
        // --- OTAs / Meta / Booking platforms ---
        "make my trip",
        "yatra",
        "ixigo",
        "booking.com",
        "expedia",
        "agoda",
        "trip.com",
        "trivago",
        "hotels.com",
        "kayak",
        "travelocity",
        "airbnb",
        "vrbo",
        "skyscanner",
        "momondo",
        "tripadvisor",

        //Thailand hotels
        "w bangkok",
        "bangkok marriott",

        // --- Generic travel terms ---
        "flight",
        "airline",
        "hotel",

        // --- Hotel chains ---
        "marriott",
        "hyatt",
        "hilton",
        "accor",

        // --- Indian premium / luxury ---
        "taj",
        "oberoi",
        "itc hotels",
        "leela",

        // Dubai / UAE Brands
        "jumeirah",
        "address hotels",
        "address grand",
        "palace downtown",
        "burj al arab",
        "one&only",
        "five luxe",
        "five palm jumeirah",
        "atlantis the palm",
        "atlantis the royal",
        "anantara the palm",
        "vida downtown",
        "vida dubai creek",
        "vida emirates hills",


        //Abu Dhabi Brands
        "emirates palace",

        // --- Global premium chains ---
        "radisson",
        "sheraton",
        "westin",
        "ritz carlton",
        "four seasons",
        "conrad",
        "st regis",
        "jw marriott",
        "grand hyatt",
        "le meridien",
        "waldorf astoria",
        "intercontinental",
        "fairfield",
        "holiday inn express",
        "hampton by hilton",
        "doubletree by hilton",
        "courtyard by marriott",
        "residence inn",
        "homewood suites",
        "aloft",
        "element by westin",
        "the edition",
        "tribe living",
        "s/o uptown",
        "moxy",
        "four points by sherato",

        // --- Mid-range / business / budget brands ---
        "doubletree",
        "holiday inn",
        "novotel",
        "mercure",
        "ibis",
        "fairmont",
        "sofitel",
        "pullman",
        "movenpick",
        "citadines",

        // --- Budget aggregators (India & Asia) ---
        "oyo",
        "treebo",
        "fabhotels",

        // --- Ultra-luxury / boutique brands ---
        "signiel",
        "aman",
        "aman resorts",
        "anantara",
        "banyan tree",
        "six senses",
        "rosewood",
        "capella",
        "kempinski",
        "kimpton maa-lai",
        "the ritz-carlton reserve",
        // --- International airlines (selected) ---
        "ryanair",
        "lufthansa",
        "emirates",
        "qatar airways",
        "british airways",
        "air france",
        "klm",
        "singapore airlines",
        "etihad airways",
        "turkish airlines",
        "cathay pacific",
        "ana",

        // --- US carriers ---
        "alaska airlines",
        "hawaiian airlines",
        "southwest airlines",
        "jetblue",
        "allegiant air",
        "spirit airlines",

        // --- Newly Added Merchants ---
        "vfs global",
        "dubai world trade centre",
        "dubai world trade cent",
        "vfs uk",
        "vfs world trade centre",
        "vfs wtc",
        "address downtown",
        "the address downtown",
        "the meydan",
        "crowne plaza",
        "crowne plaza klcc",
        "four points by sheraton",
        "sindhorn kempinski",
        "st.regis bangkok",
        "the st.regis bangkok",
        "dusit thani bangkok",

        // New adds for the 133 unparsed (grouped)
        "staybridge",
        "park silom",
        "the parq",
        "marina bay sands",
        "mbs front office",
        "w singapore",
        "gardens by the bay",
        "gardens by the bay-ret",
        "gardens by the bay-tic",
        "king power mahanakhon",
        "al reyadah hospitality",
        "four points by sheraton",
        "king group hospitality",
        "magnolias serviced",
        "magnoliasservicedrbangkok"
    )

    // --- single ordered rule list (priority preserved) ---
    private data class Rule(
        val categoryName: String,
        val includes: Set<String>,
        val excludes: Set<String> = emptySet()
    )

    private val RULES: List<Rule> = listOf(
        Rule("Tax", TAX),
        Rule("Hidden Charges", BANK_CHARGE),
        Rule("Credit Bill", CC_PAYMENT),
        Rule("Food & Drinks", FOOD),
        Rule("Groceries", GROCERY),
        Rule("Transport", TRANSPORT),
        Rule("Shopping", SHOPPING + SHOPPING_EXTENDED, SHOPPING_EXCLUDE),
        Rule("Bill", UTILITIES),
        Rule("Entertainment", ENTERTAINMENT),
        Rule("Medical", HEALTHCARE),
        Rule("Investment", INVESTMENT),
        Rule("Business", BANKING),
        Rule("Personal", PERSONAL_CARE),
        Rule("Children", EDUCATION),
        Rule("Top-up", MOBILE),
        Rule("Fitness", FITNESS),
        Rule("Insurance", INSURANCE),
        Rule("Travel", TRAVEL),
    )

    //find duplicates in each rule and print it from RULES
    public fun findDuplicateKeywords(): Set<String> {
        val duplicates = RULES.map { rule -> rule.includes + rule.excludes }.flatten().groupingBy { it }.eachCount()
            .filter { it.value > 1 }.keys
        if (duplicates.isNotEmpty()) {
            println("Duplicate keywords found across categories: $duplicates")
        }
        return duplicates
    }

    // Define all categories with their visual properties
    val categories = mapOf(
        "Food & Drinks" to CategoryInfo(
            displayName = "Food & Drinks",
            iconResId = R.drawable.type_food_stuffed_flatbread,
            color = Color(0xFFFC8019),
        ),
        "Transport" to CategoryInfo(
            displayName = "Transport",
            iconResId = R.drawable.type_travel_transport_airplane,
            color = Color(0xFF0066CC),
        ),
        "Shopping" to CategoryInfo(
            displayName = "Shopping",
            iconResId = R.drawable.type_shopping_shopping_bags,
            color = Color(0xFF893BBE),
        ),
        "Groceries" to CategoryInfo(
            displayName = "Groceries",
            iconResId = R.drawable.type_groceries_bread,
            color = Color(0xFF9E7155),
        ),
        "Home" to CategoryInfo(
            displayName = "Home",
            iconResId = R.drawable.type_event_and_place_house,
            color = Color(0xFFFFC107),
        ),
        "Entertainment" to CategoryInfo(
            displayName = "Entertainment",
            iconResId = R.drawable.type_snack_popcorn,
            color = Color(0xFFCC1A56),
        ),
        "Events" to CategoryInfo(
            displayName = "Events",
            iconResId = R.drawable.type_event_and_place_party_popper,
            color = Color(0xFF9C27B0),
        ),
        "Travel" to CategoryInfo(
            displayName = "Travel",
            iconResId = R.drawable.type_travel_transport_luggage,
            color = Color(0xFF0066CC),
        ),
        "Medical" to CategoryInfo(
            displayName = "Medical",
            iconResId = R.drawable.type_health_pill,
            color = Color(0xFFFF0041),
        ),
        "Personal" to CategoryInfo(
            displayName = "Personal",
            iconResId = R.drawable.type_tool_electronic_scissors,
            color = Color(0xFF9C27B0),
        ),
        "Fitness" to CategoryInfo(
            displayName = "Fitness",
            iconResId = R.drawable.type_sports_baseball,
            color = Color(0xFF91CC4D),
        ),
        "Services" to CategoryInfo(
            displayName = "Services",
            iconResId = R.drawable.type_tool_electronic_high_voltage,
            color = Color(0xFFFF9800),
        ),
        "Bill" to CategoryInfo(
            displayName = "Bill",
            iconResId = R.drawable.type_travel_transport_admission_tickets,
            color = Color(0xFFFF0041),
        ),
        "Subscription" to CategoryInfo(
            displayName = "Subscription",
            iconResId = R.drawable.type_tool_electronic_clapper_board,
            color = Color(0xFF3F51B5),
        ),
        "EMI" to CategoryInfo(
            displayName = "EMI",
            iconResId = R.drawable.type_travel_transport_automobile,
            color = Color(0xFFFF0041),
        ),
        "Credit Bill" to CategoryInfo(
            displayName = "Credit Bill",
            iconResId = R.drawable.type_stationary_card_file_box,
            color = Color(0xFFFF9800),
        ),
        "Investment" to CategoryInfo(
            displayName = "Investment",
            iconResId = R.drawable.type_flower_and_tree_herb,
            color = Color(0xFF91CC4D),
        ),
        "Support" to CategoryInfo(
            displayName = "Support",
            iconResId = R.drawable.type_health_stethoscope,
            color = Color(0xFF673AB7),
        ),
        "Insurance" to CategoryInfo(
            displayName = "Insurance",
            iconResId = R.drawable.type_health_mending_heart,
            color = Color(0xFFFF0041),
        ),
        "Tax" to CategoryInfo(
            displayName = "Tax",
            iconResId = R.drawable.type_finance_chart_decreasing,
            color = Color(0xFFFF5722),
        ),
        "Top-up" to CategoryInfo(
            displayName = "Top-up",
            iconResId = R.drawable.type_finance_money_bag,
            color = Color(0xFFFF9800),
        ),
        "Children" to CategoryInfo(
            displayName = "Children",
            iconResId = R.drawable.type_fruit_kiwi_fruit,
            color = Color(0xFF8BC34A),
        ),
        "Pet Care" to CategoryInfo(
            displayName = "Pet Care",
            iconResId = R.drawable.type_animal_dog_face,
            color = Color(0xFFF44336),
        ),
        "Business" to CategoryInfo(
            displayName = "Business",
            iconResId = R.drawable.type_finance_classical_building,
            color = Color(0xFF795548),
        ),
        "Miscellaneous" to CategoryInfo(
            displayName = "Miscellaneous",
            iconResId = R.drawable.type_stationary_clipboard,
            color = Color(0xFF91CC4D),
        ),
        "Self Transfer" to CategoryInfo(
            displayName = "Self Transfer",
            iconResId = R.drawable.type_finance_bank,
            color = Color(0xFF795548),
        ),
        "Savings" to CategoryInfo(
            displayName = "Savings",
            iconResId = R.drawable.type_sports_bullseye,
            color = Color(0xFFFF0041),
        ),
        "Gift" to CategoryInfo(
            displayName = "Gift",
            iconResId = R.drawable.type_stationary_wrapped_gift,
            color = Color(0xFFFF5722),
        ),
        "Lent" to CategoryInfo(
            displayName = "Lent",
            iconResId = R.drawable.type_finance_money_with_wings,
            color = Color(0xFF4CAF50),
        ),
        "Donation" to CategoryInfo(
            displayName = "Donation",
            iconResId = R.drawable.type_health_drop_of_blood,
            color = Color(0xFFFF4081),
        ),
        "Hidden Charges" to CategoryInfo(
            displayName = "Hidden Charges",
            iconResId = R.drawable.type_animal_goblin,
            color = Color(0xFFF44336),
        ),
        "Cash Withdrawal" to CategoryInfo(
            displayName = "Cash Withdrawal",
            iconResId = R.drawable.type_finance_dollar_banknote,
            color = Color(0xFF8BC34A),
        ),
        "Return" to CategoryInfo(
            displayName = "Return",
            iconResId = R.drawable.type_finance_currency_exchange,
            color = Color(0xFF33B5E5),
        ),
        "Salary" to CategoryInfo(
            displayName = "Salary",
            iconResId = R.drawable.type_finance_dollar_banknote,
            color = Color(0xFF8BC34A),
        )
    )

    /**
     * Get category for a merchant name (unified logic)
     */
    fun getCategory(merchantName: String): String {
        val merchantLower = merchantName.lowercase(Locale.ROOT)
        for (rule in RULES) {
            if (matches(merchantLower, rule.includes, rule.excludes)) {
                return rule.categoryName
            }
        }
        return "Miscellaneous"
    }
}

/**
 * Icon provider with fallback mechanism
 */
object IconProvider {

    /**
     * Get icon with full fallback chain for transactions
     * Priority: Brand Icon > Subcategory Icon > Category Icon > CategoryMapping > Miscellaneous
     */
    fun getIconForTransaction(
        context: Context? = null,
        merchantName: String,
        categoryEntity: CategoryEntity? = null,
        subcategoryEntity: SubcategoryEntity? = null,
        category: String? = null,
        subcategory: String? = null,
        accountIconResId: Int = 0,
        accountIconName: String? = null
    ): IconResource {
        // brand icon first
        BrandIcons.getIconResource(merchantName)?.let { iconRes ->
            return IconResource.DrawableResource(iconRes)
        }

        // subcategory icon
        subcategoryEntity?.let { subcat ->
            val resId = if (!subcat.iconName.isNullOrEmpty()) {
                IconResolutionUtils.nameToResId(context, subcat.iconName)
            } else {
                IconResolutionUtils.getSafeResId(context ?: return@let null, subcat.iconResId, 0)
            }

            if (resId != 0) {
                return IconResource.TintedResIcon(
                    resId = resId,
                    tint = Color(android.graphics.Color.parseColor(subcat.color))
                )
            }
        }

        // subcategory string fallback
        subcategory?.let { subcatName ->
            CategoryMapping.categories[subcatName]?.let { categoryInfo ->
                return IconResource.TintedResIcon(
                    resId = categoryInfo.iconResId,
                    tint = categoryInfo.color
                )
            }
        }

        // category icon from entity
        categoryEntity?.let { cat ->
            val resId = if (!cat.iconName.isNullOrEmpty()) {
                IconResolutionUtils.nameToResId(context, cat.iconName)
            } else {
                IconResolutionUtils.getSafeResId(context ?: return@let null, cat.iconResId, 0)
            }

            if (resId != 0) {
                return IconResource.TintedResIcon(
                    resId = resId,
                    tint = Color(android.graphics.Color.parseColor(cat.color))
                )
            }
        }

        // category string fallback
        category?.let { catName ->
            CategoryMapping.categories[catName]?.let { categoryInfo ->
                return IconResource.TintedResIcon(
                    resId = categoryInfo.iconResId,
                    tint = categoryInfo.color
                )
            }
        }

        // CategoryMapping using the transaction's category name (if available)
        // This handles built-in categories that don't have iconResId set in the entity
        categoryEntity?.let { cat ->
            CategoryMapping.categories[cat.name]?.let { categoryInfo ->
                return IconResource.TintedResIcon(
                    resId = categoryInfo.iconResId,
                    tint = categoryInfo.color
                )
            }
        }

        // account icon - Fallback
        if (!accountIconName.isNullOrEmpty()) {
            val resId = IconResolutionUtils.nameToResId(context, accountIconName)
            if (resId != 0) {
                return if (accountIconName.startsWith("ic_brand") || accountIconName.startsWith("ic_launcher") || accountIconName.startsWith("merchant_")) {
                    IconResource.DrawableResource(resId)
                } else {
                    IconResource.TintedResIcon(
                        resId = resId,
                        tint = Color.Unspecified
                    )
                }
            }
        }
        if (accountIconResId != 0) {
            val safeResId = IconResolutionUtils.getSafeResId(context ?: return@getIconForTransaction IconResource.TintedResIcon(
                resId = CategoryMapping.categories["Miscellaneous"]!!.iconResId,
                tint = CategoryMapping.categories["Miscellaneous"]!!.color
            ), accountIconResId, 0)
            
            if (safeResId != 0) {
                return IconResource.TintedResIcon(
                    resId = safeResId,
                    tint = Color.Unspecified // Use original colors of the resource
                )
            }
        }


        // Final fallback: Use Miscellaneous icon
        val categoryInfo = CategoryMapping.categories["Miscellaneous"]!!
        return IconResource.TintedResIcon(
            resId = categoryInfo.iconResId,
            tint = categoryInfo.color
        )
    }

    /**
     * Get color with full fallback chain for transactions
     * Priority: Brand Color → Subcategory Color → Category Color → Account Color
     * Returns: Pair of (colorHex, alpha) where brand colors have full opacity (1.0f)
     * and other colors have reduced opacity (0.2f)
     */
    fun getColorForTransaction(
        merchantName: String,
        categoryEntity: CategoryEntity? = null,
        subcategoryEntity: SubcategoryEntity? = null,
        category: String? = null,
        subcategory: String? = null,
        accountColorHex: String? = null
    ): Pair<String, Float>? {
        // brand color first - full opacity
        BrandIcons.getBrandColor(merchantName)?.let { return Pair(it, 1.0f) }

        // subcategory color - reduced opacity
        subcategoryEntity?.color?.let { return Pair(it, 0.2f) }

        // subcategory string fallback
        subcategory?.let { subcatName ->
            CategoryMapping.categories[subcatName]?.let { categoryInfo ->
                return Pair(String.format("#%06X", (0xFFFFFF and categoryInfo.color.toArgb())), 0.2f)
            }
        }

        // category color - reduced opacity
        categoryEntity?.color?.let { return Pair(it, 0.2f) }

        // category string fallback
        category?.let { catName ->
            CategoryMapping.categories[catName]?.let { categoryInfo ->
                return Pair(String.format("#%06X", (0xFFFFFF and categoryInfo.color.toArgb())), 0.2f)
            }
        }

        // account color fallback - reduced opacity
        accountColorHex?.let { return Pair(it, 0.2f) }

        // Return null to use default
        return null
    }

    /**
     * Get category info including icon and color
     */
    fun getCategoryInfo(merchantName: String): CategoryMapping.CategoryInfo {
        val category = CategoryMapping.getCategory(merchantName)
        return CategoryMapping.categories[category]
            ?: CategoryMapping.categories["Miscellaneous"]!!
    }
}

/**
 * Sealed class for different icon types
 */
sealed class IconResource {
    data class DrawableResource(val resId: Int) : IconResource()
    data class VectorIcon(val icon: ImageVector, val tint: Color) : IconResource()
    data class TintedResIcon(val resId: Int, val tint: Color) : IconResource()
}