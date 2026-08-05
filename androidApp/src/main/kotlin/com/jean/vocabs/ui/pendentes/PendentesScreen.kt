package com.jean.vocabs.ui.pendentes

import com.jean.vocabs.ui.textoTemporarioDoErro
import com.jean.vocabs.ui.displayName
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
import androidx.compose.material3.Icon
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
import com.jean.vocabs.shared.domain.Capture
import com.jean.vocabs.shared.domain.Entry
import com.jean.vocabs.shared.domain.EntryStatus
import com.jean.vocabs.ui.components.ArrastarParaExcluir
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
import com.jean.vocabs.ui.languages.idiomaDe

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
 *
 * Todo cartão daqui sai arrastando para o lado. É a única tela do app em que
 * isso vale, e por um motivo: fila é a coisa que se limpa. Descartar era, até
 * aqui, uma viagem de ida e volta — abrir a captura, achar o botão, confirmar —
 * para dizer "isto não era nada", e uma fila que custa isso para encolher é uma
 * fila que ninguém encolhe. O gesto se explica em [ArrastarParaExcluir]; a
 * segunda chance, em [FaixaDeDesfazer].
 */
@Composable
fun PendentesScreen(
    aoAbrirCaptura: (Capture) -> Unit,
    aoAbrirFicha: (Entry) -> Unit,
    vm: PendentesViewModel = viewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    var filtro by remember { mutableStateOf<String?>(null) }

    val captures = estado.captures.filter { filtro == null || it.languagePair.target == filtro }
    val cards = estado.cards.filter { filtro == null || it.languagePair.target == filtro }
    val maisAntiga = captures.minOfOrNull(Capture::createdAt)
    val languages = estado.porIdioma

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(9.dp),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
    ) {
        item(key = "cabecalho") {
            Text("Pendentes", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 22.dp))
            Text(
                text = resumoDaFila(captures.size, cards.size, maisAntiga),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
            if (estado.total > 0) DicaDeArrastar()
        }

        if (languages.size > 1) {
            item(key = "filtros") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp),
                ) {
                    item(key = "tudo") {
                        PilulaDeFiltroDeIdioma(
                            rotulo = "Tudo · ${estado.total}",
                            language = null,
                            selecionado = filtro == null,
                            aoClicar = { filtro = null },
                        )
                    }
                    items(languages.entries.toList(), key = { it.key }) { (codigo, quantas) ->
                        PilulaDeFiltroDeIdioma(
                            rotulo = "${idiomaDe(codigo).displayName} · $quantas",
                            language = idiomaDe(codigo),
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
                    title = "Tudo em dia",
                    detail = "Nenhuma captura ou ficha esperando.",
                )
            }
        }

        items(captures, key = { "c${it.id}" }) { capture ->
            ArrastarParaExcluir(
                aoExcluir = { vm.deleteCapture(capture) },
                descricaoDaAcao = "Excluir captura",
                modifier = Modifier.animateItem().fillMaxWidth(),
            ) {
                LinhaDeLista(
                    aoClicar = { aoAbrirCaptura(capture) },
                    start = { DiscoDeCategoria(capture.format) },
                    end = {
                        Text(
                            text = tempoRelativo(capture.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    },
                ) {
                    Text(tituloDaCaptura(capture), style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    MarcaDeIdioma(idiomaDe(capture.languagePair.target), Modifier.padding(top = 3.dp))
                }
            }
        }

        if (cards.isNotEmpty()) {
            item(key = "secao-fichas") { RotuloDeSecao("Fichas sendo geradas", Modifier.padding(top = 10.dp)) }
            items(cards, key = { "e${it.id}" }) { entry ->
                ArrastarParaExcluir(
                    aoExcluir = { vm.excluirFicha(entry) },
                    descricaoDaAcao = "Excluir ficha",
                    modifier = Modifier.animateItem().fillMaxWidth(),
                ) {
                    CartaoEntrada(
                        entry = entry,
                        aoClicar = { aoAbrirFicha(entry) },
                        tentar = { vm.tentarDeNovo(entry.id) },
                    )
                }
            }
        }
    }
}

/**
 * A linha que ensina o gesto.
 *
 * Um gesto que só existe embaixo do dedo é um gesto que metade das pessoas nunca
 * encontra: não há seta, sombra ou borda que anuncie um arrasto. A frase fica —
 * não é um balão de primeira visita — porque ela custa uma linha de 12 sp em
 * cinza e resolve a única pergunta que a tela deixaria em aberto. Some junto com
 * a fila: numa tela vazia não há cartão nenhum para arrastar.
 */
@Composable
private fun DicaDeArrastar() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 7.dp),
    ) {
        Icon(
            imageVector = Icones.Lixeira,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = "Arraste um cartão para o lado para excluir",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun resumoDaFila(captures: Int, cards: Int, maisAntiga: Long?): String = when {
    captures > 0 -> buildString {
        append(captures)
        append(if (captures == 1) " captura crua" else " capturas cruas")
        maisAntiga?.let { append(" · a mais antiga ${tempoRelativo(it)}") }
    }
    cards > 0 -> "$cards ${if (cards == 1) "card em processamento" else "cards em processamento"}"
    else -> "Suas capturas e fichas em processamento aparecem aqui."
}

@Composable
private fun CartaoEntrada(
    entry: Entry,
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
                Text(entry.title, style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                    MarcaDeIdioma(idiomaDe(entry.languagePair.target))
                    if (entry.status == EntryStatus.ERROR) {
                        Text(
                            text = " · ${textoTemporarioDoErro(entry.errorCode)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                        )
                    }
                }
            }
            if (entry.status == EntryStatus.GENERATING) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            if (entry.status == EntryStatus.ERROR) Pilula("tentar de novo", destaque = true, aoClicar = tentar)
        }
    }
}
