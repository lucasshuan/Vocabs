package com.jean.vocabs.ui.progresso

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.shared.domain.Degraus
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.shared.domain.MemoryLevel
import com.jean.vocabs.ui.components.CabecalhoDeDentro
import com.jean.vocabs.ui.components.CartaoDaTela
import com.jean.vocabs.ui.components.EstadoVazio
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.LinhaDeLista
import com.jean.vocabs.ui.components.PilulaSelecionavel
import com.jean.vocabs.ui.components.rotuloDoNivel
import com.jean.vocabs.ui.components.textoDaProximaRevisao

/**
 * Tela 4c do handoff — "O que falta".
 *
 * Cada palavra com o degrau em que está e quantos acertos faltam para o próximo
 * nome. É a tela que fala em **degraus**, e não em força de memória: a pergunta
 * aqui é o que fazer, e a força de memória responde outra coisa — quanto se
 * lembra agora, que anda para trás sozinha e não é uma tarefa.
 */
@Composable
fun OQueFaltaScreen(
    alvo: String?,
    aoVoltar: () -> Unit,
    aoAbrirFicha: (Long) -> Unit,
    vm: ProgressoViewModel = viewModel(),
) {
    LaunchedEffect(alvo) { vm.abrir(alvo) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    var soPerto by remember { mutableStateOf(true) }

    val perto = estado.pertoDeVirar
    val lista = remember(estado.palavras, soPerto) {
        val base = if (soPerto) perto else estado.palavras.filter { Degraus.nivel(it.degrau) != MemoryLevel.MASTERED }
        base.sortedBy { it.degrau }
    }
    val dominadas = estado.dominadas

    Column(
        verticalArrangement = Arrangement.spacedBy(13.dp),
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp),
    ) {
        CabecalhoDeDentro("O que falta", aoVoltar, Modifier.padding(top = 8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PilulaSelecionavel(
                rotulo = "Perto de virar · ${perto.size}",
                selecionada = soPerto,
                aoClicar = { soPerto = true },
            )
            PilulaSelecionavel(
                rotulo = "Todas · ${estado.total}",
                selecionada = !soPerto,
                aoClicar = { soPerto = false },
            )
        }

        if (lista.isEmpty()) {
            EstadoVazio(
                icone = Icones.Check,
                titulo = if (soPerto) "Nenhuma está perto" else "Nada em aberto",
                detalhe = if (soPerto) {
                    "Acerte mais uma vez e a palavra aparece aqui."
                } else {
                    "Todas as palavras já estão dominadas."
                },
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(lista, key = { it.id }) { entrada ->
                    LinhaDaPalavra(entrada, aoClicar = { aoAbrirFicha(entrada.id) })
                }
                if (dominadas > 0) {
                    item {
                        LinhaDeLista(
                            titulo = "$dominadas já ${if (dominadas == 1) "dominada" else "dominadas"}",
                            detalhe = "só voltam de mês em mês",
                            modifier = Modifier.padding(top = 9.dp),
                        )
                    }
                }
                item { Spacer(Modifier.navigationBarsPadding().height(110.dp)) }
            }
        }
    }
}

@Composable
private fun LinhaDaPalavra(entrada: Entrada, aoClicar: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    val degrau = entrada.degrau
    val nivel = Degraus.nivel(degrau)
    val faltam = Degraus.acertosParaSubirDeNivel(degrau)
    val proxima = textoDaProximaRevisao(entrada.retencao, System.currentTimeMillis())

    CartaoDaTela(
        aoClicar = aoClicar,
        forma = MaterialTheme.shapes.medium,
        recheio = PaddingValues(horizontal = 15.dp, vertical = 13.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(entrada.titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            proxima?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (it == "revisar agora") cores.primary else cores.onSurfaceVariant,
                )
            }
        }

        entrada.ficha?.translation?.takeIf(String::isNotBlank)?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = cores.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        ) {
            EscadaDeDegraus(degrau, Modifier.weight(1f))
            Text(
                text = "${rotuloDoNivel(nivel)} · degrau $degrau de ${Degraus.TOTAL}",
                style = MaterialTheme.typography.bodySmall,
                color = cores.onSurfaceVariant,
                modifier = Modifier.padding(start = 10.dp),
            )
        }

        if (faltam > 0) {
            Text(
                text = textoDoQueFalta(faltam, Degraus.nivel(degrau + faltam)),
                style = MaterialTheme.typography.bodySmall,
                color = cores.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * A escada como cinco traços.
 *
 * Cinco traços separados e não uma barra contínua: degrau é contagem, e uma
 * barra sugeriria que existem posições entre um degrau e o seguinte — que é
 * justamente o que a força de memória mostra, na outra tela.
 */
@Composable
private fun EscadaDeDegraus(degrau: Int, modifier: Modifier = Modifier) {
    val cores = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = modifier) {
        repeat(Degraus.TOTAL) { indice ->
            val alcancado = indice < degrau
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(
                        color = if (alcancado) cores.tertiary else cores.outlineVariant,
                        shape = RoundedCornerShape(3.dp),
                    ),
            )
        }
    }
}

internal fun textoDoQueFalta(acertos: Int, proximoNivel: MemoryLevel): String {
    val nome = rotuloDoNivel(proximoNivel)
    return if (acertos == 1) "1 acerto para $nome" else "$acertos acertos para $nome"
}
