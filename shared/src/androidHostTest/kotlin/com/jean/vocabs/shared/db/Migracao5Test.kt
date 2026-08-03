package com.jean.vocabs.shared.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Migracao5Test {

    @Test
    fun `migracao renomeia ipa preserva ficha e adota o par padrao`() {
        val driver = comEsquemaV5()

        VocabsDatabase.Schema.migrate(driver, 5, 6)
        val queries = VocabsDatabase(driver).vocabsQueries
        val entrada = queries.buscarEntradaPorId(7).executeAsOne()

        assertEquals("/vɜːdənt/", entrada.pronuncia)
        assertEquals("verdejante", entrada.traducao)
        assertEquals(82.0, entrada.pontos)
        assertEquals(3L, entrada.acertos)
        assertEquals(1L, entrada.erros)

        // Tudo o que já existia nasceu no único par que existia.
        assertEquals("pt-BR", entrada.idioma_nativo)
        assertEquals("en", entrada.idioma_alvo)
    }

    @Test
    fun `migracao reconstroi so as capturas na linha do tempo`() {
        val driver = comEsquemaV5()

        VocabsDatabase.Schema.migrate(driver, 5, 6)
        val queries = VocabsDatabase(driver).vocabsQueries
        val eventos = queries.listarEventosDesde(Long.MIN_VALUE) { _, entradaId, _, instante, tipo, detalhe, alvo, _, _ ->
            listOf(entradaId.toString(), instante.toString(), tipo, detalhe.orEmpty(), alvo)
        }.executeAsList()

        // Uma linha só, e é a captura: o desfecho das revisões antigas não estava
        // guardado, e inventá-lo encheria a tela Dia a dia de dias que ninguém viveu.
        assertEquals(1, eventos.size)
        assertEquals(listOf("7", "1000", "CAPTURADA", "", "verdant"), eventos.single())
    }

    @Test
    fun `entrada nova pode nascer noutro par sem tocar nas antigas`() {
        val driver = comEsquemaV5()
        VocabsDatabase.Schema.migrate(driver, 5, 6)
        val queries = VocabsDatabase(driver).vocabsQueries

        queries.inserirCaptura(
            trecho = "der Zaun",
            origem = null,
            criado_em = 2000,
            status = "PROCESSADA",
            formato = "TEXTO",
            midia_caminho = null,
            duracao_ms = null,
            erro_transcricao = null,
            idioma_nativo = "pt-BR",
            idioma_alvo = "de",
        )
        val capturaId = queries.ultimoIdInserido().executeAsOne()
        queries.inserirEntrada(capturaId, "Zaun", 4, 8, "PALAVRA", "PENDENTE")

        val pares = queries.listarTodasEntradas().executeAsList().map { it.idioma_alvo }
        assertTrue(pares.containsAll(listOf("en", "de")), pares.toString())
    }

    /** O banco como ele fica depois da `4.sqm` — captura e entrada separadas, `ipa` ainda no lugar. */
    private fun comEsquemaV5(): JdbcSqliteDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ESQUEMA_V5.split(';').map(String::trim).filter(String::isNotEmpty).forEach { sql ->
            driver.execute(null, sql, 0)
        }
        driver.execute(
            null,
            """INSERT INTO captura VALUES (
                7, 'A verdant field', 'Book', 1000, 'PROCESSADA', 'TEXTO', NULL, NULL, NULL
            )""",
            0,
        )
        driver.execute(
            null,
            """INSERT INTO entrada VALUES (
                7, 7, 'verdant', 2, 9, 'PALAVRA', 'PRONTA',
                'verdejante', '["Muito verde"]', 'A verdant garden.', '/vɜːdənt/', NULL, NULL,
                82.0, 12.0, 900, 4, 3, 1
            )""",
            0,
        )
        return driver
    }

    private companion object {
        const val ESQUEMA_V5 = """
            CREATE TABLE captura (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                trecho TEXT,
                origem TEXT,
                criado_em INTEGER NOT NULL,
                status TEXT NOT NULL,
                formato TEXT NOT NULL,
                midia_caminho TEXT,
                duracao_ms INTEGER,
                erro_transcricao TEXT
            );
            CREATE TABLE entrada (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                captura_id INTEGER NOT NULL REFERENCES captura(id) ON DELETE CASCADE,
                alvo TEXT NOT NULL,
                inicio INTEGER,
                fim INTEGER,
                tipo TEXT NOT NULL,
                status TEXT NOT NULL,
                traducao TEXT,
                definicoes_json TEXT,
                exemplo TEXT,
                ipa TEXT,
                relacionadas_json TEXT,
                erro TEXT,
                pontos REAL NOT NULL DEFAULT 100.0,
                taxa_decaimento REAL NOT NULL DEFAULT 40.0,
                data_ultima_interacao INTEGER NOT NULL DEFAULT 0,
                revisoes INTEGER NOT NULL DEFAULT 0,
                acertos INTEGER NOT NULL DEFAULT 0,
                erros INTEGER NOT NULL DEFAULT 0
            );
            CREATE INDEX captura_criado_em ON captura(criado_em DESC);
            CREATE INDEX captura_status ON captura(status);
            CREATE INDEX entrada_captura ON entrada(captura_id);
            CREATE INDEX entrada_status ON entrada(status);
            CREATE VIEW entrada_com_captura AS
            SELECT
                e.id, e.captura_id, e.alvo, e.inicio, e.fim, e.tipo, e.status,
                e.traducao, e.definicoes_json, e.exemplo, e.ipa, e.relacionadas_json, e.erro,
                e.pontos, e.taxa_decaimento, e.data_ultima_interacao, e.revisoes, e.acertos, e.erros,
                c.trecho, c.origem, c.criado_em, c.formato, c.midia_caminho, c.duracao_ms
            FROM entrada e JOIN captura c ON c.id = e.captura_id;
            CREATE TABLE dia_revisado (
                dia INTEGER NOT NULL PRIMARY KEY,
                revisoes INTEGER NOT NULL DEFAULT 0
            );
            CREATE TABLE uso_ia (
                mes TEXT NOT NULL PRIMARY KEY,
                geracoes INTEGER NOT NULL DEFAULT 0
            );
        """
    }
}
