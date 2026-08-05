package com.jean.vocabs.ui.guardado

import com.jean.vocabs.ui.displayName
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
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
import com.jean.vocabs.shared.domain.Entry
import com.jean.vocabs.shared.domain.EntryStatus
import com.jean.vocabs.ui.components.BandeiraCircular
import com.jean.vocabs.ui.components.BotaoPrincipal
import com.jean.vocabs.ui.components.CartaoDaTela
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.Movimento
import com.jean.vocabs.ui.components.entradaSuave
import com.jean.vocabs.ui.components.respirando
import com.jean.vocabs.ui.languages.idiomaDe
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

    LaunchedEffect(estado.trabalhando, estado.entries.isEmpty(), interagiu) {
        if (estado.entries.isEmpty() || estado.trabalhando || interagiu) return@LaunchedEffect
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
            if (estado.target.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    BandeiraCircular(idiomaDe(estado.target), tamanho = 18.dp)
                    Text(
                        text = "no seu ${idiomaDe(estado.target).displayName.lowercase()} · ${estado.totalDoCurso} ${if (estado.totalDoCurso == 1) "card" else "cards"} agora",
                        style = MaterialTheme.typography.bodySmall,
                        color = cores.onSurfaceVariant,
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            estado.entries.forEachIndexed { indice, entry ->
                LinhaGuardada(entry, Modifier.entradaSuave(indice))
            }
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
                text = "Capturar outra",
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
        animationSpec = Movimento.molaElastica(),
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

/**
 * Uma linha do que acabou de entrar.
 *
 * É a única tela do app em que o conteúdo muda **sozinho**, com a pessoa olhando:
 * a IA volta e "montando o sentido" vira a tradução. Essa troca é a promessa da
 * tela sendo cumprida, e por isso as duas metades da linha — o subtexto e o selo
 * da direita — cruzam em vez de trocar de um quadro para o outro.
 */
@Composable
private fun LinhaGuardada(entry: Entry, modifier: Modifier = Modifier) {
    val cores = MaterialTheme.colorScheme
    val pronta = entry.status == EntryStatus.READY
    val comErro = entry.status == EntryStatus.ERROR

    CartaoDaTela(
        forma = MaterialTheme.shapes.medium,
        recheio = PaddingValues(horizontal = 15.dp, vertical = 13.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(entry.title, style = MaterialTheme.typography.titleMedium)
                AnimatedContent(
                    targetState = entry.status,
                    transitionSpec = { fadeIn(tween(Movimento.PADRAO)) togetherWith fadeOut(tween(Movimento.RAPIDO)) },
                    label = "estadoDaLinha",
                ) { status ->
                    when (status) {
                        EntryStatus.READY -> Text(
                            text = entry.card?.translation.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = cores.onSurfaceVariant,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                        EntryStatus.ERROR -> Text(
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
                                modifier = Modifier.respirando(ativo = true),
                            )
                        }
                    }
                }
            }
            // O selo de pronta entra com mola: ele é o desfecho de uma espera, e
            // é a única coisa da linha que a pessoa pode estar esperando ver.
            AnimatedVisibility(
                visible = pronta,
                enter = scaleIn(Movimento.molaElastica()) + fadeIn(tween(Movimento.PADRAO)),
                exit = fadeOut(tween(Movimento.RAPIDO)),
            ) {
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
            }
            if (!pronta && !comErro) {
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
private fun AcaoDeSaida(text: String, modifier: Modifier = Modifier, aoClicar: () -> Unit) {
    Surface(
        onClick = aoClicar,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = com.jean.vocabs.ui.components.contornoDeCartao(),
        modifier = modifier,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.height(48.dp)) {
            Text(text, style = MaterialTheme.typography.titleSmall)
        }
    }
}

/** Tempo de leitura de duas linhas curtas, e nada além disso. */
private const val FECHA_SOZINHO_EM_MS = 3_500L
