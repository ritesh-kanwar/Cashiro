package com.cashiroai.shared.data.local

expect class SharedDatabaseFactory() {
    fun createDatabase(): SharedDatabase
}
