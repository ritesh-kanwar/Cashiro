package com.ritesh.cashiro.data.manager

import android.util.Log
import com.ritesh.cashiro.BuildConfig
import com.ritesh.parser.core.ParsedTransaction
import com.ritesh.cashiro.data.database.entity.CardType
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.data.mapper.accountIdentity
import com.ritesh.cashiro.data.mapper.toEntityType
import com.ritesh.cashiro.data.mapper.isSourceCard
import com.ritesh.cashiro.data.repository.AccountBalanceRepository
import com.ritesh.cashiro.data.repository.CardRepository
import com.ritesh.cashiro.utils.PiiRedactor
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BalanceUpdateProcessor @Inject constructor(
    private val cardRepository: CardRepository,
    private val accountBalanceRepository: AccountBalanceRepository
) {
    companion object {
        private const val TAG = "BalanceUpdateProcessor"
    }

    suspend fun process(
        parsedTransaction: ParsedTransaction,
        entity: TransactionEntity,
        rowId: Long
    ) {
        val sourceAccount = if (parsedTransaction.isSourceCard || parsedTransaction.isMobileWallet) parsedTransaction.accountIdentity else
            parsedTransaction.accountLast4?.let {
                accountBalanceRepository.resolveAccountLast4(parsedTransaction.bankName, it, parsedTransaction.currency)
            }
        val sameAccount = entity.bankName == parsedTransaction.bankName &&
            entity.accountNumber == sourceAccount && entity.currency == parsedTransaction.currency
        if (!parsedTransaction.isSourceCard && !sameAccount) {
            val bank = entity.bankName ?: return
            val suffix = entity.accountNumber ?: return
            accountBalanceRepository.insertTransactionBalance(
                bankName = bank, accountLast4 = suffix, amount = entity.amount,
                transactionType = entity.transactionType, explicitBalance = null,
                timestamp = entity.dateTime, transactionId = rowId, creditLimit = null,
                isCreditCard = false, smsSource = null, currency = entity.currency
            )
            return
        }
        val parsedAccountLast4 = sourceAccount?.takeIf { it.isNotBlank() }
        val resolvedAccountLast4 = entity.accountNumber?.takeIf { it.isNotBlank() }
        val fallbackAccountLast4 = parsedAccountLast4 ?: resolvedAccountLast4
        if (fallbackAccountLast4 == null) return

        val isFromCard = parsedTransaction.isSourceCard

        val targetAccountLast4: String? = if (isFromCard) {
            var card = cardRepository.getCard(parsedTransaction.bankName, fallbackAccountLast4)

            if (card == null) {
                val isCredit = (parsedTransaction.type.toEntityType() == TransactionType.CREDIT)
                cardRepository.findOrCreateCard(
                    cardLast4 = fallbackAccountLast4,
                    bankName = parsedTransaction.bankName,
                    isCredit = isCredit
                )
                card = cardRepository.getCard(parsedTransaction.bankName, fallbackAccountLast4)
            }

            if (card == null) {
                Log.w(TAG, "Could not create/find card for ${parsedTransaction.bankName}")
                null
            } else {
                cardRepository.updateCardBalance(
                    cardId = card.id,
                    balance = parsedTransaction.balance,
                    source = sanitizeSmsSource(parsedTransaction).take(200),
                    date = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(parsedTransaction.timestamp),
                        ZoneId.systemDefault()
                    )
                )

                when {
                    card.cardType == CardType.CREDIT -> fallbackAccountLast4
                    card.cardType == CardType.DEBIT && card.accountLast4 != null -> card.accountLast4
                    else -> null
                }
            }
        } else {
            fallbackAccountLast4
        }

        if (targetAccountLast4 != null) {
            val isCreditCard = (parsedTransaction.type.toEntityType() == TransactionType.CREDIT) ||
                    fallbackAccountLast4.let {
                        cardRepository.getCard(parsedTransaction.bankName, it)?.cardType
                    } == CardType.CREDIT

            accountBalanceRepository.insertTransactionBalance(
                bankName = parsedTransaction.bankName,
                accountLast4 = targetAccountLast4,
                amount = entity.amount,
                transactionType = entity.transactionType,
                explicitBalance = parsedTransaction.balance,
                timestamp = entity.dateTime,
                transactionId = if (rowId != -1L) rowId else null,
                creditLimit = parsedTransaction.creditLimit,
                isCreditCard = isCreditCard,
                smsSource = sanitizeSmsSource(parsedTransaction),
                currency = parsedTransaction.currency,
                isWallet = parsedTransaction.isMobileWallet
            )

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Saved balance update from SMS transaction")
            }
        }
    }

    private fun sanitizeSmsSource(parsedTransaction: ParsedTransaction): String {
        return PiiRedactor.redact(parsedTransaction.smsBody).take(500)
    }
}
