package com.ritesh.parser.core.bank

/**
 * Factory for creating bank-specific parsers based on SMS sender.
 */
object BankParserFactory {

    private val parsers = listOf(
        ApolloParser(),
        AwashBankParser(),
        BSFBankParser(),
        BancoAgricolaParser(),
        BancoCuscatlanParser(),
        BancoPromericaParser(),
        BankOfAbyssiniaParser(),
        BkashParser(),
        ChaseUKParser(),
        CitizensBankParser(),
        D360BankParser(),
        GTBankParser(),
        JanaSmallFinanceBankParser(),
        LumbiniBikashBankParser(),
        MachchhapuchreBankParser(),
        MoniepointParser(),
        NDBBankParser(),
        NationalBankOfEgyptParser(),
        NationalSavingsBankParser(),
        NationsTrustBankParser(),
        NepalBankParser(),
        NepalSBIBankParser(),
        PasargadBankParser(),
        PluxeeBankParser(),
        SiketBankParser(),
        StandardCharteredNepalParser(),
        StandardCharteredNigeriaParser(),
        Trading212Parser(),
        VFDBankParser(),
        ZamZamBankParser(),

        HDFCMutualFundParser(),  // HDFC Mutual Fund (must be before HDFCBankParser to avoid interception by HDFC's broad DLT pattern)
        HDFCBankParser(),
        SBIBankParser(),
        SaraswatBankParser(),
        DBSBankParser(),
        IndianBankParser(),
        FederalBankParser(),
        JuspayParser(),
        SliceParser(),
        CredParser(),
        LazyPayParser(),
        UtkarshBankParser(),
        ICICIBankParser(),
        KarnatakaBankParser(),
        KeralaGraminBankParser(),
        IDBIBankParser(),
        JupiterBankParser(),
        AxisBankParser(),
        PNBBankParser(),
        CanaraBankParser(),
        BankOfBarodaParser(),
        BankOfIndiaParser(),
        JioPaymentsBankParser(),
        KotakBankParser(),
        IDFCFirstBankParser(),
        UnionBankParser(),
        HSBCBankParser(),
        CentralBankOfIndiaParser(),
        SouthIndianBankParser(),
        JKBankParser(),
        JioPayParser(),
        IPPBParser(),
        DOPBankParser(),
        CityUnionBankParser(),
        IndianOverseasBankParser(),
        AirtelPaymentsBankParser(),
        IndusIndBankParser(),
        AMEXBankParser(),
        OneCardParser(),
        UCOBankParser(),
        AUBankParser(),
        YesBankParser(),
        BandhanBankParser(),
        ADCBParser(),  // Abu Dhabi Commercial Bank (UAE)
        FABParser(),  // First Abu Dhabi Bank (UAE)
        EmiratesNBDParser(),  // Emirates NBD Bank (UAE)
        LivBankParser(),  // Liv Bank (UAE)
        CitiBankParser(),  // Citi Bank (USA)
        DiscoverCardParser(),  // Discover Card (USA)
        OldHickoryParser(),  // Old Hickory Credit Union (USA)
        LaxmiBankParser(),  // Laxmi Sunrise Bank (Nepal)
        CBEBankParser(),  // Commercial Bank of Ethiopia
        EverestBankParser(),  // Everest Bank (Nepal)
        BancolombiaParser(),  // Bancolombia (Colombia)
        MashreqBankParser(),  // Mashreq Bank (UAE)
        CharlesSchwabParser(),  // Charles Schwab (USA)
        NavyFederalParser(),  // Navy Federal Credit Union (USA)
        AdelFiParser(),  // AdelFi Credit Union (USA)
        AlecuBankParser(),  // ALECU Credit Union (USA)
        PriorbankParser(),  // Priorbank (Belarus)
        AlinmaBankParser(),  // Alinma Bank (Saudi Arabia)
        NabilBankParser(),  // Nabil Bank (Nepal)
        NMBBankParser(),  // NMB Bank (Nepal)
        ManjushreeFinanceParser(), // Manjushree Finance (Nepal)
        SiddharthaBankParser(),  // Siddhartha Bank Limited (Nepal)
        PrimeCommercialBankParser(),  // Prime Commercial Bank (Nepal)
        MPesaTanzaniaParser(),  // M-Pesa Tanzania (must be before Kenya M-PESA)
        MPESAParser(),  // M-PESA (Kenya)
        SelcomPesaParser(),  // Selcom Pesa (Tanzania)
        TigoPesaParser(),  // Tigo Pesa / Mixx by Yas (Tanzania)
        CIBEgyptParser(),  // CIB - Commercial International Bank (Egypt)
        DhanlaxmiBankParser(),  // Dhanlaxmi Bank (India)
        HuntingtonBankParser(),  // Huntington Bank (USA)
        StandardCharteredBankParser(),  // Standard Chartered Bank (India and Pakistan)
        EquitasBankParser(),  // Equitas Small Finance Bank (India)
        TelebirrParser(),  // Telebirr (Ethiopia)
        ZemenBankParser(),  // Zemen Bank (Ethiopia)
        DashenBankParser(),  // Dashen Bank (Ethiopia)
        FaysalBankParser(),  // Faysal Bank (Pakistan)
        MelliBankParser(),  // Melli Bank (Iran)
        ParsianBankParser(),  // Parsian Bank (Iran)
        BangkokBankParser(),  // Bangkok Bank (Thailand)
        KasikornBankParser(),  // Kasikorn Bank (Thailand)
        SiamCommercialBankParser(),  // Siam Commercial Bank (Thailand)
        KrungThaiBankParser(),  // Krungthai Bank (Thailand)
        KrungsriBankParser(),  // Krungsri / Bank of Ayudhya (Thailand)
        TTBBankParser(),  // TMBThanachart Bank (Thailand)
        GSBBankParser(),  // Government Savings Bank (Thailand)
        BAACBankParser(),  // BAAC (Thailand)
        UOBThailandParser(),  // UOB Thailand
        CIMBThaiParser(),  // CIMB Thai (Thailand)
        KTCCreditCardParser(),  // KTC Credit Card (Thailand)
        MBankCZParser(),  // mBank CZ (Czech Republic)
        AlRajhiBankParser(),  // Al Rajhi Bank (Saudi Arabia)
        ChaseBankParser(),  // Chase Bank (USA)
        TBankParser(),  // T-Bank / Tinkoff (Russia)
        BankMuscatParser(),  // Bank Muscat (Oman)
        BPCEParser(),      // BPCE (France)
        // Africa Expansion P3-D: Mozambique
        StandardBankMozambiqueParser(),  // Standard Bank Mozambique
        MillenniumBimParser(),  // Millennium BIM (Mozambique)
        EMolaParser(),  // E-Mola (Mozambique)
        MPesaMozambiqueParser(),  // M-PESA Mozambique (must be after MPesaTanzaniaParser)
        // Africa Expansion P3-D: Tanzania
        CrdbBankParser(),  // CRDB Bank (Tanzania)
        DiamondTrustBankParser(),  // Diamond Trust Bank (Tanzania)
        MixxByYasParser(),  // Mixx by Yas (Tanzania)
        NMBTanzaniaParser(),  // NMB Tanzania
        GreaterBankParser(),  // Greater Bank (Tanzania)
        // P3-A: Nigerian Banks
        AccessBankParser(),  // Access Bank (Nigeria)
        ZenithBankParser(),  // Zenith Bank (Nigeria)
        KeystoneBankParser(),  // Keystone Bank (Nigeria)
        JaizBankParser(),  // Jaiz Bank (Nigeria)
        OpayBankParser(),  // Opay (Nigeria)
        // P3-B: Additional Indian Banks
        NSDLPaymentsBankParser(),  // NSDL Payments Bank (India)
        PunjabSindBankParser(),  // Punjab & Sind Bank (India)
        KeralaBankParser(),  // Kerala Bank (India)
        CashfreeParser(),  // Cashfree (India)
        NaviMutualFundParser(),  // Navi Mutual Fund (India)
        // P3-C: Middle East Expansion
        EmiratesIslamicParser(),  // Emirates Islamic (UAE)
        SNBAlAhliBankParser(),  // SNB AlAhli (Saudi Arabia)
        STCBankParser(),  // STC Bank (Saudi Arabia)
        SabbBankParser(),  // SABB Bank (Saudi Arabia)
        MellatBankParser(),  // Mellat Bank (Iran)
        BankinoBankParser(),  // Bankino (Iran)
        BluBankParser(),  // Blu Bank (Iran)
        ArabBankParser(),  // Arab Bank (Egypt)
        // P3-E: Other Regions
        SampathBankParser(),  // Sampath Bank (Sri Lanka)
        EnparaBankParser(),  // Enpara Bank (Turkey)
        SparkasseRheinMaasParser(),  // Sparkasse Rhein-Maas (Germany)
        AltanaFCUParser()  // Altana FCU (USA)
        // Add more bank parsers here as we implement them
    )

    /**
     * Returns the appropriate bank parser for the given sender.
     * Returns null if no specific parser is found.
     */
    fun getParser(sender: String): BankParser? {
        return parsers.firstOrNull { it.canHandle(sender) }
    }

    /**
     * Returns all bank parsers that can handle the given sender.
     * Multiple parsers may match the same sender (e.g., specialized parsers
     * before a general one), so callers should use firstNotNullOfOrNull
     * to let the content decide which parser produces a result.
     */
    fun getParsers(sender: String): List<BankParser> {
        return parsers.filter { it.canHandle(sender) }
    }

    /**
     * Returns the bank parser for the given bank name.
     * Returns null if no specific parser is found.
     */
    fun getParserByName(bankName: String): BankParser? {
        return parsers.firstOrNull { it.getBankName() == bankName }
    }

    /**
     * Returns all available bank parsers.
     */
    fun getAllParsers(): List<BankParser> = parsers

    /**
     * Checks if the sender belongs to any known bank.
     */
    fun isKnownBankSender(sender: String): Boolean {
        return parsers.any { it.canHandle(sender) }
    }
}
