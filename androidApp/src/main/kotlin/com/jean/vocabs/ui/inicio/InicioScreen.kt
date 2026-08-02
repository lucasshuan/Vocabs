package com.jean.vocabs.ui.inicio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.shared.domain.ResumoRevisao
import com.jean.vocabs.ui.idiomas.Idiomas
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.escalaAoPressionar
import com.jean.vocabs.ui.components.tempoAte
import com.jean.vocabs.ui.theme.CorDeAtalho
import com.jean.vocabs.ui.theme.CoresDeAtalho
import java.time.LocalTime

/**
 * A porta do app.
 *
 * Não tem bloco de captura próprio: o botão central da barra faz isso de
 * qualquer aba, e é o alvo mais fácil do rodapé. O critério de alcance do
 * polegar do [docs/INTERFACE.md] continua sendo obedecido — só que pela barra,
 * que é a mesma em toda tela, em vez de por um bloco repetido aqui.
 */
@Composable
fun InicioScreen(
    aoEscrever: () -> Unit,
    aoAbrirPalavras: () -> Unit,
    aoAbrirPendentes: () -> Unit,
    aoRevisar: () -> Unit,
    aoAbrirPerfil: () -> Unit,
    vm: InicioViewModel = viewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Cabecalho(
                estado = estado,
                aoAbrirPerfil = aoAbrirPerfil,
                modifier = Modifier.padding(top = 16.dp),
            )

            FilaDeIdiomas(modifier = Modifier.padding(top = 16.dp))

            estado.revisao?.let { revisao ->
                if (revisao.naFila > 0) {
                    AnimatedVisibility(
                        visible = true,
                        enter = scaleIn(tween(240)) + fadeIn(tween(240)),
                    ) {
                        CartaoRevisao(
                            revisao = revisao,
                            aoRevisar = aoRevisar,
                            modifier = Modifier.padding(top = 24.dp),
                        )
                    }
                } else {
                    // Sumir quando não há o que revisar deixaria o recurso
                    // invisível justamente no dia em que você está em dia.
                    Text(
                        text = "Nada pedindo revisão agora" +
                            (revisao.proximaEmMillis?.let { " — a próxima volta ${tempoAte(it)}." }
                                ?: "."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                }
            }

            GradeDeAtalhos(
                estado = estado,
                aoEscrever = aoEscrever,
                aoAbrirPalavras = aoAbrirPalavras,
                aoAbrirPendentes = aoAbrirPendentes,
                modifier = Modifier.padding(top = 24.dp),
            )

            // Espaço da barra, que flutua por cima desta coluna.
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

/**
 * Tudo que o app faz, numa grade só.
 *
 * O que existe e o que ainda não existe convivem aqui de propósito: a diferença
 * é dita pela cor, não por um título de seção. Um card apagado no meio dos
 * coloridos já se explica sozinho, e separar em duas listas daria a promessa o
 * mesmo peso visual do que funciona.
 */
@Composable
private fun GradeDeAtalhos(
    estado: InicioEstado,
    aoEscrever: () -> Unit,
    aoAbrirPalavras: () -> Unit,
    aoAbrirPendentes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CartaoDeAtalho(
                titulo = "Capturar",
                legenda = "áudio, foto ou texto",
                icone = Icones.CapturarCirculo,
                // Paleta própria da grade, e não o azul/laranja do app: aqueles
                // já significam ação de captura e tipo de palavra, e repeti-los
                // aqui fazia os cards parecerem eco dos botões logo abaixo.
                atalho = CoresDeAtalho.violeta,
                aoClicar = aoEscrever,
                modifier = Modifier.weight(1f),
            )
            CartaoDeAtalho(
                titulo = "Palavras",
                legenda = when (estado.totalPalavras) {
                    0 -> "nenhuma ainda"
                    1 -> "1 ficha"
                    else -> "${estado.totalPalavras} fichas"
                },
                icone = Icones.Cartas,
                atalho = CoresDeAtalho.turquesa,
                aoClicar = aoAbrirPalavras,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CartaoDeAtalho(
                titulo = "Pendentes",
                legenda = when (estado.pendentes) {
                    0 -> "nada parado"
                    1 -> "1 esperando"
                    else -> "${estado.pendentes} esperando"
                },
                icone = Icones.Inbox,
                atalho = CoresDeAtalho.framboesa,
                aoClicar = aoAbrirPendentes,
                modifier = Modifier.weight(1f),
            )
            CartaoDeAtalho(
                titulo = "Exercícios",
                legenda = "cloze, leitura, associação",
                icone = Icones.Check,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CartaoDeAtalho(
                titulo = "Rede",
                legenda = "palavras puxando palavras",
                icone = Icones.Rede,
                modifier = Modifier.weight(1f),
            )
            CartaoDeAtalho(
                titulo = "Escuta",
                legenda = "ditado e cruzadas",
                icone = Icones.Som,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Um card da grade. Sem [aoClicar] ele é uma promessa, não um botão.
 *
 * Nada de `onClick` que não faz nada: um card que responde ao toque com silêncio
 * é pior que um que visivelmente não responde. O apagamento e a palavra
 * "em breve" no lugar da legenda dizem isso antes de você tentar.
 */
@Composable
private fun CartaoDeAtalho(
    titulo: String,
    legenda: String,
    icone: ImageVector,
    modifier: Modifier = Modifier,
    atalho: CorDeAtalho? = null,
    aoClicar: (() -> Unit)? = null,
) {
    val cores = MaterialTheme.colorScheme
    val disponivel = aoClicar != null

    val fundo = atalho?.fundo ?: cores.surface.copy(alpha = 0.55f)
    val conteudo = atalho?.conteudo ?: cores.onSurfaceVariant.copy(alpha = 0.75f)
    val interacao = remember { MutableInteractionSource() }

    val corpo: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .background(conteudo.copy(alpha = 0.14f), CircleShape),
            ) {
                Icon(
                    imageVector = icone,
                    contentDescription = null,
                    tint = conteudo,
                    modifier = Modifier.size(19.dp),
                )
            }
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = conteudo,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = if (disponivel) legenda else "em breve",
                style = MaterialTheme.typography.bodySmall,
                color = conteudo.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }

    if (aoClicar == null) {
        Surface(shape = MaterialTheme.shapes.large, color = fundo, modifier = modifier) { corpo() }
    } else {
        Surface(
            onClick = aoClicar,
            shape = MaterialTheme.shapes.large,
            color = fundo,
            interactionSource = interacao,
            modifier = modifier.escalaAoPressionar(interacao),
        ) { corpo() }
    }
}

/**
 * O cabeçalho inteiro é o botão do perfil.
 *
 * A bandeira no lugar do ícone de pessoa não é enfeite: o idioma nativo é o que
 * define em que língua as fichas são escritas, ou seja, qual "projeto" está
 * aberto. Mostrá-lo aqui é a resposta mais curta para "sobre que base o app está
 * trabalhando agora", e como a troca mora na tela de progresso, a mesma bandeira
 * vira o caminho até lá.
 */
@Composable
private fun Cabecalho(
    estado: InicioEstado,
    aoAbrirPerfil: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme
    val interacao = remember { MutableInteractionSource() }
    val nativo = Idiomas.nativoAtual

    Surface(
        onClick = aoAbrirPerfil,
        shape = MaterialTheme.shapes.large,
        color = Color.Transparent,
        interactionSource = interacao,
        modifier = modifier
            .fillMaxWidth()
            .escalaAoPressionar(interacao),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = saudacao(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = cores.onBackground,
                )
                Text(
                    text = resumoDoDia(estado),
                    style = MaterialTheme.typography.bodyMedium,
                    color = cores.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .background(cores.secondaryContainer, CircleShape),
            ) {
                Text(text = nativo.bandeira, fontSize = 22.sp)
            }
            Icon(
                imageVector = Icones.Avancar,
                contentDescription = "Seu progresso",
                tint = cores.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 2.dp)
                    .size(20.dp),
            )
        }
    }
}

/**
 * Os idiomas que se está aprendendo.
 *
 * Rola na horizontal porque a lista é feita para crescer; hoje tem um item só e
 * um "+" que ainda não faz nada. O "+" fica desde já para o lugar da ação existir
 * antes da ação — quando um segundo idioma entrar, nada se move de posição.
 */
@Composable
private fun FilaDeIdiomas(modifier: Modifier = Modifier) {
    val cores = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Idiomas.alvos.forEach { idioma ->
            val selecionado = idioma.codigo == Idiomas.alvoAtual.codigo
            Surface(
                shape = RoundedCornerShape(50),
                color = if (selecionado) cores.inverseSurface else cores.surface,
                border = if (selecionado) null else BorderStroke(1.dp, cores.outline),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(text = idioma.bandeira, fontSize = 16.sp)
                    Text(
                        text = idioma.nome,
                        style = MaterialTheme.typography.labelLarge
                            .copy(fontWeight = FontWeight.SemiBold),
                        color = if (selecionado) cores.inverseOnSurface else cores.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }

        Surface(
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(1.dp, cores.outline),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icones.Mais,
                    contentDescription = "Adicionar idioma",
                    tint = cores.onSurfaceVariant,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

private fun saudacao(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Bom dia"
    in 12..17 -> "Boa tarde"
    else -> "Boa noite"
}

private fun resumoDoDia(estado: InicioEstado): String {
    if (estado.totalPalavras == 0) return "Nenhuma palavra ainda — capture a primeira."

    val palavras = if (estado.totalPalavras == 1) "1 palavra" else "${estado.totalPalavras} palavras"
    val sequencia = estado.revisao?.diasSeguidos ?: 0
    return if (sequencia >= 2) "$palavras  ·  $sequencia dias seguidos" else palavras
}

@Composable
private fun CartaoRevisao(
    revisao: ResumoRevisao,
    aoRevisar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme
    val interacao = remember { MutableInteractionSource() }

    Surface(
        onClick = aoRevisar,
        shape = MaterialTheme.shapes.large,
        color = cores.primary,
        interactionSource = interacao,
        modifier = modifier
            .fillMaxWidth()
            .escalaAoPressionar(interacao),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .background(cores.onPrimary.copy(alpha = 0.18f), CircleShape),
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
                    color = cores.onPrimary,
                )
                val legenda = when {
                    revisao.revisouHoje -> "Você já revisou hoje."
                    revisao.diasSeguidos >= 2 -> "${revisao.diasSeguidos} dias seguidos — não perca."
                    else -> "Leva uns 4 minutos."
                }
                Text(
                    text = legenda,
                    style = MaterialTheme.typography.bodySmall,
                    color = cores.onPrimary.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                text = "→",
                style = MaterialTheme.typography.titleMedium,
                color = cores.onPrimary,
            )
        }
    }
}
