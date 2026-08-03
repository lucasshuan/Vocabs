package com.jean.vocabs.ui.guardado

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.shared.domain.StatusEntrada
import com.jean.vocabs.ui.components.BandeiraCircular
import com.jean.vocabs.ui.components.BotaoPrincipal
import com.jean.vocabs.ui.components.CartaoDaTela
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.idiomas.idiomaDe
import kotlinx.coroutines.delay

/**
 * Tela 11 do handoff — "Guardado".
 *
 * Confirma em menta, mostra o que entrou e em qual idioma, e deixa a IA
 * trabalhando ao fundo. A ação em destaque é **capturar de novo**, e não sair:
 * quem acabou de guardar duas coisas de um mesmo trecho quase sempre tem uma
 * terceira, e o custo da próxima captura é o assunto do app inteiro.
 *
 * Ela se fecha sozinha quando não há mais nada acontecendo. Enquanto uma ficha
 * estiver sendo montada, fica: seria estranho prometer mostrar o trabalho e
 * sumir com ele no meio.
 */
@Composable
fun GuardadoScreen(
    ids: List<Long>,
    aoFechar: () -> Unit,
    aoCapturarOutra: () -> Unit,
    aoVerFichas: () -> Unit,
    aoRevisar: () -> Unit,
    vm: GuardadoViewModel = viewModel(),
) {
    LaunchedEffect(ids) { vm.acompanhar(ids) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val cores = MaterialTheme.colorScheme
    var interagiu by remember { mutableStateOf(false) }

    LaunchedEffect(estado.trabalhando, estado.entradas.isEmpty(), interagiu) {
        if (estado.entradas.isEmpty() || estado.trabalhando || interagiu) return@LaunchedEffect
        delay(FECHA_SOZINHO_EM_MS)
        aoFechar()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(13.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 34.dp),
        ) {
            SeloDeConfirmacao()
            // A contagem vem da rota e não do banco: ela já é conhecida no
            // primeiro frame, e um "0 guardadas" piscando antes da consulta
            // desmentiria justo a tela que existe para confirmar.
            Text(
                text = "${ids.size} ${if (ids.size == 1) "guardada" else "guardadas"}",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            if (estado.alvo.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    BandeiraCircular(idiomaDe(estado.alvo), tamanho = 18.dp)
                    Text(
                        text = "no seu ${idiomaDe(estado.alvo).nome.lowercase()} · ${estado.totalDoCurso} ${if (estado.totalDoCurso == 1) "ficha" else "fichas"} agora",
                        style = MaterialTheme.typography.bodySmall,
                        color = cores.onSurfaceVariant,
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            estado.entradas.forEach { LinhaGuardada(it) }
        }

        Surface(shape = MaterialTheme.shapes.medium, color = cores.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (ids.size > 1) "Todas entram na revisão de hoje. Se a IA errar o sentido, você corrige na ficha — nada trava aqui."
                else "Ela entra na revisão de hoje. Se a IA errar o sentido, você corrige na ficha — nada trava aqui.",
                style = MaterialTheme.typography.bodySmall,
                color = cores.onSurfaceVariant,
                modifier = Modifier.padding(14.dp),
            )
        }

        Spacer(Modifier.weight(1f))

        Column(verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.padding(bottom = 8.dp)) {
            BotaoPrincipal(
                texto = "Capturar outra",
                aoClicar = { interagiu = true; aoCapturarOutra() },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                AcaoDeSaida("Ver as fichas", Modifier.weight(1f)) { interagiu = true; aoVerFichas() }
                AcaoDeSaida("Revisar agora", Modifier.weight(1f)) { interagiu = true; aoRevisar() }
            }
            Text(
                text = if (estado.trabalhando) "A IA continua montando ao fundo." else "Fecha sozinho e volta para onde você estava.",
                style = MaterialTheme.typography.bodySmall,
                color = cores.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.navigationBarsPadding().height(8.dp))
    }
}

/** O tique entra com mola: é a única comemoração do app, e ela dura 300 ms. */
@Composable
private fun SeloDeConfirmacao() {
    val cores = MaterialTheme.colorScheme
    var apareceu by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { apareceu = true }
    val escala by animateFloatAsState(
        targetValue = if (apareceu) 1f else 0.5f,
        animationSpec = spring(dampingRatio = 0.52f, stiffness = Spring.StiffnessMediumLow),
        label = "escalaDoSelo",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.scale(escala).size(96.dp).background(cores.tertiaryContainer.copy(alpha = 0.45f), CircleShape),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(76.dp).background(cores.tertiaryContainer, CircleShape),
        ) {
            Icon(Icones.Check, null, tint = cores.tertiary, modifier = Modifier.size(34.dp))
        }
    }
}

@Composable
private fun LinhaGuardada(entrada: Entrada) {
    val cores = MaterialTheme.colorScheme
    val pronta = entrada.status == StatusEntrada.PRONTA
    val comErro = entrada.status == StatusEntrada.ERRO

    CartaoDaTela(
        forma = MaterialTheme.shapes.medium,
        recheio = PaddingValues(horizontal = 15.dp, vertical = 13.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(entrada.titulo, style = MaterialTheme.typography.titleMedium)
                when {
                    pronta -> Text(
                        text = entrada.ficha?.traducao.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = cores.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                    comErro -> Text(
                        text = "não deu certo — dá para tentar de novo em Pendentes",
                        style = MaterialTheme.typography.bodySmall,
                        color = cores.error,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                    else -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        modifier = Modifier.padding(top = 5.dp),
                    ) {
                        BarraIndeterminada()
                        Text(
                            text = "montando o sentido",
                            style = MaterialTheme.typography.bodySmall,
                            color = cores.onSurfaceVariant,
                        )
                    }
                }
            }
            if (pronta) {
                Surface(shape = CircleShape, color = cores.tertiaryContainer) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Icon(Icones.Check, null, tint = cores.tertiary, modifier = Modifier.size(11.dp))
                        Text("ficha pronta", style = MaterialTheme.typography.labelSmall, color = cores.onTertiaryContainer)
                    }
                }
            } else if (!comErro) {
                Text("IA", style = MaterialTheme.typography.labelSmall, color = cores.outline)
            }
        }
    }
}

/**
 * A barra que anda sem saber quanto falta.
 *
 * A geração não tem progresso mensurável — ela é uma requisição que volta ou
 * não. Uma barra que fingisse porcentagem estaria inventando; esta só diz que
 * alguma coisa está acontecendo.
 */
@Composable
private fun BarraIndeterminada() {
    val cores = MaterialTheme.colorScheme
    var cheia by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { cheia = true }
    val fracao by animateFloatAsState(
        targetValue = if (cheia) 0.72f else 0.12f,
        animationSpec = tween(2_400),
        label = "fracaoDaGeracao",
    )
    Box(Modifier.width(54.dp).height(5.dp).background(cores.outlineVariant, CircleShape)) {
        Box(Modifier.fillMaxWidth(fracao).height(5.dp).background(cores.primary, CircleShape))
    }
}

@Composable
private fun AcaoDeSaida(texto: String, modifier: Modifier = Modifier, aoClicar: () -> Unit) {
    Surface(
        onClick = aoClicar,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = com.jean.vocabs.ui.components.contornoDeCartao(),
        modifier = modifier,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.height(48.dp)) {
            Text(texto, style = MaterialTheme.typography.titleSmall)
        }
    }
}

/** Tempo de leitura de duas linhas curtas, e nada além disso. */
private const val FECHA_SOZINHO_EM_MS = 3_500L
