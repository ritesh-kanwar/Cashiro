package com.ritesh.cashiro.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.Store
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Locale

/**
 * Centralized category mapping system for consistent categorization
 * across transactions and subscriptions
 */
object CategoryMapping {

    data class CategoryInfo(
        val displayName: String,
        val icon: ImageVector,
        val color: Color,
        val fallbackIcon: ImageVector = Icons.Default.Category
    )

    // --- helpers ---
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
                val regex = "\\b${keyword.replace(".", "\\.")}\\b".toRegex()
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
        "credit card", "yes bank", "idfc", "indusind", "pnb", "canara", "union bank", "rbl",

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
        Rule("Bank Charges", BANK_CHARGE),
        Rule("Credit Card Payment", CC_PAYMENT),
        Rule("Food & Dining", FOOD),
        Rule("Groceries", GROCERY),
        Rule("Transportation", TRANSPORT),
        Rule("Shopping", SHOPPING + SHOPPING_EXTENDED, SHOPPING_EXCLUDE),
        Rule("Bills & Utilities", UTILITIES),
        Rule("Entertainment", ENTERTAINMENT),
        Rule("Healthcare", HEALTHCARE),
        Rule("Investments", INVESTMENT),
        Rule("Banking", BANKING),
        Rule("Personal Care", PERSONAL_CARE),
        Rule("Education", EDUCATION),
        Rule("Mobile", MOBILE),
        Rule("Fitness", FITNESS),
        Rule("Insurance", INSURANCE),
        Rule("Travel", TRAVEL),
    )

    //find duplicates in each rule and print it from RULES
    public fun findDuplicateKeywords(): Set<String> {
        val duplicates = RULES.flatMap { it.includes + it.excludes }.groupingBy { it }.eachCount()
            .filter { it.value > 1 }.keys
        if (duplicates.isNotEmpty()) {
            println("Duplicate keywords found across categories: $duplicates")
        }
        return duplicates
    }

    // Define all categories with their visual properties
    val categories = mapOf(
        "Food & Dining" to CategoryInfo(
            displayName = "Food & Dining",
            icon = Icons.Default.Restaurant,
            color = Color(0xFFFC8019), // Swiggy orange
            fallbackIcon = Icons.Default.Fastfood
        ),
        "Groceries" to CategoryInfo(
            displayName = "Groceries",
            icon = Icons.Default.ShoppingCart,
            color = Color(0xFF5AC85A), // BigBasket green
            fallbackIcon = Icons.Default.LocalGroceryStore
        ),
        "Transportation" to CategoryInfo(
            displayName = "Transportation",
            icon = Icons.Default.DirectionsCar,
            color = Color(0xFF000000), // Uber black
            fallbackIcon = Icons.Default.Commute
        ),
        "Shopping" to CategoryInfo(
            displayName = "Shopping",
            icon = Icons.Default.ShoppingBag,
            color = Color(0xFFFF9900), // Amazon orange
            fallbackIcon = Icons.Default.Store
        ),
        "Bills & Utilities" to CategoryInfo(
            displayName = "Bills & Utilities",
            icon = Icons.Default.Receipt,
            color = Color(0xFF4CAF50), // Utility green
            fallbackIcon = Icons.Default.Payment
        ),
        "Entertainment" to CategoryInfo(
            displayName = "Entertainment",
            icon = Icons.Default.MovieFilter,
            color = Color(0xFFE50914), // Netflix red
            fallbackIcon = Icons.Default.PlayCircle
        ),
        "Healthcare" to CategoryInfo(
            displayName = "Healthcare",
            icon = Icons.Default.LocalHospital,
            color = Color(0xFF10847E), // PharmEasy teal
            fallbackIcon = Icons.Default.HealthAndSafety
        ),
        "Investments" to CategoryInfo(
            displayName = "Investments",
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            color = Color(0xFF00D09C), // Groww green
            fallbackIcon = Icons.AutoMirrored.Filled.ShowChart
        ),
        "Banking" to CategoryInfo(
            displayName = "Banking",
            icon = Icons.Default.AccountBalance,
            color = Color(0xFF004C8F), // HDFC blue
            fallbackIcon = Icons.Default.AccountBalanceWallet
        ),
        "Personal Care" to CategoryInfo(
            displayName = "Personal Care",
            icon = Icons.Default.Face,
            color = Color(0xFF6A4C93), // Urban Company purple
            fallbackIcon = Icons.Default.Spa
        ),
        "Education" to CategoryInfo(
            displayName = "Education",
            icon = Icons.Default.School,
            color = Color(0xFF673AB7), // Byju's purple
            fallbackIcon = Icons.Default.Book
        ),
        "Mobile" to CategoryInfo(
            displayName = "Mobile & Recharge",
            icon = Icons.Default.Smartphone,
            color = Color(0xFF2A3890), // Jio blue
            fallbackIcon = Icons.Default.PhoneAndroid
        ),
        "Fitness" to CategoryInfo(
            displayName = "Fitness",
            icon = Icons.Default.FitnessCenter,
            color = Color(0xFFFF3278), // Cult.fit pink
            fallbackIcon = Icons.Default.SportsMartialArts
        ),
        "Insurance" to CategoryInfo(
            displayName = "Insurance",
            icon = Icons.Default.Shield,
            color = Color(0xFF0066CC), // LIC blue
            fallbackIcon = Icons.Default.Security
        ),
        "Tax" to CategoryInfo(
            displayName = "Tax",
            icon = Icons.Default.AccountBalanceWallet,
            color = Color(0xFF795548), // Brown for tax
            fallbackIcon = Icons.Default.Receipt
        ),
        "Bank Charges" to CategoryInfo(
            displayName = "Bank Charges",
            icon = Icons.Default.MoneyOff,
            color = Color(0xFF9E9E9E), // Grey for charges
            fallbackIcon = Icons.Default.RemoveCircle
        ),
        "Credit Card Payment" to CategoryInfo(
            displayName = "Credit Card Payment",
            icon = Icons.Default.CreditCard,
            color = Color(0xFF1976D2), // Blue for credit card
            fallbackIcon = Icons.Default.Payment
        ),
        "Salary" to CategoryInfo(
            displayName = "Salary",
            icon = Icons.Default.Payments,
            color = Color(0xFF4CAF50), // Income green
            fallbackIcon = Icons.Default.AttachMoney
        ),
        "Income" to CategoryInfo(
            displayName = "Other Income",
            icon = Icons.Default.AddCircle,
            color = Color(0xFF4CAF50), // Income green
            fallbackIcon = Icons.AutoMirrored.Filled.TrendingUp
        ),
        "Travel" to CategoryInfo(
            displayName = "Travel",
            icon = Icons.Default.Flight,
            color = Color(0xFF00BCD4), // Travel blue
            fallbackIcon = Icons.Default.AirplanemodeActive
        ),
        "Others" to CategoryInfo(
            displayName = "Others",
            icon = Icons.Default.Category,
            color = Color(0xFF757575), // Grey
            fallbackIcon = Icons.Default.MoreHoriz
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
        return "Others"
    }
}

/**
 * Icon provider with fallback mechanism
 */
object IconProvider {

    /**
     * Get icon for a merchant with fallback logic
     * 1. Try to get brand-specific icon
     * 2. If not found, use category icon
     * 3. If category not found, use default icon
     */
    fun getIconForMerchant(merchantName: String): IconResource {
        // Try brand icon first
        BrandIcons.getIconResource(merchantName)?.let { iconRes ->
            return IconResource.DrawableResource(iconRes)
        }

        // Fall back to category icon
        val category = CategoryMapping.getCategory(merchantName)
        val categoryInfo = CategoryMapping.categories[category]
            ?: CategoryMapping.categories["Others"]!!

        return IconResource.VectorIcon(
            icon = categoryInfo.icon,
            tint = categoryInfo.color
        )
    }

    /**
     * Get category info including icon and color
     */
    fun getCategoryInfo(merchantName: String): CategoryMapping.CategoryInfo {
        val category = CategoryMapping.getCategory(merchantName)
        return CategoryMapping.categories[category]
            ?: CategoryMapping.categories["Others"]!!
    }
}

/**
 * Sealed class for different icon types
 */
sealed class IconResource {
    data class DrawableResource(val resId: Int) : IconResource()
    data class VectorIcon(val icon: ImageVector, val tint: Color) : IconResource()
}