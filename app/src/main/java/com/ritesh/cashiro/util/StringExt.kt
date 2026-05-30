package com.ritesh.cashiro.util

fun String.capitalizeFirst(): String {
    if (this.isEmpty()) return this
    return this.take(1).uppercase() + this.drop(1)
}
