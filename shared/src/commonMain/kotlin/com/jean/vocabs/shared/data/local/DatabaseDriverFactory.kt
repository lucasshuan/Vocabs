package com.jean.vocabs.shared.data.local

import app.cash.sqldelight.db.SqlDriver

/**
 * A costura de plataforma do banco. Hoje só existe a implementação Android;
 * o iOS entra depois com uma classe que devolve um NativeSqliteDriver, sem
 * tocar em nada do resto do módulo.
 *
 * É uma interface e não um expect/actual porque isso também deixa os testes
 * injetarem um driver em memória sem precisar de emulador.
 */
fun interface DatabaseDriverFactory {
    fun create(): SqlDriver
}
