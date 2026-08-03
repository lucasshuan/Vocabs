package com.jean.vocabs.ui.pendentes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.shared.domain.Captura
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.shared.domain.StatusEntrada
import com.jean.vocabs.ui.components.CartaoDaTela
import com.jean.vocabs.ui.components.DiscoDeCategoria
import com.jean.vocabs.ui.components.EstadoVazio
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.LinhaDeLista
import com.jean.vocabs.ui.components.MarcaDeIdioma
import com.jean.vocabs.ui.components.Pilula
import com.jean.vocabs.ui.components.PilulaDeFiltroDeIdioma
import com.jean.vocabs.ui.components.RotuloDeSecao
import com.jean.vocabs.ui.components.tempoRelativo
import com.jean.vocabs.ui.components.tituloDaCaptura
import com.jean.vocabs.ui.idiomas.idiomaDe

/**
 * Tela 05 do handoff — "Pendentes", de todos os idiomas.
 *
 * O filtro por bandeira é escolha manual e mora **na composição**, não no
 * ViewModel: sair da aba o desfaz. Um recorte que sobrevivesse à saída faria a
 * fila parecer menor do que é na próxima visita, que é exatamente o erro que
 * este handoff veio corrigir.
 *
 * Em toda linha o subtexto é o idioma. O estado de transcrição saiu dali porque
 * o disco colorido à esquerda já diz se aquilo é áudio, foto ou texto — e o
 * idioma, que é decidido na gravação, não aparecia em lugar nenhum.
 */
@Composable
fun PendentesScreen(
    aoAbrirCaptura: (Captura) -> Unit,
    aoAbrirFicha: (Entrada) -> Unit,
    vm: PendentesViewModel = viewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    var filtro by remember { mutableStateOf<String?>(null) }

    val capturas = estado.capturas.filter { filtro == null || it.par.alvo == filtro }
    val fichas = estado.fichas.filter { filtro == null || it.par.alvo == filtro }
    val maisAntiga = capturas.minOfOrNull(Captura::criadoEm)
    val idiomas = estado.porIdioma

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(9.dp),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
    ) {
        item(key = "cabecalho") {
            Text("Pendentes", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 22.dp))
            Text(
                text = resumoDaFila(capturas.size, fichas.size, maisAntiga),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
        }

        if (idiomas.size > 1) {
            item(key = "filtros") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp),
                ) {
                    item(key = "tudo") {
                        PilulaDeFiltroDeIdioma(
                            rotulo = "Tudo · ${estado.total}",
                            idioma = null,
                            selecionado = filtro == null,
                            aoClicar = { filtro = null },
                        )
                    }
                    items(idiomas.entries.toList(), key = { it.key }) { (codigo, quantas) ->
                        PilulaDeFiltroDeIdioma(
                            rotulo = "${idiomaDe(codigo).nome} · $quantas",
                            idioma = idiomaDe(codigo),
                            selecionado = filtro == codigo,
                            aoClicar = { filtro = if (filtro == codigo) null else codigo },
                        )
                    }
                }
            }
        }

        if (estado.total == 0) {
            item(key = "vazio") {
                EstadoVazio(
                    icone = Icones.Check,
                    titulo = "Tudo em dia",
                    detalhe = "Nenhuma captura ou ficha esperando.",
                )
            }
        }

        items(capturas, key = { "c${it.id}" }) { captura ->
            LinhaDeLista(
                modifier = Modifier.animateItem(),
                aoClicar = { aoAbrirCaptura(captura) },
                inicio = { DiscoDeCategoria(captura.formato) },
                fim = {
                    Text(
                        text = tempoRelativo(captura.criadoEm),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                },
            ) {
                Text(tituloDaCaptura(captura), style = MaterialTheme.typography.titleSmall, maxLines = 1)
                MarcaDeIdioma(idiomaDe(captura.par.alvo), Modifier.padding(top = 3.dp))
            }
        }

        if (fichas.isNotEmpty()) {
            item(key = "secao-fichas") { RotuloDeSecao("Fichas sendo geradas", Modifier.padding(top = 10.dp)) }
            items(fichas, key = { "e${it.id}" }) { entrada ->
                CartaoEntrada(
                    entrada = entrada,
                    modifier = Modifier.animateItem(),
                    aoClicar = { aoAbrirFicha(entrada) },
                    tentar = { vm.tentarDeNovo(entrada.id) },
                )
            }
        }
    }
}

private fun resumoDaFila(capturas: Int, fichas: Int, maisAntiga: Long?): String = when {
    capturas > 0 -> buildString {
        append(capturas)
        append(if (capturas == 1) " captura crua" else " capturas cruas")
        maisAntiga?.let { append(" · a mais antiga ${tempoRelativo(it)}") }
    }
    fichas > 0 -> "$fichas ${if (fichas == 1) "ficha em processamento" else "fichas em processamento"}"
    else -> "Suas capturas e fichas em processamento aparecem aqui."
}

@Composable
private fun CartaoEntrada(
    entrada: Entrada,
    aoClicar: () -> Unit,
    tentar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CartaoDaTela(
        forma = MaterialTheme.shapes.medium,
        recheio = PaddingValues(horizontal = 15.dp, vertical = 14.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clickable(onClick = aoClicar)) {
                Text(entrada.titulo, style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                    MarcaDeIdioma(idiomaDe(entrada.par.alvo))
                    if (entrada.status == StatusEntrada.ERRO) {
                        Text(
                            text = " · ${entrada.erro ?: "falha na geração"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                        )
                    }
                }
            }
            if (entrada.status == StatusEntrada.GERANDO) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            if (entrada.status == StatusEntrada.ERRO) Pilula("tentar de novo", destaque = true, aoClicar = tentar)
        }
    }
}
