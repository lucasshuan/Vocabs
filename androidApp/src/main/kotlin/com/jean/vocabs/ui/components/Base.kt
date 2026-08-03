package com.jean.vocabs.ui.components

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
import androidx.compose.runtime.Composable
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
fun contornoDeCartao(): BorderStroke? =
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
fun CaixaTracejada(
    modifier: Modifier = Modifier,
    raio: Dp = 18.dp,
    cor: Color = MaterialTheme.colorScheme.outline,
    recheio: PaddingValues = PaddingValues(horizontal = 15.dp, vertical = 12.dp),
    aoClicar: (() -> Unit)? = null,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    val forma = RoundedCornerShape(raio)
    val base = modifier
        .clip(forma)
        .then(if (aoClicar != null) Modifier.clickable(onClick = aoClicar) else Modifier)
        .contornoTracejado(cor, raio)
        .padding(recheio)
    Column(modifier = base, content = conteudo)
}

/** A superfície padrão de conteúdo: surface, contorno conforme o tema, cantos grandes. */
@Composable
fun CartaoDaTela(
    modifier: Modifier = Modifier,
    forma: Shape = MaterialTheme.shapes.large,
    cor: Color = MaterialTheme.colorScheme.surface,
    recheio: PaddingValues = PaddingValues(16.dp),
    aoClicar: (() -> Unit)? = null,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    val contorno = contornoDeCartao()
    val interno: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(recheio), content = conteudo)
    }
    if (aoClicar == null) {
        Surface(shape = forma, color = cor, border = contorno, modifier = modifier, content = interno)
    } else {
        Surface(onClick = aoClicar, shape = forma, color = cor, border = contorno, modifier = modifier, content = interno)
    }
}

/**
 * O rótulo de apoio acima de cada bloco ("Seu contexto", "Capturadas hoje").
 *
 * Caixa normal e 12 sp: o handoff abandonou o versalete espaçado do desenho
 * antigo, que competia em peso com o próprio conteúdo que anunciava.
 */
@Composable
fun RotuloDeSecao(texto: String, modifier: Modifier = Modifier) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** O ladrilho de número grande: Início, Perfil e o resumo da revisão usam o mesmo. */
@Composable
fun CartaoMetrica(
    valor: String,
    rotulo: String,
    modifier: Modifier = Modifier,
    destaque: Boolean = false,
    aoClicar: (() -> Unit)? = null,
) {
    CartaoDaTela(
        modifier = modifier,
        forma = MaterialTheme.shapes.medium,
        recheio = PaddingValues(horizontal = 14.dp, vertical = 13.dp),
        aoClicar = aoClicar,
    ) {
        Text(
            text = valor,
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
fun LinhaDeLista(
    modifier: Modifier = Modifier,
    aoClicar: (() -> Unit)? = null,
    inicio: (@Composable () -> Unit)? = null,
    fim: (@Composable () -> Unit)? = null,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    CartaoDaTela(
        modifier = modifier.fillMaxWidth(),
        forma = MaterialTheme.shapes.medium,
        recheio = PaddingValues(horizontal = 15.dp, vertical = 14.dp),
        aoClicar = aoClicar,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            inicio?.let {
                it()
                Spacer(Modifier.width(13.dp))
            }
            Column(Modifier.weight(1f), content = conteudo)
            fim?.let {
                Spacer(Modifier.width(10.dp))
                it()
            }
        }
    }
}

/** O caso comum de [LinhaDeLista]: título e uma linha de apoio. */
@Composable
fun LinhaDeLista(
    titulo: String,
    modifier: Modifier = Modifier,
    detalhe: String? = null,
    aoClicar: (() -> Unit)? = null,
    inicio: (@Composable () -> Unit)? = null,
    fim: (@Composable () -> Unit)? = null,
) {
    LinhaDeLista(modifier = modifier, aoClicar = aoClicar, inicio = inicio, fim = fim) {
        Text(titulo, style = MaterialTheme.typography.titleSmall)
        detalhe?.let {
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
fun DiscoDeIcone(
    icone: ImageVector,
    descricao: String?,
    cor: Color,
    fundo: Color,
    tamanho: Dp = 38.dp,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(tamanho).background(fundo, CircleShape),
    ) {
        Icon(icone, descricao, tint = cor, modifier = Modifier.size(tamanho * 0.52f))
    }
}

/** A chevron de "isto abre outra tela", no cinza-lilás de informação de apoio. */
@Composable
fun ChevronDeLinha() {
    Icon(
        imageVector = Icones.Avancar,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp),
    )
}

/** A ação principal da tela: ameixa, largura inteira, cantos de 18. */
@Composable
fun BotaoPrincipal(
    texto: String,
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    conteudoInicial: (@Composable RowScope.() -> Unit)? = null,
) {
    Button(
        onClick = aoClicar,
        enabled = habilitado,
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        modifier = modifier.fillMaxWidth().height(56.dp),
    ) {
        conteudoInicial?.invoke(this)
        Text(texto, style = MaterialTheme.typography.titleMedium)
    }
}

/** A pílula de filtro de Palavras e a aba de formato da Captura são a mesma coisa. */
@Composable
fun PilulaSelecionavel(
    rotulo: String,
    selecionada: Boolean,
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier,
    forma: Shape = CircleShape,
) {
    val cores = MaterialTheme.colorScheme
    Surface(
        onClick = aoClicar,
        shape = forma,
        color = if (selecionada) cores.primary else cores.surface,
        contentColor = if (selecionada) cores.onPrimary else cores.onSurfaceVariant,
        border = if (selecionada) null else contornoDeCartao(),
        modifier = modifier,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Text(
                text = rotulo,
                style = MaterialTheme.typography.labelMedium,
                color = if (selecionada) cores.onPrimary else cores.onSurfaceVariant,
            )
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
fun Pilula(
    texto: String,
    modifier: Modifier = Modifier,
    destaque: Boolean = false,
    aoClicar: (() -> Unit)? = null,
) {
    val cores = MaterialTheme.colorScheme
    val fundo = if (destaque) cores.secondaryContainer else cores.surface
    val cor = if (destaque) cores.primary else cores.onSurface
    val contorno = if (destaque) null else contornoDeCartao()
    val conteudo: @Composable () -> Unit = {
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium,
            color = cor,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
        )
    }
    if (aoClicar == null) {
        Surface(shape = CircleShape, color = fundo, border = contorno, modifier = modifier, content = conteudo)
    } else {
        Surface(onClick = aoClicar, shape = CircleShape, color = fundo, border = contorno, modifier = modifier, content = conteudo)
    }
}

/** O vazio de Palavras, Pendentes e Revisão: disco de menta, título e uma linha. */
@Composable
fun EstadoVazio(
    icone: ImageVector,
    titulo: String,
    detalhe: String,
    modifier: Modifier = Modifier,
    acao: (@Composable () -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 60.dp),
    ) {
        DiscoDeIcone(
            icone = icone,
            descricao = null,
            cor = MaterialTheme.colorScheme.tertiary,
            fundo = MaterialTheme.colorScheme.tertiaryContainer,
            tamanho = 72.dp,
        )
        Text(titulo, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
        Text(
            text = detalhe,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        acao?.let {
            Box(Modifier.padding(top = 20.dp)) { it() }
        }
    }
}

/** O botão redondo de cabeçalho (voltar, fechar, descartar). */
@Composable
fun BotaoCircular(
    icone: ImageVector,
    descricao: String,
    aoClicar: () -> Unit,
    cor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Surface(
        onClick = aoClicar,
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = cor,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
            Icon(icone, descricao, tint = cor, modifier = Modifier.size(24.dp))
        }
    }
}

/** Botão de texto discreto ("Não lembro"), centralizado sob a ação principal. */
@Composable
fun AcaoSecundaria(texto: String, aoClicar: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = aoClicar,
        color = Color.Transparent,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.height(48.dp)) {
            Text(
                text = texto,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
