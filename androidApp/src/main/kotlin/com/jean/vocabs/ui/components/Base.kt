package com.jean.vocabs.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jean.vocabs.ui.theme.LocalTemaEscuro

/**
 * Os blocos que o handoff repete em todas as telas.
 *
 * Cada um existe porque a mesma forma aparecia em três ou quatro lugares e ia
 * divergindo em cada um: o cartão que ganhava contorno no escuro numa tela e não
 * na outra, o número grande que era centralizado aqui e alinhado à esquerda ali.
 * Manter a regra num lugar só é o que faz as telas continuarem parecidas.
 */

/**
 * O contorno do cartão — nulo no escuro, de propósito.
 *
 * No claro a superfície é branca sobre um fundo quase branco e precisa da linha
 * para existir; no escuro a superfície já é mais clara que o fundo e a linha só
 * sujaria a borda.
 */
@Composable
fun cardOutline(): BorderStroke? =
    if (LocalTemaEscuro.current) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)

/**
 * O contorno tracejado de "aqui cabe mais alguma coisa".
 *
 * O handoff o usa em quatro lugares — o `+` da faixa, "Adicionar idioma", o
 * convite de captura do dia vazio e o rodapé da fila — e sempre com o mesmo
 * sentido: a caixa existe, o conteúdo é que ainda não. Um contorno sólido diria
 * "isto é um cartão"; o tracejado diz "isto é um espaço".
 *
 * Desenhado à mão porque `BorderStroke` não aceita `PathEffect`.
 */
fun Modifier.contornoTracejado(
    cor: Color,
    raio: Dp = 18.dp,
    espessura: Dp = 1.dp,
): Modifier = drawBehind {
    val traco = espessura.toPx()
    drawRoundRect(
        color = cor,
        topLeft = Offset(traco / 2f, traco / 2f),
        size = Size(size.width - traco, size.height - traco),
        cornerRadius = CornerRadius(raio.toPx()),
        style = Stroke(
            width = traco,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(traco * 5, traco * 4)),
        ),
    )
}

/** A caixa tracejada pronta: fundo do tema, contorno pontilhado, conteúdo livre. */
@Composable
fun DashedBox(
    modifier: Modifier = Modifier,
    raio: Dp = 18.dp,
    cor: Color = MaterialTheme.colorScheme.outline,
    recheio: PaddingValues = PaddingValues(horizontal = 15.dp, vertical = 12.dp),
    aoClicar: (() -> Unit)? = null,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    val forma = RoundedCornerShape(raio)
    val toque = rememberHaptics()
    val base = modifier
        .then(if (aoClicar != null) Modifier.encolheAoTocar(toque) else Modifier)
        .clip(forma)
        .then(
            if (aoClicar != null) Modifier.clickable(interactionSource = toque, indication = ripple(), onClick = aoClicar)
            else Modifier,
        )
        .contornoTracejado(cor, raio)
        .padding(recheio)
    Column(modifier = base, content = conteudo)
}

/**
 * A superfície padrão de conteúdo: surface, contorno conforme o tema, cantos grandes.
 *
 * Quando é clicável, cede sob o dedo. O encolhimento mora aqui e não em cada
 * chamada porque é justamente a espécie de detalhe que só existe se for de graça:
 * há dezenas de cartões clicáveis no app, e nenhuma tela lembraria de pedir por
 * ele um a um.
 */
@Composable
fun ScreenCard(
    modifier: Modifier = Modifier,
    forma: Shape = MaterialTheme.shapes.large,
    cor: Color = MaterialTheme.colorScheme.surface,
    recheio: PaddingValues = PaddingValues(16.dp),
    aoClicar: (() -> Unit)? = null,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    val contorno = cardOutline()
    val interno: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(recheio), content = conteudo)
    }
    if (aoClicar == null) {
        Surface(shape = forma, color = cor, border = contorno, modifier = modifier, content = interno)
    } else {
        val toque = rememberHaptics()
        Surface(
            onClick = aoClicar,
            shape = forma,
            color = cor,
            border = contorno,
            interactionSource = toque,
            modifier = modifier.encolheAoTocar(toque),
            content = interno,
        )
    }
}

/**
 * O rótulo de apoio acima de cada bloco ("Seu contexto", "Capturadas hoje").
 *
 * Caixa normal e 12 sp: o handoff abandonou o versalete espaçado do desenho
 * antigo, que competia em peso com o próprio conteúdo que anunciava.
 */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** O ladrilho de número grande: Início, Perfil e o resumo da revisão usam o mesmo. */
@Composable
fun MetricCard(
    value: String,
    rotulo: String,
    modifier: Modifier = Modifier,
    destaque: Boolean = false,
    aoClicar: (() -> Unit)? = null,
) {
    ScreenCard(
        modifier = modifier,
        forma = MaterialTheme.shapes.medium,
        recheio = PaddingValues(horizontal = 14.dp, vertical = 13.dp),
        aoClicar = aoClicar,
    ) {
        // Sem transição no número, de propósito. O ladrilho recebe uma `String`
        // já formatada — "42%", "4,2", "—" — e quem quer ver o valor subir passa
        // o resultado de `contagemAnimada`. Um `AnimatedContent` aqui dispararia
        // uma transição por quadro justamente nesse caso, que é o único em que o
        // número de fato se move.
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = if (destaque) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = rotulo,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** A linha de lista com ícone/valor nas pontas — Pendentes, Perfil e o aviso do Início. */
@Composable
fun ListRow(
    modifier: Modifier = Modifier,
    aoClicar: (() -> Unit)? = null,
    start: (@Composable () -> Unit)? = null,
    end: (@Composable () -> Unit)? = null,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    ScreenCard(
        modifier = modifier.fillMaxWidth(),
        forma = MaterialTheme.shapes.medium,
        recheio = PaddingValues(horizontal = 15.dp, vertical = 14.dp),
        aoClicar = aoClicar,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            start?.let {
                it()
                Spacer(Modifier.width(13.dp))
            }
            Column(Modifier.weight(1f), content = conteudo)
            end?.let {
                Spacer(Modifier.width(10.dp))
                it()
            }
        }
    }
}

/** O caso comum de [LinhaDeLista]: título e uma linha de apoio. */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    aoClicar: (() -> Unit)? = null,
    start: (@Composable () -> Unit)? = null,
    end: (@Composable () -> Unit)? = null,
) {
    ListRow(modifier = modifier, aoClicar = aoClicar, start = start, end = end) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        detail?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** O disco colorido que abre as linhas de Pendentes. */
@Composable
fun IconDisc(
    icon: ImageVector,
    descricao: String?,
    cor: Color,
    fundo: Color,
    tamanho: Dp = 38.dp,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(tamanho).background(fundo, CircleShape),
    ) {
        Icon(icon, descricao, tint = cor, modifier = Modifier.size(tamanho * 0.52f))
    }
}

/** A chevron de "isto abre outra tela", no cinza-lilás de informação de apoio. */
@Composable
fun RowChevron() {
    Icon(
        imageVector = AppIcons.Avancar,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp),
    )
}

/** A ação principal da tela: ameixa, largura inteira, cantos de 18. */
@Composable
fun PrimaryButton(
    text: String,
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    conteudoInicial: (@Composable RowScope.() -> Unit)? = null,
) {
    val toque = rememberHaptics()
    // Menos que os cartões (0,97): este botão ocupa a largura da tela, e a mesma
    // proporção num alvo desse tamanho vira um solavanco em vez de um toque.
    Button(
        onClick = aoClicar,
        enabled = habilitado,
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        interactionSource = toque,
        modifier = modifier.encolheAoTocar(toque, minimo = 0.985f).fillMaxWidth().height(56.dp),
    ) {
        conteudoInicial?.invoke(this)
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

/** A pílula de filtro de Palavras e a aba de formato da Capture são a mesma coisa. */
@Composable
fun SelectablePill(
    rotulo: String,
    selecionada: Boolean,
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier,
    forma: Shape = CircleShape,
) {
    val cores = MaterialTheme.colorScheme
    val toque = rememberHaptics()
    // As cores transitam em vez de trocar de um quadro para o outro: numa fileira
    // de filtros, o corte seco faz duas pílulas piscarem ao mesmo tempo e nenhuma
    // das duas diz de onde a seleção veio.
    val fundo by animateColorAsState(
        targetValue = if (selecionada) cores.primary else cores.surface,
        animationSpec = tween(Motion.FAST),
        label = "fundoDaPilula",
    )
    val tinta by animateColorAsState(
        targetValue = if (selecionada) cores.onPrimary else cores.onSurfaceVariant,
        animationSpec = tween(Motion.FAST),
        label = "tintaDaPilula",
    )
    Surface(
        onClick = aoClicar,
        shape = forma,
        color = fundo,
        contentColor = tinta,
        border = if (selecionada) null else cardOutline(),
        interactionSource = toque,
        modifier = modifier.encolheAoTocar(toque, minimo = 0.94f),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Text(text = rotulo, style = MaterialTheme.typography.labelMedium, color = tinta)
        }
    }
}

/**
 * Pílula de conteúdo: termo relacionado, "ver mais", "tentar de novo".
 *
 * Com [destaque] ela vira ameixa sobre lilás — é o que separa "isto é mais uma
 * palavra" de "isto faz alguma coisa".
 */
@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    destaque: Boolean = false,
    aoClicar: (() -> Unit)? = null,
) {
    val cores = MaterialTheme.colorScheme
    val fundo = if (destaque) cores.secondaryContainer else cores.surface
    val cor = if (destaque) cores.primary else cores.onSurface
    val contorno = if (destaque) null else cardOutline()
    val conteudo: @Composable () -> Unit = {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = cor,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
        )
    }
    if (aoClicar == null) {
        Surface(shape = CircleShape, color = fundo, border = contorno, modifier = modifier, content = conteudo)
    } else {
        val toque = rememberHaptics()
        Surface(
            onClick = aoClicar,
            shape = CircleShape,
            color = fundo,
            border = contorno,
            interactionSource = toque,
            modifier = modifier.encolheAoTocar(toque, minimo = 0.94f),
            content = conteudo,
        )
    }
}

/**
 * O vazio de Palavras, Pendentes e Revisão: disco de menta, título e uma linha.
 *
 * O disco entra com mola e o texto sobe atrás dele. Uma tela vazia é o único
 * lugar do app em que não há conteúdo nenhum para segurar o olho, e chegar
 * montada faz "Memória em dia" parecer uma falha de carregamento em vez do
 * desfecho bom que ela é.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
    acao: (@Composable () -> Unit)? = null,
) {
    var chegou by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { chegou = true }
    val escala by animateFloatAsState(
        targetValue = if (chegou) 1f else 0.6f,
        animationSpec = Motion.molaElastica(),
        label = "escalaDoVazio",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 60.dp),
    ) {
        Box(Modifier.graphicsLayer { scaleX = escala; scaleY = escala; alpha = if (chegou) 1f else 0f }) {
            IconDisc(
                icon = icon,
                descricao = null,
                cor = MaterialTheme.colorScheme.tertiary,
                fundo = MaterialTheme.colorScheme.tertiaryContainer,
                tamanho = 72.dp,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.entradaSuave(index = 1).padding(top = 16.dp),
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.entradaSuave(index = 2).padding(top = 4.dp),
        )
        acao?.let {
            Box(Modifier.entradaSuave(index = 3).padding(top = 20.dp)) { it() }
        }
    }
}

/** O botão redondo de cabeçalho (voltar, fechar, descartar). */
@Composable
fun CircularButton(
    icon: ImageVector,
    descricao: String,
    aoClicar: () -> Unit,
    cor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val toque = rememberHaptics()
    Surface(
        onClick = aoClicar,
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = cor,
        interactionSource = toque,
        modifier = Modifier.encolheAoTocar(toque, minimo = 0.88f),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
            Icon(icon, descricao, tint = cor, modifier = Modifier.size(24.dp))
        }
    }
}

/** Botão de texto discreto ("Não lembro"), centralizado sob a ação principal. */
@Composable
fun SecondaryAction(text: String, aoClicar: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = aoClicar,
        color = Color.Transparent,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.height(48.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
