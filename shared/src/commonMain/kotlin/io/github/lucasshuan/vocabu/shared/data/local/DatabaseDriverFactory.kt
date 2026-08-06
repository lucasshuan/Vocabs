package io.github.lucasshuan.vocabu.shared.data.local

import app.cash.sqldelight.db.SqlDriver

/**
 * An interface rather than expect/actual, so tests can inject an in-memory
 * driver without an emulator.
 */
fun interface DatabaseDriverFactory {
    fun create(): SqlDriver
}
