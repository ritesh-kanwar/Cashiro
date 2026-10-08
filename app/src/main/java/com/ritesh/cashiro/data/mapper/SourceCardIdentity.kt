package com.ritesh.cashiro.data.mapper

import com.ritesh.parser.core.ParsedTransaction
import com.ritesh.parser.core.TransactionType

/** Card identity comes from the parser, before transaction rules run. */
internal val ParsedTransaction.isSourceCard: Boolean
    get() = isFromCard || type == TransactionType.CREDIT
