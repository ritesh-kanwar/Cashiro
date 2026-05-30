import com.ritesh.cashiro.presentation.common.icons.CategoryMapping
import org.junit.Test
import kotlin.test.assertEquals

class CategoryMappingTest {

    @Test
    fun testCategoryMapping() {
        // Test some key merchants from each category
        assertEquals("Food & Drinks", getCategory("Piri Piri Flaming Grill"))
        assertEquals("Food & Drinks", getCategory("Naixue"))
        assertEquals("Food & Drinks", getCategory("BN-Icon Siam"))
        assertEquals("Food & Drinks", getCategory("Amici"))

        assertEquals("Food & Drinks", getCategory("PF Changs"))
        assertEquals("Food & Drinks", getCategory("Bombay Bungalow"))
        assertEquals("Food & Drinks", getCategory("Heytea"))
        assertEquals("Food & Drinks", getCategory("Google Nomadtable"))
        assertEquals("Food & Drinks", getCategory("GHL*JHAROKA BY INDUS BANGKOK  11 TH"))
        assertEquals("Food & Drinks", getCategory("GHLJHAROKA BY INDUS BANGKOK  11 TH"))
        assertEquals("Food & Drinks", getCategory("Ksher  *AKARASKYROOFTOBangkok TH"))


        assertEquals("Groceries", getCategory("7-11"))

        assertEquals("Transport", getCategory("Airports of Thailand"))
        assertEquals("Transport", getCategory("Expressway"))
        assertEquals("Transport", getCategory("Grab A-123"))
        assertEquals("Transport", getCategory("SATS T1"))
        assertEquals("Transport", getCategory("Pyxbolt Services"))

        assertEquals("Shopping", getCategory("Uniqlo TRX"))
        assertEquals("Shopping", getCategory("Lyn"))
        assertEquals("Shopping", getCategory("Apple Central World"))
        assertEquals("Shopping", getCategory("OpenAI ChatGPT"))
        assertEquals("Shopping", getCategory("Sukhumvit City Mall"))
        assertEquals("Shopping", getCategory("The Emsphere"))
        assertEquals("Shopping", getCategory("Central World"))
        assertEquals("Shopping", getCategory("The Empire Tower"))

        assertEquals("Entertainment", getCategory("Major Cineplex"))
        assertEquals("Entertainment", getCategory("Ticketmelon"))
        assertEquals("Entertainment", getCategory("2C2P Major Cineplex"))

        assertEquals("Travel", getCategory("Four Points by Sheraton"))
        assertEquals("Travel", getCategory("Crowne Plaza KLCC"))
        assertEquals("Travel", getCategory("Dusit Thani Bangkok"))
        assertEquals("Travel", getCategory("Marina Bay Sands"))
        assertEquals("Travel", getCategory("Hilton Garden Inn"))
        assertEquals("Travel", getCategory("WESTIN KL-FRONT OFFICE KUALA LUMPUR MY MY"))
        assertEquals("Travel", getCategory("WWW.MAGNOLIASSERVICEDRBANGKOK TH"))
        assertEquals("Travel", getCategory("FOUR POINTS BY SHERATOBANGKOK 11 TH"))


        assertEquals("Medical", getCategory("Life Pharm"))
        assertEquals("Medical", getCategory("Bumrungrad"))
        assertEquals("Medical", getCategory("Medex"))
        assertEquals("Medical", getCategory("BOOTS_4287 C.WORLD 3 FBANGKOK TH"))

        assertEquals("Personal", getCategory("Sultans of Shave"))
        assertEquals("Personal", getCategory("Mandarin Oriental Spa"))
        assertEquals("Personal", getCategory("Truefitt and Hill"))
        assertEquals("Personal", getCategory("Phetsathorn Co.,Ltd."))

        assertEquals("Tax", getCategory("Abu Dhabi Judicial Dep"))
        assertEquals("Tax", getCategory("Sharjah Finance Depart"))

        assertEquals("Bill", getCategory("Tamdeed Projects"))
        assertEquals("Bill", getCategory("WWW.PAYSOLUT*WWW.PAYSOBANGKOK  TH"))

        assertEquals("Business", getCategory("My Fatoorah"))
        assertEquals("Business", getCategory("Transfer: 002 → 001"))
        assertEquals("Business", getCategory("Transfer from 001 to 002"))
        assertEquals("Business", getCategory("Transfer to 0001"))

        // Test some should remain as Miscellaneous (too generic)
        assertEquals("Miscellaneous", getCategory("Twin Made"))
        findDuplicateKeywords()
    }
}

// Helper function to test categorization
private fun getCategory(merchantName: String): String {
    return CategoryMapping.getCategory(merchantName)
}

private fun findDuplicateKeywords(): Set<String> {
    return CategoryMapping.findDuplicateKeywords()
}