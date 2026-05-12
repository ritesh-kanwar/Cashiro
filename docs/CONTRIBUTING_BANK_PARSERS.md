# Contributing New Bank Parsers

This guide explains the process for adding support for a new bank or financial service to **Cashiro**.

## Overview
Cashiro uses a modular parser system to extract transaction data from SMS messages. All parsing logic is centralized in the `:parser-core` module, while UI branding (icons and colors) is handled in the `:app` module.

---

## Step 1: Create the Parser Class
The parser class defines how to identify the bank's SMS and how to extract data (amount, type, account, etc.).

1.  **File Location**: `parser-core/src/main/kotlin/com/ritesh/parser/core/bank/`
2.  **Implementation**: Your class must extend `BankParser`. 

### Key Methods to Implement:
*   `getBankName()`: Returns the display name (e.g., `"HDFC Bank"`).
*   `getCurrency()`: Default currency code (e.g., `"INR"`).
*   `canHandle(sender: String)`: Returns `true` if this parser should handle messages from the given sender/address.
*   `extractAmount(message: String)`: Use Regex to find and return the transaction amount as a `BigDecimal`.
*   `extractTransactionType(message: String)`: Identify if the message represents an **EXPENSE**, **INCOME**, or **TRANSFER**.
*   `extractMerchant(message: String)`: (Optional) Identify the merchant or recipient involved.

### Example Template:
```kotlin
class ExampleBankParser : BankParser() {
    override fun getBankName() = "Example Bank"
    override fun getCurrency() = "INR"

    override fun canHandle(sender: String) = sender.uppercase().contains("EXAMP")

    override fun extractAmount(message: String): BigDecimal? {
        val pattern = Regex("""Rs\.?\s*([0-9,.]+)""")
        return pattern.find(message)?.groupValues?.get(1)
            ?.replace(",", "")?.toBigDecimalOrNull()
    }

    override fun extractTransactionType(message: String): TransactionType? {
        val lower = message.lowercase()
        return when {
            lower.contains("debited") -> TransactionType.EXPENSE
            lower.contains("credited") -> TransactionType.INCOME
            else -> null
        }
    }
}
```

---

## Step 2: Register the Parser
Your parser will not be used until it is registered in the factory.

1.  **File**: `parser-core/src/main/kotlin/com/ritesh/parser/core/bank/BankParserFactory.kt`
2.  **Action**: Add `YourNewBankParser()` to the `parsers` list.

> [!NOTE]
> The order in the list determines priority. Place parsers with very specific `canHandle` logic higher in the list.

---

## Step 3: Add Branding (Icons & Colors)
To ensure the bank logo appears in the transaction list:

1.  **Icon Resource**: Add a logo file (PNG/WebP) to `app/src/main/res/drawable-nodpi/`.
    *   **Naming Convention**: `ic_brand_new_bank.webp`
    *   **Example**: `ic_brand_nabil_bank.webp`, `ic_brand_manjushree_finance.webp`
2.  **Icon Mapping**: Update `app/src/main/java/com/ritesh/cashiro/presentation/common/icons/BrandIcons.kt`.
    *   Add entry to `brandMappings`: `"new bank" to R.drawable.ic_brand_new_bank`
3.  **Brand Color**: In the same file, add the bank's primary color to the `getBrandColor` function.

> [!NOTE]
> For banks with separate branding, add separate assets and mappings instead of reusing another bank's logo.

---

## Step 4: Create Unit Tests
Unit tests are mandatory for all parsers to ensure accuracy across different SMS templates.

1.  **File Location**: `parser-core/src/test/kotlin/com/ritesh/parser/core/bank/`
2.  **Implementation**: Create a test class using the `ParserTestUtils` helper.
3.  **Verification**: Run the test using Gradle:
    ```bash
    ./gradlew :parser-core:test --tests "YourNewBankParserTest"
    ```

---

## Step 5: Update Bank Support Matrix
Keep the central documentation up-to-date for users.

1.  **File**: `docs/BANK_SUPPORT.md`
2.  **Action**:
    - Add a new row to the **Supported Banks & Transaction Patterns** table.
    - Add your bank's common sender IDs to the **Sender ID Patterns** table.
    - Update the **Last Updated** date at the bottom of the file.

---

## Step 6: Verification
Before submitting a Pull Request:
1.  Ensure all unit tests pass.
2.  Ensure the app builds successfully (`./gradlew assembleDebug`).
3.  (Recommended) Test with real SMS samples using the "Scan SMS" feature in the app.
