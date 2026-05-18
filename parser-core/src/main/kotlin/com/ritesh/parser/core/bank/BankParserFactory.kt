package com.ritesh.parser.core.bank

/**
 * Factory for creating bank-specific parsers based on SMS sender.
 */
object BankParserFactory {

    private val parsers = listOf(
        HDFCMutualFundParser(),  // HDFC Mutual Fund (must be before HDFCBankParser to avoid interception by HDFC's broad DLT pattern)
        HDFCBankParser(),
        SBIBankParser(),
        SaraswatBankParser(),
        DBSBankParser(),
        IndianBankParser(),
        FederalBankParser(),
        JuspayParser(),
        CashfreeParser(),  // Cashfree payment gateway (India) — grouped with other aggregators
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
        PunjabSindBankParser(),  // Punjab & Sind Bank (India)
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
        AltanaFCUParser(),  // Altana Federal Credit Union (USA) - must be before Everest Bank (numeric catch-all)
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
<<<<<<< ours
<<<<<<< ours
        PrimeCommercialBankParser(),  // Prime Commercial Bank (Nepal)
=======
>>>>>>> theirs
=======
        PrimeCommercialBankParser(),  // Prime Commercial Bank (Nepal)
>>>>>>> theirs
        MPesaTanzaniaParser(),  // M-Pesa Tanzania (must be before Kenya M-PESA)
        MPESAParser(),  // M-PESA (Kenya)
        SelcomPesaParser(),  // Selcom Pesa (Tanzania)
        TigoPesaParser(),  // Tigo Pesa / Mixx by Yas (Tanzania)
        CIBEgyptParser(),  // CIB - Commercial International Bank (Egypt)
        DhanlaxmiBankParser(),  // Dhanlaxmi Bank (India)
        DOPBankParser(),  // Department of Post (India)
        HuntingtonBankParser(),  // Huntington Bank (USA)
<<<<<<< ours
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
<<<<<<< ours
        MBankCZParser(),  // mBank CZ (Czech Republic)
        AlRajhiBankParser(),  // Al Rajhi Bank (Saudi Arabia)
        ChaseBankParser(),  // Chase Bank (USA)
        TBankParser(),  // T-Bank / Tinkoff (Russia)
<<<<<<< ours
        BankMuscatParser(),  // Bank Muscat (Oman)
        BPCEParser(),      // BPCE (France)
        CashfreeParser(),  // Cashfree payment gateway (India)
        EnparaBankParser(),  // Enpara (Turkey)
        GreaterBankParser(),  // Greater Bank (India)
        PunjabSindBankParser(),  // Punjab & Sind Bank (India)
        SNBAlAhliBankParser(),  // Saudi National Bank / Al Ahli (Saudi Arabia)
        STCBankParser(),  // STC Bank (Saudi Arabia)
        SabbBankParser(),  // SABB / Saudi Awwal Bank (Saudi Arabia)
        SparkasseRheinMaasParser()  // Sparkasse Rhein-Maas (Germany)
=======
        StandardCharteredBankParser(),  // Standard Chartered Bank (India)
        EquitasBankParser(),  // Equitas Small Finance Bank (India)
        TelebirrParser(),  // Telebirr (Ethiopia)
        ZemenBankParser(),  // Zemen Bank (Ethiopia)
        DashenBankParser()  // Dashen Bank (Ethiopia)
>>>>>>> theirs
=======
        TBankParser()  // T-Bank / Tinkoff (Russia)
>>>>>>> theirs
=======
        ChaseBankParser(),  // Chase Bank (USA)
<<<<<<< ours
        AlRajhiBankParser()  // Al Rajhi Bank (Saudi Arabia)
>>>>>>> theirs
=======
        AlRajhiBankParser(),  // Al Rajhi Bank (Saudi Arabia)
<<<<<<< ours
=======
        SNBAlAhliBankParser(),  // Saudi National Bank / Al Ahli Bank (Saudi Arabia)
        STCBankParser(),  // STC Bank (Saudi Arabia)
        SabbBankParser(),  // SABB - Saudi Awwal Bank (Saudi Arabia)
>>>>>>> theirs
        MBankCZParser(),  // mBank CZ (Czech Republic)
<<<<<<< ours
<<<<<<< ours
        BankMuscatParser()  // Bank Muscat (Oman)
>>>>>>> theirs
=======
=======
        SparkasseRheinMaasParser(),  // Sparkasse Rhein-Maas (Germany)
<<<<<<< ours
>>>>>>> theirs
=======
        EnparaBankParser(),  // Enpara (Turkey) — push notifications
>>>>>>> theirs
        BankMuscatParser(),  // Bank Muscat (Oman)
        GreaterBankParser()  // Greater Bank (India)
>>>>>>> theirs
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
