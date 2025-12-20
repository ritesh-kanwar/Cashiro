import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.bank.FederalBankParser
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import com.ritesh.parser.core.test.TestResult
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class FederalBankMandateParsingTest {

    @Test
    fun `federal bank mandate helpers behave consistently`() {
        val parser = FederalBankParser()

        ParserTestUtils.printTestHeader(
            parserName = "Federal Bank Mandate",
            bankName = parser.getBankName(),
            currency = parser.getCurrency()
        )

        val testMessages = mapOf(
            "mandate_creation" to "Dear Customer, You have successfully created a mandate on Streaming Service for a MONTHLY frequency starting from 11-06-2025 for a maximum amount of Rs 299.00 Mandate Ref No- abc123def456@fifederal - Federal Bank",
            "payment_due" to "Hi, payment due for Streaming Service,INR 299.00 on 06/08/2024 will be processed using Federal Bank Debit Card ****. To cancel, visit https://www.sihub.in/managesi/federal T&CA -Federal Bank",
            "payment_successful" to "Hi, payment of INR 299.00 for Streaming Service via e-mandate ID: xyz789abc on Federal Bank Debit Card **** is processed successfully. To manage, visit: https://www.sihub.in/managesi/federal T&CA - Federal Bank"
        )

        val results = mutableListOf<TestResult>()

        ParserTestUtils.printSectionHeader("Mandate Creation Detection")
        val creationMessage = testMessages.getValue("mandate_creation")
        val isCreation = parser.isMandateCreationNotification(creationMessage)
        val creationResult = TestResult(
            name = "Mandate creation detected",
            passed = isCreation,
            error = if (!isCreation) "Parser did not flag mandate creation" else null,
            details = if (isCreation) "Mandate creation notification recognised" else null
        )
        ParserTestUtils.printTestResult(creationResult)
        results.add(creationResult)

        if (isCreation) {
            val mandateInfo = parser.parseEMandateSubscription(creationMessage)
            val mandateResult = if (mandateInfo != null &&
                mandateInfo.amount == BigDecimal("299.00") &&
                mandateInfo.merchant == "Streaming Service" &&
                mandateInfo.nextDeductionDate == "11-06-2025" &&
                mandateInfo.umn == "abc123def456@fifederal"
            ) {
                TestResult(
                    name = "Mandate subscription parsed",
                    passed = true,
                    details = "Mandate for ${mandateInfo.merchant} starting ${mandateInfo.nextDeductionDate}"
                )
            } else {
                TestResult(
                    name = "Mandate subscription parsed",
                    passed = false,
                    error = "Mandate parsing mismatch: $mandateInfo"
                )
            }
            ParserTestUtils.printTestResult(mandateResult)
            results.add(mandateResult)
        }

        ParserTestUtils.printSectionHeader("Future Debit Detection")
        val paymentDueMessage = testMessages.getValue("payment_due")
        val futureDebitInfo = parser.parseFutureDebit(paymentDueMessage)
        val futureResult = if (futureDebitInfo != null &&
            futureDebitInfo.amount == BigDecimal("299.00") &&
            futureDebitInfo.merchant == "Streaming Service" &&
            futureDebitInfo.nextDeductionDate == "06/08/24"
        ) {
            TestResult(
                name = "Future debit parsed",
                passed = true,
                details = "Next deduction ${futureDebitInfo.nextDeductionDate}"
            )
        } else {
            TestResult(
                name = "Future debit parsed",
                passed = false,
                error = "Future debit parsing mismatch: $futureDebitInfo"
            )
        }
        ParserTestUtils.printTestResult(futureResult)
        results.add(futureResult)

        ParserTestUtils.printSectionHeader("Successful Mandate Payment")
        val successMessage = testMessages.getValue("payment_successful")
        val isDeclined = parser.isDeclinedMandatePayment(successMessage)
        val declineResult = TestResult(
            name = "Mandate payment not declined",
            passed = !isDeclined,
            error = if (isDeclined) "Parser flagged successful payment as declined" else null,
            details = if (!isDeclined) "Decline detection working" else null
        )
        ParserTestUtils.printTestResult(declineResult)
        results.add(declineResult)

        val isTransaction = parser.isTransactionMessageForTesting(successMessage)
        val transactionFlagResult = TestResult(
            name = "Mandate payment recognised as transaction",
            passed = isTransaction,
            error = if (!isTransaction) "Parser did not treat successful payment as transaction" else null,
            details = if (isTransaction) "Transaction message detected" else null
        )
        ParserTestUtils.printTestResult(transactionFlagResult)
        results.add(transactionFlagResult)

        val transactionCase = ParserTestCase(
            name = "Mandate payment parsed",
            message = successMessage,
            sender = "AD-FEDBNK",
            expected = ExpectedTransaction(
                amount = BigDecimal("299.00"),
                currency = "INR",
                type = TransactionType.EXPENSE,
                merchant = "Streaming Service via e-mandate ID: xyz789abc"
            )
        )
        val parsedPaymentResult = ParserTestUtils.runSingleTest(parser, transactionCase)
        ParserTestUtils.printTestResult(parsedPaymentResult)
        results.add(parsedPaymentResult)

        val passed = results.count { it.passed }
        val failed = results.size - passed
        val failureDetails = results.filterNot { it.passed }.mapNotNull { it.error }

        ParserTestUtils.printTestSummary(
            totalTests = results.size,
            passedTests = passed,
            failedTests = failed,
            failureDetails = failureDetails
        )
    }
}
