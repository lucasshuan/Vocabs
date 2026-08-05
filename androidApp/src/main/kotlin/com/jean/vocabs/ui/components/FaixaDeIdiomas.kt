package com.jean.vocabs.ui.components

import com.jean.vocabs.ui.displayName
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jean.vocabs.contracts.Language
import com.jean.vocabs.shared.domain.CourseSummary
import com.jean.vocabs.shared.domain.CourseBadge
import com.jean.vocabs.ui.languages.idiomaDe

/**
 * A faixa de cursos do topo da Início.
 *
 * Duas regras a governam, e as duas são sobre confiança na posição:
 *
 * A **ordem é fixa** — nunca reordenada por pendência. A troca de página é um
 * deslize, e o deslize só é barato enquanto a memória muscular souber que o
 * espanhol fica à direita do inglês. Uma faixa que se reordena a cada revisão
 * vencida faria a pessoa reler as três bandeiras toda manhã.
 *
 * Toda bandeira **tem selo** — número em ameixa quando há o que revisar, tique
 * em menta quando está em dia, ampulheta cinza quando o curso ainda não tem nada
 * agendado. Nunca vazio, nunca um "0" escrito: o zero é a única boa notícia da
 * faixa, e escrevê-lo como número o transformaria num placar de nada feito.
 *
 * A rolagem acompanha a página: quem chega ao francês deslizando o carrossel
 * encontra o chip do francês já visível, sem precisar arrastar a faixa também.
 */
@Composable
fun FaixaDeIdiomas(
    courses: List<CourseSummary>,
    ativo: String,
    aoEscolher: (String) -> Unit,
    aoAdicionar: () -> Unit,
    modifier: Modifier = Modifier,
    recheio: PaddingValues = PaddingValues(horizontal = 20.dp),
) {
    val estado = rememberLazyListState()
    val indiceAtivo = courses.indexOfFirst { it.languagePair.target == ativo }

    LaunchedEffect(indiceAtivo) {
        if (indiceAtivo >= 0) estado.animateScrollToItem(indiceAtivo)
    }

    LazyRow(
        state = estado,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = recheio,
        modifier = modifier.fillMaxWidth(),
    ) {
        items(courses.size, key = { courses[it].languagePair.target }) { indice ->
            val course = courses[indice]
            ChipDeIdioma(
                language = idiomaDe(course.languagePair.target),
                badge = course.badge,
                selecionado = course.languagePair.target == ativo,
                aoClicar = { aoEscolher(course.languagePair.target) },
            )
        }
        item(key = "adicionar") { ChipDeAdicionarIdioma(aoAdicionar) }
    }
}

/**
 * Uma bandeira da faixa: disco, nome e selo.
 *
 * O selo do curso ativo inverte as cores porque o chip inteiro ficou ameixa — um
 * selo ameixa sobre ameixa desapareceria justo na única página que está aberta.
 */
@Composable
fun ChipDeIdioma(
    language: Language,
    badge: CourseBadge,
    selecionado: Boolean,
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme
    val fundo by animateColorAsState(
        targetValue = if (selecionado) cores.primary else cores.surface,
        animationSpec = tween(Movimento.RAPIDO),
        label = "fundoDoChip",
    )
    val text by animateColorAsState(
        targetValue = if (selecionado) cores.onPrimary else cores.onSurfaceVariant,
        animationSpec = tween(Movimento.RAPIDO),
        label = "textoDoChip",
    )

    val toque = lembrarToque()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .encolheAoTocar(toque, minimo = 0.94f)
            .height(38.dp)
            .clip(CircleShape)
            .background(fundo)
            .then(
                if (selecionado) Modifier
                else Modifier.border(1.dp, cores.outline, CircleShape),
            )
            .clickable(interactionSource = toque, indication = ripple(), onClick = aoClicar)
            .padding(start = 8.dp, end = 10.dp)
            .semantics { contentDescription = descricaoDoSelo(language.displayName, badge) },
    ) {
        BandeiraCircular(language, tamanho = 24.dp)
        Text(text = language.displayName, style = MaterialTheme.typography.titleSmall, color = text)
        SeloDeCursoBadge(badge, invertido = selecionado)
    }
}

/**
 * O selo, nos três estados.
 *
 * O número troca com uma transição vertical curta: quando uma revisão sai da
 * fila, o "3" sobe e o "2" entra por baixo — o movimento diz que o número desceu
 * sem precisar de mais nada na tela.
 */
@Composable
private fun SeloDeCursoBadge(badge: CourseBadge, invertido: Boolean) {
    val cores = MaterialTheme.colorScheme
    val fundo = when {
        badge is CourseBadge.Revisar && invertido -> cores.onPrimary
        badge is CourseBadge.Revisar -> cores.secondaryContainer
        badge is CourseBadge.EmDia -> cores.tertiaryContainer
        else -> cores.surfaceVariant
    }
    val tinta = when {
        badge is CourseBadge.Revisar && invertido -> cores.primary
        badge is CourseBadge.Revisar -> cores.primary
        badge is CourseBadge.EmDia -> cores.tertiary
        else -> cores.onSurfaceVariant
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .defaultMinSize(minWidth = 20.dp)
            .height(20.dp)
            .background(fundo, CircleShape),
    ) {
        when (badge) {
            is CourseBadge.Revisar -> AnimatedContent(
                targetState = badge.quantas,
                transitionSpec = {
                    val subindo = targetState < initialState
                    val entry = slideInVertically { altura -> if (subindo) altura else -altura } + fadeIn(tween(140))
                    val saida = slideOutVertically { altura -> if (subindo) -altura else altura } + fadeOut(tween(140))
                    entry togetherWith saida
                },
                label = "contagemDoSelo",
            ) { quantas ->
                Text(
                    text = quantas.coerceAtMost(99).toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = tinta,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
            CourseBadge.EmDia -> Icon(Icones.Check, null, tint = tinta, modifier = Modifier.size(12.dp))
            CourseBadge.Vazio -> Icon(Icones.Ampulheta, null, tint = tinta, modifier = Modifier.size(12.dp))
        }
    }
}

/** O `+` que fecha a faixa e leva para adicionar idioma. */
@Composable
private fun ChipDeAdicionarIdioma(aoClicar: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(38.dp)
            .width(42.dp)
            .clip(CircleShape)
            .clickable(onClick = aoClicar)
            .contornoTracejado(cores.outline, raio = 19.dp)
            .semantics { contentDescription = "Adicionar idioma" },
    ) {
        Icon(Icones.Mais, null, tint = cores.primary, modifier = Modifier.size(20.dp))
    }
}

/**
 * O chip de filtro de Pendentes: bandeira e contagem, ou só texto no "Tudo".
 *
 * O "Tudo" é o único sem bandeira de propósito — ele não é um idioma a mais na
 * fileira, é a ausência de recorte, e uma bandeira ali sugeriria o contrário.
 */
@Composable
fun PilulaDeFiltroDeIdioma(
    rotulo: String,
    language: Language?,
    selecionado: Boolean,
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme
    val fundo by animateColorAsState(
        targetValue = if (selecionado) cores.primary else cores.surface,
        animationSpec = tween(Movimento.RAPIDO),
        label = "fundoDoFiltro",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        modifier = modifier
            .height(32.dp)
            .clip(CircleShape)
            .background(fundo)
            .then(if (selecionado) Modifier else Modifier.border(1.dp, cores.outline, CircleShape))
            .clickable(onClick = aoClicar)
            .padding(start = if (language == null) 13.dp else 6.dp, end = 13.dp),
    ) {
        language?.let { BandeiraCircular(it, tamanho = 20.dp) }
        Text(
            text = rotulo,
            style = MaterialTheme.typography.labelMedium,
            color = if (selecionado) cores.onPrimary else cores.onSurfaceVariant,
        )
    }
}

/**
 * A pastilha de idioma que abre as linhas de Pendentes e os cabeçalhos de
 * Vocabulários: bandeira pequena e nome, sem estado.
 */
@Composable
fun MarcaDeIdioma(
    language: Language,
    modifier: Modifier = Modifier,
    cor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    tamanhoDaBandeira: androidx.compose.ui.unit.Dp = 15.dp,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        BandeiraCircular(language, tamanho = tamanhoDaBandeira)
        Text(language.displayName, style = MaterialTheme.typography.bodySmall, color = cor)
    }
}

/** O ponto do carrossel: uma barrinha para a página aberta, discos para as outras. */
@Composable
fun PontosDePagina(total: Int, current: Int, modifier: Modifier = Modifier) {
    if (total <= 1) return
    val cores = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = modifier) {
        repeat(total) { indice ->
            val aberto = indice == current
            val largura by androidx.compose.animation.core.animateDpAsState(
                targetValue = if (aberto) 16.dp else 5.dp,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "larguraDoPonto",
            )
            val cor by animateColorAsState(
                targetValue = if (aberto) cores.primary else cores.outlineVariant,
                animationSpec = tween(Movimento.RAPIDO),
                label = "corDoPonto",
            )
            Box(Modifier.width(largura).height(5.dp).background(cor, CircleShape))
        }
    }
}

private fun descricaoDoSelo(name: String, badge: CourseBadge): String = when (badge) {
    is CourseBadge.Revisar -> "$name, ${badge.quantas} para revisar"
    CourseBadge.EmDia -> "$name, em dia"
    CourseBadge.Vazio -> "$name, nada agendado"
}
