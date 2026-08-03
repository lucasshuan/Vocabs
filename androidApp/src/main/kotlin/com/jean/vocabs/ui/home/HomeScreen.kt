package com.jean.vocabs.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.shared.domain.NivelMemoria
import com.jean.vocabs.ui.components.BandeiraCircular
import com.jean.vocabs.ui.components.BarraDeMemoria
import com.jean.vocabs.ui.components.CartaoDaTela
import com.jean.vocabs.ui.components.EstadoVazio
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.PilulaSelecionavel
import com.jean.vocabs.ui.components.TipoBadge
import com.jean.vocabs.ui.components.corDoRotuloDoNivel
import com.jean.vocabs.ui.components.rotuloDoNivel
import com.jean.vocabs.ui.components.textoDaProximaRevisao
import com.jean.vocabs.ui.idiomas.idiomaDe

/**
 * Tela 06 do handoff — "Vocabulários", os três idiomas juntos.
 *
 * Os níveis continuam logo abaixo da busca, onde sempre estiveram. O que mudou é
 * o idioma: ele deixou de ser mais uma pílula na mesma fileira e virou cabeçalho
 * de grupo. Duas fileiras de filtro na mesma tela ensinariam que idioma e nível
 * são a mesma espécie de escolha, e eles não são — nível é um recorte, idioma é
 * uma divisão.
 */
@Composable
fun HomeScreen(
    aoAbrirFicha: (Long) -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
    ) {
        item(key = "cabecalho") {
            Text("Vocabulários", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 22.dp))
            Text(
                text = "${estado.total} ${if (estado.total == 1) "ficha" else "fichas"} · ${estado.dominadas} dominadas",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
            OutlinedTextField(
                value = estado.busca,
                onValueChange = vm::buscar,
                leadingIcon = { Icon(Icones.Lupa, null) },
                placeholder = { Text("Buscar em todos os idiomas") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 12.dp),
            ) {
                FiltroMemoria.entries.forEach { filtro ->
                    PilulaSelecionavel(filtro.rotulo, estado.filtro == filtro, aoClicar = { vm.filtrar(filtro) })
                }
            }
        }

        if (estado.carregado && estado.total == 0) {
            item(key = "vazio") {
                EstadoVazio(
                    icone = Icones.Cartas,
                    titulo = "Sua coleção começa aqui",
                    detalhe = "Capture um trecho e escolha o que quer aprender.",
                )
            }
        } else if (estado.carregado && estado.encontradas == 0) {
            item(key = "sem-resultado") {
                EstadoVazio(
                    icone = Icones.Lupa,
                    titulo = "Nenhuma ficha encontrada",
                    detalhe = "Tente outra busca ou outro nível.",
                )
            }
        }

        // Busca sem resultado nenhum mostra só o vazio: repetir três cabeçalhos
        // sem nada embaixo transformaria a resposta numa lista de nãos.
        val comGrupos = estado.encontradas > 0

        estado.grupos.forEach { grupo ->
            if (grupo.total == 0 || !comGrupos || grupo.vazioPorFiltro) return@forEach

            item(key = "g${grupo.par.alvo}") {
                CabecalhoDeIdioma(
                    grupo = grupo,
                    modifier = Modifier.animateItem(),
                    aoAlternar = { vm.alternarGrupo(grupo.par.alvo) },
                )
            }
            if (!grupo.recolhido) {
                items(grupo.entradas, key = { "e${it.id}" }) { entrada ->
                    CartaoPalavra(entrada, Modifier.animateItem()) { aoAbrirFicha(entrada.id) }
                }
            }
        }
    }
}

/**
 * O cabeçalho de um idioma: bandeira, nome, o que há dentro e a chevron.
 *
 * A chevron gira em vez de trocar de ícone — é o mesmo elemento mudando de
 * estado, e o giro de 90° diz para onde a lista foi sem exigir que ninguém
 * compare dois desenhos.
 */
@Composable
private fun CabecalhoDeIdioma(
    grupo: GrupoDeIdioma,
    aoAlternar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme
    val aberto = !grupo.recolhido
    val giro by animateFloatAsState(
        targetValue = if (aberto) 90f else 0f,
        animationSpec = tween(200),
        label = "giroDoCabecalho",
    )

    Surface(
        onClick = aoAlternar,
        shape = RoundedCornerShape(12.dp),
        color = if (aberto) cores.secondaryContainer else cores.surfaceVariant,
        modifier = modifier.fillMaxWidth().padding(top = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            BandeiraCircular(idiomaDe(grupo.par.alvo), tamanho = 20.dp)
            Text(
                text = idiomaDe(grupo.par.alvo).nome,
                style = MaterialTheme.typography.titleSmall,
                color = if (aberto) cores.onSecondaryContainer else cores.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (grupo.naFila > 0) "${grupo.total} · ${grupo.naFila} para revisar" else "${grupo.total} · em dia",
                style = MaterialTheme.typography.bodySmall,
                color = if (grupo.naFila > 0) cores.primary else cores.onSurfaceVariant,
            )
            Icon(
                imageVector = Icones.Avancar,
                contentDescription = if (aberto) "Recolher" else "Expandir",
                tint = if (aberto) cores.primary else cores.onSurfaceVariant,
                modifier = Modifier.size(16.dp).rotate(giro),
            )
        }
    }
}

@Composable
private fun CartaoPalavra(entrada: Entrada, modifier: Modifier = Modifier, aoClicar: () -> Unit) {
    val agora = System.currentTimeMillis()
    val nivel = entrada.retencao?.nivelEm(agora) ?: NivelMemoria.NOVA
    val pontos = entrada.retencao?.pontosEm(agora) ?: 0.0
    val proxima = textoDaProximaRevisao(entrada.retencao, agora)
    val naFila = entrada.precisaRevisar(agora)

    CartaoDaTela(modifier = modifier.fillMaxWidth(), aoClicar = aoClicar) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(entrada.titulo, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            TipoBadge(entrada.tipo)
        }
        Text(
            text = entrada.ficha?.traducao.orEmpty(),
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
            proxima?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (naFila) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
