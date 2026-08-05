package com.jean.vocabs.shared.data.local

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.jean.vocabs.shared.db.VocabsDatabase

class AndroidDatabaseDriverFactory(
    private val context: Context,
) : DatabaseDriverFactory {
    override fun create() = AndroidSqliteDriver(
        schema = VocabsDatabase.Schema,
        context = context,
        name = "vocabs.db",
    )
}
