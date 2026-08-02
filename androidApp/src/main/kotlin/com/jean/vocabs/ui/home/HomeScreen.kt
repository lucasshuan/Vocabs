package com.jean.vocabs.ui.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.shared.domain.NivelMemoria
import com.jean.vocabs.ui.components.BarraDeMemoria
import com.jean.vocabs.ui.components.CartaoDaTela
import com.jean.vocabs.ui.components.EstadoVazio
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.PilulaSelecionavel
import com.jean.vocabs.ui.components.TipoBadge
import com.jean.vocabs.ui.components.corDoRotuloDoNivel
import com.jean.vocabs.ui.components.rotuloDoNivel
import com.jean.vocabs.ui.components.textoDaProximaRevisao

@Composable
fun HomeScreen(
    aoAbrirFicha: (Long) -> Unit,
    aoRevisar: () -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
    ) {
        item {
            Text("Vocabulários", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 22.dp))
            Text(
                "${estado.total} ${if (estado.total == 1) "ficha" else "fichas"} · ${estado.dominadas} dominadas",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
            OutlinedTextField(
                value = estado.busca,
                onValueChange = vm::buscar,
                leadingIcon = { Icon(Icones.Lupa, null) },
                placeholder = { Text("Buscar palavra ou expressão") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 12.dp),
            ) {
                FiltroMemoria.entries.forEach { filtro ->
                    PilulaSelecionavel(filtro.rotulo, estado.filtro == filtro) { vm.filtrar(filtro) }
                }
            }
        }

        if (estado.carregado && estado.entradas.isEmpty()) {
            item {
                EstadoVazio(
                    icone = Icones.Cartas,
                    titulo = if (estado.total == 0) "Sua coleção começa aqui" else "Nenhuma ficha encontrada",
                    detalhe = if (estado.total == 0) "Capture um trecho e escolha o que quer aprender." else "Tente outra busca ou filtro.",
                )
            }
        }

        items(estado.entradas, key = { it.id }) { entrada ->
            CartaoPalavra(entrada) { aoAbrirFicha(entrada.id) }
        }
    }
}

@Composable
private fun CartaoPalavra(entrada: Entrada, aoClicar: () -> Unit) {
    val agora = System.currentTimeMillis()
    val nivel = entrada.retencao?.nivelEm(agora) ?: NivelMemoria.NOVA
    val pontos = entrada.retencao?.pontosEm(agora) ?: 0.0
    val proxima = textoDaProximaRevisao(entrada.retencao, agora)
    val naFila = proxima == "revisar agora"

    CartaoDaTela(modifier = Modifier.fillMaxWidth(), aoClicar = aoClicar) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(entrada.titulo, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            TipoBadge(entrada.tipo)
        }
        Text(
            entrada.ficha?.traducao.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 9.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 11.dp)) {
            BarraDeMemoria(pontos, nivel, Modifier.width(84.dp), altura = 6.dp)
            Text(
                text = rotuloDoNivel(nivel),
                style = MaterialTheme.typography.bodySmall,
                color = corDoRotuloDoNivel(nivel),
                modifier = Modifier.padding(start = 9.dp).weight(1f),
            )
            Text(
                text = proxima,
                style = MaterialTheme.typography.bodySmall,
                color = if (naFila) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
