package com.jean.vocabs.ui.progress

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
import com.jean.vocabs.shared.domain.Steps
import com.jean.vocabs.shared.domain.Entry
import com.jean.vocabs.shared.domain.MemoryLevel
import com.jean.vocabs.ui.components.InnerHeader
import com.jean.vocabs.ui.components.ScreenCard
import com.jean.vocabs.ui.components.EmptyState
import com.jean.vocabs.ui.components.AppIcons
import com.jean.vocabs.ui.components.ListRow
import com.jean.vocabs.ui.components.SelectablePill
import com.jean.vocabs.ui.components.levelLabel
import com.jean.vocabs.ui.components.nextReviewText

/**
 * Tela 4c do handoff — "O que falta".
 *
 * Cada palavra com o degrau em que está e quantos acertos faltam para o próximo
 * nome. É a tela que fala em **degraus**, e não em força de memória: a pergunta
 * aqui é o que fazer, e a força de memória responde outra coisa — quanto se
 * lembra agora, que anda para trás sozinha e não é uma tarefa.
 */
@Composable
fun WhatsLeftScreen(
    target: String?,
    aoVoltar: () -> Unit,
    aoAbrirFicha: (Long) -> Unit,
    vm: ProgressViewModel = viewModel(),
) {
    LaunchedEffect(target) { vm.abrir(target) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    var soPerto by remember { mutableStateOf(true) }

    val perto = estado.pertoDeVirar
    val lista = remember(estado.words, soPerto) {
        val base = if (soPerto) perto else estado.words.filter { Steps.level(it.degrau) != MemoryLevel.MASTERED }
        base.sortedBy { it.degrau }
    }
    val mastered = estado.mastered

    Column(
        verticalArrangement = Arrangement.spacedBy(13.dp),
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp),
    ) {
        InnerHeader("O que falta", aoVoltar, Modifier.padding(top = 8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectablePill(
                rotulo = "Perto de virar · ${perto.size}",
                selecionada = soPerto,
                aoClicar = { soPerto = true },
            )
            SelectablePill(
                rotulo = "Todas · ${estado.total}",
                selecionada = !soPerto,
                aoClicar = { soPerto = false },
            )
        }

        if (lista.isEmpty()) {
            EmptyState(
                icon = AppIcons.Check,
                title = if (soPerto) "Nenhuma está perto" else "Nada em aberto",
                detail = if (soPerto) {
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
                items(lista, key = { it.id }) { entry ->
                    WordRow(entry, aoClicar = { aoAbrirFicha(entry.id) })
                }
                if (mastered > 0) {
                    item {
                        ListRow(
                            title = "$mastered já ${if (mastered == 1) "dominada" else "mastered"}",
                            detail = "só voltam de mês em mês",
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
private fun WordRow(entry: Entry, aoClicar: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    val degrau = entry.degrau
    val level = Steps.level(degrau)
    val faltam = Steps.hitsToLevelUp(degrau)
    val proxima = nextReviewText(entry.retention, System.currentTimeMillis())

    ScreenCard(
        aoClicar = aoClicar,
        forma = MaterialTheme.shapes.medium,
        recheio = PaddingValues(horizontal = 15.dp, vertical = 13.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(entry.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            proxima?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (it == "revisar agora") cores.primary else cores.onSurfaceVariant,
                )
            }
        }

        entry.card?.translation?.takeIf(String::isNotBlank)?.let {
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
            StepLadder(degrau, Modifier.weight(1f))
            Text(
                text = "${levelLabel(level)} · degrau $degrau de ${Steps.TOTAL}",
                style = MaterialTheme.typography.bodySmall,
                color = cores.onSurfaceVariant,
                modifier = Modifier.padding(start = 10.dp),
            )
        }

        if (faltam > 0) {
            Text(
                text = whatsLeftText(faltam, Steps.level(degrau + faltam)),
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
private fun StepLadder(degrau: Int, modifier: Modifier = Modifier) {
    val cores = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = modifier) {
        repeat(Steps.TOTAL) { indice ->
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

internal fun whatsLeftText(hits: Int, proximoNivel: MemoryLevel): String {
    val name = levelLabel(proximoNivel)
    return if (hits == 1) "1 acerto para $name" else "$hits acertos para $name"
}
