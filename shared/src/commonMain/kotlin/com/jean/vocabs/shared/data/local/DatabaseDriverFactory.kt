package com.jean.vocabs.shared.data.local

import app.cash.sqldelight.db.SqlDriver

/**
 * The database's platform seam. Only the Android implementation exists today.
 *
 * An interface rather than expect/actual, because that also lets tests inject an
 * in-memory driver without an emulator.
 */
fun interface DatabaseDriverFactory {
    fun create(): SqlDriver
}
