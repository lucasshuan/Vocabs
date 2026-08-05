package io.github.lucasshuan.vocabu.shared.data.local

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.github.lucasshuan.vocabu.shared.db.VocabsDatabase

class AndroidDatabaseDriverFactory(
    private val context: Context,
) : DatabaseDriverFactory {
    override fun create() = AndroidSqliteDriver(
        schema = VocabsDatabase.Schema,
        context = context,
        name = "vocabs.db",
    )
}
