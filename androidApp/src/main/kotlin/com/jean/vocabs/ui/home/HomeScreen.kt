package com.jean.vocabs.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.shared.domain.FormatoCaptura
import com.jean.vocabs.shared.domain.ResumoRevisao
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.TipoBadge
import com.jean.vocabs.ui.components.escalaAoPressionar
import com.jean.vocabs.ui.components.tempoAte

@Composable
fun HomeScreen(
    aoAbrirFicha: (Long) -> Unit,
    aoRevisar: () -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()

    if (!estado.carregado) return

    if (estado.total == 0) {
        EstadoVazio()
        return
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 140.dp),
        verticalItemSpacing = 12.dp,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            Cabecalho(total = estado.total)
        }

        // Acima dos filtros: filtro é navegação, e este cartão é a única chamada
        // para ação da tela.
        estado.revisao?.let { revisao ->
            item(span = StaggeredGridItemSpan.FullLine) {
                BlocoRevisao(revisao = revisao, aoRevisar = aoRevisar)
            }
        }

        if (estado.origens.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                LinhaFiltros(
                    origens = estado.origens,
                    filtro = estado.filtro,
                    aoFiltrar = vm::filtrar,
                )
            }
        }

        if (estado.entradas.isEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Text(
                    text = "Nada capturado de “${estado.filtro}” ainda.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }

        items(estado.entradas, key = { it.id }) { entrada ->
            CartaoEntrada(
                entrada = entrada,
                aoClicar = { aoAbrirFicha(entrada.id) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun Cabecalho(total: Int) {
    Column(modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)) {
        Text(
            text = "Vocabs",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = if (total == 1) "1 palavra aprendida" else "$total palavras aprendidas",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/**
 * Com fila, um cartão; sem fila, uma linha discreta.
 *
 * Sumir quando não há nada a revisar seria o pior dos dois mundos: o recurso
 * ficaria invisível justamente no dia em que você está em dia, e não daria para
 * saber se ele está funcionando.
 */
@Composable
private fun BlocoRevisao(revisao: ResumoRevisao, aoRevisar: () -> Unit) {
    if (revisao.naFila == 0) {
        val quando = revisao.proximaEmMillis?.let { " — a próxima volta ${tempoAte(it)}." } ?: "."
        Text(
            text = "Nada pedindo revisão agora$quando",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        return
    }

    val interacao = remember { MutableInteractionSource() }
    val cores = MaterialTheme.colorScheme

    Surface(
        onClick = aoRevisar,
        shape = MaterialTheme.shapes.large,
        color = cores.primaryContainer,
        interactionSource = interacao,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .escalaAoPressionar(interacao),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .background(cores.primary, CircleShape),
            ) {
                Icon(
                    imageVector = Icones.Repetir,
                    contentDescription = null,
                    tint = cores.onPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
            ) {
                Text(
                    text = if (revisao.naFila == 1) {
                        "1 palavra pedindo revisão"
                    } else {
                        "${revisao.naFila} palavras pedindo revisão"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = cores.onPrimaryContainer,
                )
                if (revisao.diasSeguidos >= 2) {
                    Text(
                        text = "${revisao.diasSeguidos} dias seguidos",
                        style = MaterialTheme.typography.bodySmall,
                        color = cores.onPrimaryContainer.copy(alpha = 0.75f),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Text(
                text = "→",
                style = MaterialTheme.typography.titleMedium,
                color = cores.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun LinhaFiltros(
    origens: List<String>,
    filtro: String?,
    aoFiltrar: (String?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ChipFiltro(
            texto = "Todas",
            selecionado = filtro == null,
            aoClicar = { aoFiltrar(null) },
        )
        origens.forEach { origem ->
            val ativo = filtro?.equals(origem, ignoreCase = true) == true
            ChipFiltro(
                texto = origem.replaceFirstChar { it.uppercase() },
                selecionado = ativo,
                aoClicar = { aoFiltrar(if (ativo) null else origem) },
            )
        }
    }
}

@Composable
private fun ChipFiltro(texto: String, selecionado: Boolean, aoClicar: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    val fundo by animateColorAsState(
        targetValue = if (selecionado) cores.inverseSurface else cores.surface,
        animationSpec = tween(200),
        label = "fundoChip",
    )
    val conteudo by animateColorAsState(
        targetValue = if (selecionado) cores.inverseOnSurface else cores.onSurfaceVariant,
        animationSpec = tween(200),
        label = "conteudoChip",
    )

    Surface(
        onClick = aoClicar,
        shape = RoundedCornerShape(50),
        color = fundo,
        border = if (selecionado) null else BorderStroke(1.dp, cores.outline),
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = conteudo,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun CartaoEntrada(
    entrada: Entrada,
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interacao = remember { MutableInteractionSource() }

    Surface(
        onClick = aoClicar,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        interactionSource = interacao,
        modifier = modifier
            .fillMaxWidth()
            .escalaAoPressionar(interacao),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = entrada.titulo,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                MarcaDeFormato(entrada.formato)
            }

            entrada.ficha?.let { ficha ->
                Text(
                    text = ficha.traducao,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            entrada.trecho?.let { trecho ->
                Text(
                    text = trecho,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            entrada.ficha?.let { ficha ->
                Box(modifier = Modifier.padding(top = 10.dp)) {
                    TipoBadge(ficha.tipo)
                }
            }
        }
    }
}

/** Lembra que a palavra nasceu de uma foto ou de um áudio, não de digitação. */
@Composable
private fun MarcaDeFormato(formato: FormatoCaptura) {
    val icone = when (formato) {
        FormatoCaptura.FOTO -> Icones.Camera
        FormatoCaptura.AUDIO -> Icones.Microfone
        FormatoCaptura.TEXTO -> return
    }
    Icon(
        imageVector = icone,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(start = 8.dp, top = 4.dp)
            .size(15.dp),
    )
}

@Composable
private fun EstadoVazio() {
    val pulso by rememberInfiniteTransition(label = "vazio").animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "pulsoVazio",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .size(76.dp)
                .graphicsLayer {
                    scaleX = pulso
                    scaleY = pulso
                },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icones.Mais,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
        Text(
            text = "Nenhuma ficha ainda",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            text = "Capture uma palavra — escrevendo, falando ou fotografando — e ela aparece aqui assim que a ficha ficar pronta.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
