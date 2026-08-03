package com.jean.vocabs.shared.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class Migracao4Test {
    @Test
    fun `migracao separa captura preservando ids ficha erros e retencao`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ESQUEMA_V3.split(';').map(String::trim).filter(String::isNotEmpty).forEach { sql ->
            driver.execute(null, sql, 0)
        }
        driver.execute(
            null,
            """INSERT INTO entrada VALUES (
                7, 'A verdant field', 'verdant', 'Book', 1000, 'PRONTA', 'TEXTO', NULL,
                'PALAVRA', 'verdejante', '["Muito verde"]', 'A verdant garden.', '/v/', NULL,
                82.0, 12.0, 900, 4, 3, 1
            )""",
            0,
        )
        driver.execute(
            null,
            """INSERT INTO entrada VALUES (
                8, NULL, NULL, NULL, 2000, 'RASCUNHO', 'FOTO', 'foto.jpg',
                NULL, NULL, NULL, NULL, NULL, 'ocr indisponível',
                100.0, 40.0, 2000, 0, 0, 0
            )""",
            0,
        )

        // 1.sqm..3.sqm já formavam a versão 4; daqui até a atual passa por todas.
        VocabsDatabase.Schema.migrate(driver, 4, VocabsDatabase.Schema.version)
        val queries = VocabsDatabase(driver).vocabsQueries
        val capturas = queries.listarTodasCapturas().executeAsList()
        val entradas = queries.listarTodasEntradas().executeAsList()

        assertEquals(listOf(8L, 7L), capturas.map { it.id })
        assertEquals(1, entradas.size)
        val entrada = entradas.single()
        assertEquals(7L, entrada.id)
        assertEquals(7L, entrada.captura_id)
        assertEquals("verdejante", entrada.traducao)
        assertEquals(2L, entrada.inicio)
        assertEquals(9L, entrada.fim)
        assertEquals(82.0, entrada.pontos)
        assertEquals(3L, entrada.acertos)
        assertEquals(1L, entrada.erros)
        val rascunho = capturas.first { it.id == 8L }
        assertEquals("AGUARDANDO_SELECAO", rascunho.status)
        assertEquals("foto.jpg", rascunho.midia_caminho)
        assertEquals("ocr indisponível", rascunho.erro_transcricao)
        assertNull(queries.buscarEntradaPorId(8).executeAsOneOrNull())
    }

    private companion object {
        const val ESQUEMA_V3 = """
            CREATE TABLE entrada (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                trecho TEXT,
                alvo TEXT,
                origem TEXT,
                criado_em INTEGER NOT NULL,
                status TEXT NOT NULL,
                formato TEXT NOT NULL,
                midia_caminho TEXT,
                tipo TEXT,
                traducao TEXT,
                definicoes_json TEXT,
                exemplo TEXT,
                ipa TEXT,
                erro TEXT,
                pontos REAL NOT NULL DEFAULT 100.0,
                taxa_decaimento REAL NOT NULL DEFAULT 40.0,
                data_ultima_interacao INTEGER NOT NULL DEFAULT 0,
                revisoes INTEGER NOT NULL DEFAULT 0,
                acertos INTEGER NOT NULL DEFAULT 0,
                erros INTEGER NOT NULL DEFAULT 0
            );
            CREATE INDEX entrada_criado_em ON entrada(criado_em DESC);
            CREATE TABLE dia_revisado (
                dia INTEGER NOT NULL PRIMARY KEY,
                revisoes INTEGER NOT NULL DEFAULT 0
            );
        """
    }
}
