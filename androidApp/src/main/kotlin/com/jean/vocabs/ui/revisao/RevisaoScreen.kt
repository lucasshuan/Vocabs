package com.jean.vocabs.ui.revisao

import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.RotuloSecao
import com.jean.vocabs.ui.components.TipoBadge
import com.jean.vocabs.ui.components.escalaAoPressionar
import java.util.Locale

@Composable
fun RevisaoScreen(
    aoVoltar: () -> Unit,
    vm: RevisaoViewModel = viewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Cabecalho(estado = estado, aoVoltar = aoVoltar)

        AnimatedContent(
            targetState = estado,
            transitionSpec = {
                (fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 14 })
                    .togetherWith(fadeOut(tween(120)))
            },
            contentKey = { atual ->
                when (atual) {
                    is RevisaoEstado.Cartao -> atual.entrada.id to atual.revelado
                    else -> atual::class
                }
            },
            label = "corpoRevisao",
            modifier = Modifier.fillMaxSize(),
        ) { atual ->
            when (atual) {
                RevisaoEstado.Carregando -> Box(Modifier.fillMaxSize())
                RevisaoEstado.Vazia -> NadaParaRevisar(aoVoltar)
                is RevisaoEstado.Cartao -> Cartao(
                    estado = atual,
                    aoRevelar = vm::revelar,
                    aoResponder = vm::responder,
                )
                is RevisaoEstado.Resumo -> Resumo(
                    estado = atual,
                    aoMaisUmaRodada = vm::novaRodada,
                    aoVoltar = aoVoltar,
                )
            }
        }
    }
}

@Composable
private fun Cabecalho(estado: RevisaoEstado, aoVoltar: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        // Sair no meio não pede confirmação: cada resposta já foi gravada quando
        // você tocou no botão, então não há nada a perder.
        Surface(
            onClick = aoVoltar,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = Icones.Voltar,
                    contentDescription = "Sair da revisão",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        if (estado is RevisaoEstado.Cartao) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
            ) {
                Text(
                    text = "${estado.posicao} de ${estado.total}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { estado.posicao.toFloat() / estado.total.coerceAtLeast(1) },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .height(4.dp),
                )
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun Cartao(
    estado: RevisaoEstado.Cartao,
    aoRevelar: () -> Unit,
    aoResponder: (Boolean) -> Unit,
) {
    val entrada = estado.entrada

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            if (estado.revelado) {
                Verso(entrada)
            } else {
                Frente(entrada, aoRevelar)
            }
        }

        Box(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(bottom = 20.dp, top = 12.dp),
        ) {
            if (estado.revelado) {
                BotoesDeResposta(aoResponder)
            } else {
                Button(
                    onClick = aoRevelar,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Text(
                        text = "Mostrar resposta",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun Frente(entrada: Entrada, aoRevelar: () -> Unit) {
    val interacao = remember { MutableInteractionSource() }

    Surface(
        onClick = aoRevelar,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        interactionSource = interacao,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .escalaAoPressionar(interacao),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
        ) {
            entrada.ficha?.let { TipoBadge(it.tipo) }

            Text(
                text = entrada.titulo,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp),
            )

            entrada.ficha?.ipa?.takeIf { it.isNotBlank() }?.let { ipa ->
                Text(
                    text = "/$ipa/",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            // O trecho com o alvo apagado transforma todo flashcard num cloze
            // contra o seu próprio contexto — sem IA, sem conteúdo novo. É o que o
            // documento de produto pede para a "trava de leitura": o valioso não é
            // a palavra isolada, é a frase inteira de volta.
            trechoComLacuna(entrada)?.let { comLacuna ->
                Text(
                    text = comLacuna,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontStyle = FontStyle.Italic,
                        lineHeight = 26.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }

            Text(
                text = "Você lembra o que significa?",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
}

/**
 * O trecho com o alvo trocado por uma lacuna, ou nulo quando isso não dá.
 *
 * Muitas vezes o alvo não aparece literal na frase: você capturou `running` e o
 * trecho tem `ran`. Nesse caso mostrar o trecho na frente entregaria a resposta,
 * então é melhor não mostrar nada e degradar para um flashcard puro.
 *
 * A lacuna tem largura fixa de propósito — um buraco do tamanho da palavra seria
 * uma pista de graça.
 */
private fun trechoComLacuna(entrada: Entrada): String? {
    val trecho = entrada.trecho?.takeIf { it.isNotBlank() } ?: return null
    val alvo = entrada.alvo?.takeIf { it.isNotBlank() } ?: return null
    if (!trecho.contains(alvo, ignoreCase = true)) return null
    return "“${trecho.replace(alvo, "_____", ignoreCase = true)}”"
}

@Composable
private fun Verso(entrada: Entrada) {
    val ficha = entrada.ficha
    val contexto = LocalContext.current

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        lateinit var motor: TextToSpeech
        motor = TextToSpeech(contexto) { status ->
            if (status == TextToSpeech.SUCCESS) {
                motor.language = Locale.US
                tts = motor
            }
        }
        onDispose { motor.shutdown() }
    }

    Column(modifier = Modifier.padding(top = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = entrada.titulo,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Surface(
                onClick = {
                    tts?.speak(entrada.titulo, TextToSpeech.QUEUE_FLUSH, null, "vocabs-revisao")
                },
                enabled = tts != null,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icones.Som,
                        contentDescription = "Ouvir",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }

        Text(
            text = ficha?.traducao.orEmpty(),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 10.dp),
        )

        // Só a primeira definição: um verso de flashcard precisa ser lido em dois
        // segundos, e a ficha inteira está a um toque de distância.
        ficha?.definicoes?.firstOrNull()?.let { definicao ->
            Text(
                text = definicao,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        // O trecho inteiro, agora sem lacuna: é o pagamento da trava de leitura.
        // Sem o exemplo gerado pela IA — o contexto onde você viu vale mais.
        entrada.trecho?.takeIf { it.isNotBlank() }?.let { trecho ->
            RotuloSecao("ONDE VOCÊ VIU", modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.inverseSurface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "“$trecho”",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontStyle = FontStyle.Italic,
                        lineHeight = 26.sp,
                    ),
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.padding(20.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** Dois botões, só dois. O sistema de pontos existe para ter menos variáveis. */
@Composable
private fun BotoesDeResposta(aoResponder: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = { aoResponder(false) },
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
        ) {
            Icon(
                imageVector = Icones.Fechar,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
            )
            Text(
                text = "Não lembrei",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        Button(
            onClick = { aoResponder(true) },
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
        ) {
            Icon(
                imageVector = Icones.Check,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
            )
            Text(
                text = "Lembrei",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

@Composable
private fun Resumo(
    estado: RevisaoEstado.Resumo,
    aoMaisUmaRodada: () -> Unit,
    aoVoltar: () -> Unit,
) {
    val total = estado.acertos + estado.erros

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "${estado.acertos} de $total",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = if (estado.erros == 0) "Sessão limpa." else "Sessão fechada.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )

        AnimatedVisibility(visible = estado.errados.isNotEmpty(), enter = fadeIn(tween(300))) {
            Text(
                text = "Voltam mais cedo: ${estado.errados.joinToString(" · ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp),
            )
        }

        // Sem troféu, sem chama, sem animação: gamificação é Fase 7. Aqui é só o
        // dado que torna o critério de saída verificável.
        Text(
            text = when {
                estado.diasSeguidos >= 2 -> "${estado.diasSeguidos} dias seguidos"
                else -> "Primeiro dia. Volte amanhã para a sequência começar."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 24.dp),
        )

        if (estado.restantes > 0) {
            Text(
                text = if (estado.restantes == 1) {
                    "Ainda há 1 palavra na fila."
                } else {
                    "Ainda há ${estado.restantes} palavras na fila."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 28.dp),
            )
            Button(
                onClick = aoMaisUmaRodada,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(56.dp),
            ) {
                Icon(
                    imageVector = Icones.Repetir,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Mais uma rodada",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        TextButton(onClick = aoVoltar, modifier = Modifier.padding(top = 12.dp)) {
            Text("Voltar")
        }
    }
}

@Composable
private fun NadaParaRevisar(aoVoltar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(76.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icones.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
        Text(
            text = "Nada pedindo revisão",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            text = "Todas as suas palavras ainda estão frescas. Volte quando elas começarem a esfriar.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        TextButton(onClick = aoVoltar, modifier = Modifier.padding(top = 20.dp)) {
            Text("Voltar")
        }
    }
}
