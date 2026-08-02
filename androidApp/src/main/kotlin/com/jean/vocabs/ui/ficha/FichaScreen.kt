package com.jean.vocabs.ui.ficha

import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.contracts.FichaResponse
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.shared.domain.FormatoCaptura
import com.jean.vocabs.shared.domain.RetencaoAgora
import com.jean.vocabs.shared.domain.StatusEntrada
import com.jean.vocabs.ui.components.AnelProgresso
import com.jean.vocabs.ui.components.BarraDeMemoria
import com.jean.vocabs.ui.components.BotaoCircular
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.RotuloSecao
import com.jean.vocabs.ui.components.TipoBadge
import com.jean.vocabs.ui.components.corDoNivel
import com.jean.vocabs.ui.components.escalaAoPressionar
import com.jean.vocabs.ui.components.rotuloDoNivel
import com.jean.vocabs.ui.components.tempoAte
import com.jean.vocabs.ui.components.tempoRelativo
import java.util.Locale

@Composable
fun FichaScreen(
    id: Long,
    aoVoltar: () -> Unit,
    vm: FichaViewModel = viewModel(),
) {
    // remember(id) evita recriar o StateFlow a cada recomposição.
    val fluxo = remember(id) { vm.observar(id) }
    val entrada by fluxo.collectAsStateWithLifecycle()
    val atual = entrada

    val fluxoMemoria = remember(id) { vm.observarMemoria(id) }
    val memoria by fluxoMemoria.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            BotaoCircular(icone = Icones.Voltar, descricao = "Voltar", aoClicar = aoVoltar)
            Spacer(modifier = Modifier.weight(1f))
            AnimatedVisibility(
                visible = atual?.ficha != null,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { -it / 2 },
            ) {
                atual?.ficha?.let { TipoBadge(it.tipo) }
            }
        }

        if (atual != null) {
            ConteudoEntrada(
                entrada = atual,
                memoria = memoria,
                aoTentarDeNovo = vm::tentarDeNovo,
            )
        }

        Spacer(
            modifier = Modifier
                .navigationBarsPadding()
                .height(32.dp),
        )
    }
}

@Composable
private fun ConteudoEntrada(
    entrada: Entrada,
    memoria: RetencaoAgora?,
    aoTentarDeNovo: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(
            text = entrada.titulo,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 8.dp),
        )

        AnimatedVisibility(visible = entrada.ficha != null, enter = fadeIn(tween(300))) {
            entrada.ficha?.let { ficha ->
                Text(
                    text = "/${ficha.ipa}/   ·   ${ficha.tipo.name.lowercase()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        entrada.trecho?.takeIf { it.isNotBlank() }?.let { trecho ->
            CartaoCitacao(
                trecho = trecho,
                origem = entrada.origem,
                formato = entrada.formato,
                criadoEm = entrada.criadoEm,
                modifier = Modifier.padding(top = 20.dp),
            )
        }

        memoria?.let { BlocoMemoria(it) }

        AnimatedContent(
            targetState = entrada.status,
            transitionSpec = {
                (fadeIn(tween(320)) + slideInVertically(tween(320)) { it / 12 })
                    .togetherWith(fadeOut(tween(140)))
            },
            label = "corpoFicha",
        ) { status ->
            Column(modifier = Modifier.padding(top = 24.dp)) {
                when (status) {
                    StatusEntrada.PRONTA -> entrada.ficha?.let { ficha ->
                        BlocoFicha(entrada = entrada, ficha = ficha)
                    }
                    StatusEntrada.ERRO -> BlocoErro(
                        mensagem = entrada.erro,
                        aoTentarDeNovo = aoTentarDeNovo,
                    )
                    else -> BlocoGerando()
                }
            }
        }
    }
}

@Composable
private fun BlocoMemoria(memoria: RetencaoAgora) {
    Column(modifier = Modifier.padding(top = 24.dp)) {
        RotuloSecao("FORÇA DE MEMÓRIA")

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 10.dp),
        ) {
            BarraDeMemoria(
                pontos = memoria.pontos,
                nivel = memoria.nivel,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${memoria.pontos.toInt()}",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = corDoNivel(memoria.nivel),
                modifier = Modifier.padding(start = 12.dp),
            )
        }

        val quando = if (memoria.proximaEmMillis <= 0L) {
            "pedindo revisão agora"
        } else {
            "próxima revisão ${tempoAte(memoria.proximaEmMillis)}"
        }
        Text(
            text = "${rotuloDoNivel(memoria.nivel)}  ·  $quando",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )

        // O placar só aparece quando existe: dizer "0% de acerto" para uma palavra
        // que ninguém revisou ainda seria desanimador e, pior, falso.
        memoria.taxaDeAcerto?.let { taxa ->
            val porcentagem = (taxa * 100).toInt()
            Text(
                text = "${memoria.acertos} de ${memoria.respondidas} acertos  ·  $porcentagem%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** O trecho onde a palavra apareceu — o contexto pessoal vale mais que dicionário. */
@Composable
private fun CartaoCitacao(
    trecho: String,
    origem: String?,
    formato: FormatoCaptura,
    criadoEm: Long,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.large,
        color = cores.inverseSurface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "“$trecho”",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontStyle = FontStyle.Italic,
                    lineHeight = 26.sp,
                ),
                color = cores.inverseOnSurface,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(cores.primary, CircleShape),
                )
                val onde = origem?.let { "Capturado de $it" } ?: "Capturado"
                val como = when (formato) {
                    FormatoCaptura.FOTO -> " por foto"
                    FormatoCaptura.AUDIO -> " por áudio"
                    FormatoCaptura.TEXTO -> ""
                }
                Text(
                    text = "$onde$como  ·  ${tempoRelativo(criadoEm)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = cores.inverseOnSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun BlocoFicha(entrada: Entrada, ficha: FichaResponse) {
    val contexto = LocalContext.current

    // TTS nativo do aparelho: pronúncia de graça, sem IA e sem rede.
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

    Column {
        RotuloSecao("TRADUÇÃO")
        Text(
            text = ficha.traducao,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 6.dp),
        )

        if (ficha.definicoes.isNotEmpty()) {
            RotuloSecao("SIGNIFICADO", modifier = Modifier.padding(top = 22.dp))
            ficha.definicoes.forEach { definicao ->
                Row(modifier = Modifier.padding(top = 6.dp)) {
                    if (ficha.definicoes.size > 1) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                    Text(
                        text = definicao,
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }

        if (ficha.exemplo.isNotBlank()) {
            RotuloSecao("EXEMPLO", modifier = Modifier.padding(top = 22.dp))
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(
                    text = ficha.exemplo,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontStyle = FontStyle.Italic,
                        lineHeight = 24.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        Row(modifier = Modifier.padding(top = 28.dp)) {
            AcaoTile(
                rotulo = "Ouvir",
                icone = Icones.Som,
                corIcone = MaterialTheme.colorScheme.onPrimaryContainer,
                corFundoIcone = MaterialTheme.colorScheme.primaryContainer,
                habilitado = tts != null,
                aoClicar = {
                    val fala = listOf(entrada.alvo.orEmpty(), ficha.exemplo)
                        .filter { it.isNotBlank() }
                        .joinToString(". ")
                    tts?.speak(fala, TextToSpeech.QUEUE_FLUSH, null, "vocabs-ficha")
                },
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.size(12.dp))
            AcaoTile(
                rotulo = "Compartilhar",
                icone = Icones.Compartilhar,
                corIcone = MaterialTheme.colorScheme.onTertiaryContainer,
                corFundoIcone = MaterialTheme.colorScheme.tertiaryContainer,
                aoClicar = {
                    val texto = buildString {
                        appendLine("${entrada.titulo} /${ficha.ipa}/")
                        appendLine(ficha.traducao)
                        if (ficha.exemplo.isNotBlank()) {
                            appendLine()
                            appendLine("“${ficha.exemplo}”")
                        }
                    }.trim()
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, texto)
                    }
                    contexto.startActivity(Intent.createChooser(intent, "Compartilhar ficha"))
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AcaoTile(
    rotulo: String,
    icone: ImageVector,
    corIcone: Color,
    corFundoIcone: Color,
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
) {
    val interacao = remember { MutableInteractionSource() }
    Surface(
        onClick = aoClicar,
        enabled = habilitado,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        interactionSource = interacao,
        modifier = modifier
            .escalaAoPressionar(interacao)
            .graphicsLayer { alpha = if (habilitado) 1f else 0.5f },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .background(corFundoIcone, CircleShape),
            ) {
                Icon(
                    imageVector = icone,
                    contentDescription = null,
                    tint = corIcone,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = rotulo,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

/** Skeleton pulsante no lugar do conteúdo — a ficha está sendo escrita agora. */
@Composable
private fun BlocoGerando() {
    val alfa by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.05f,
        targetValue = 0.14f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "alfaSkeleton",
    )
    val corBarra = MaterialTheme.colorScheme.onSurface.copy(alpha = alfa)

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnelProgresso(
                girando = true,
                cor = MaterialTheme.colorScheme.primary,
                tamanho = 20.dp,
            )
            Text(
                text = "Escrevendo a ficha…",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 10.dp),
            )
        }

        listOf(1f, 0.85f, 0.6f).forEach { fracao ->
            Box(
                modifier = Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth(fracao)
                    .height(14.dp)
                    .background(corBarra, RoundedCornerShape(7.dp)),
            )
        }

        Text(
            text = "Você não precisa esperar aqui — a ficha chega sozinha.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 18.dp),
        )
    }
}

@Composable
private fun BlocoErro(mensagem: String?, aoTentarDeNovo: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Não deu para gerar a ficha",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = mensagem ?: "O servidor não respondeu. Tente de novo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 6.dp),
            )
            Button(
                onClick = aoTentarDeNovo,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Icon(
                    imageVector = Icones.Repetir,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Tentar de novo",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
