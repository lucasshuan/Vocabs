package com.jean.vocabs.ui.ficha

import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.shared.domain.RetencaoAgora
import com.jean.vocabs.shared.domain.StatusEntrada
import com.jean.vocabs.ui.components.BarraDeMemoria
import com.jean.vocabs.ui.components.BotaoCircular
import com.jean.vocabs.ui.components.CartaoDaTela
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.Movimento
import com.jean.vocabs.ui.components.Pilula
import com.jean.vocabs.ui.components.RotuloDeSecao
import com.jean.vocabs.ui.components.TipoBadge
import com.jean.vocabs.ui.components.contornoDeCartao
import com.jean.vocabs.ui.components.corDoRotuloDoNivel
import com.jean.vocabs.ui.components.rotuloDoNivel
import com.jean.vocabs.ui.components.tempoAte
import com.jean.vocabs.ui.components.tempoRelativo
import com.jean.vocabs.ui.idiomas.idiomaDe
import java.util.Locale

@Composable
fun FichaScreen(id: Long, aoVoltar: () -> Unit, vm: FichaViewModel = viewModel()) {
    val entradaFlow = remember(id) { vm.observar(id) }
    val memoriaFlow = remember(id) { vm.observarMemoria(id) }
    val entrada by entradaFlow.collectAsStateWithLifecycle()
    val memoria by memoriaFlow.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var confirmarExclusao by remember { mutableStateOf(false) }
    var expandiu by remember { mutableStateOf(false) }
    val tts = rememberTts(idiomaDe(entrada?.par?.alvo).etiqueta)

    if (confirmarExclusao) {
        AlertDialog(
            onDismissRequest = { confirmarExclusao = false },
            title = { Text("Excluir esta ficha?") },
            text = { Text("A mídia compartilhada será preservada enquanto houver outra ficha da mesma captura.") },
            confirmButton = {
                TextButton(onClick = { vm.excluir(); confirmarExclusao = false; aoVoltar() }) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmarExclusao = false }) { Text("Cancelar") } },
        )
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            BotaoCircular(Icones.Voltar, "Voltar", aoVoltar)
            Spacer(Modifier.weight(1f))
            Box {
                BotaoCircular(Icones.MaisVertical, "Mais opções", { menu = true })
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Compartilhar") },
                        leadingIcon = { Icon(Icones.Compartilhar, null) },
                        onClick = {
                            menu = false
                            entrada?.let { item ->
                                val texto = "${item.titulo} — ${item.ficha?.traducao.orEmpty()}\n${item.trecho.orEmpty()}\nTagarara"
                                contexto.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, texto)
                                }, "Compartilhar ficha"))
                            }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Excluir", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icones.Lixeira, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menu = false; confirmarExclusao = true },
                    )
                }
            }
        }

        val item = entrada
        if (item == null) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(top = 100.dp))
            return@Column
        }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(horizontal = 20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(item.titulo, style = MaterialTheme.typography.displaySmall, modifier = Modifier.weight(1f, fill = false))
                    // Sem voz instalada para o idioma da ficha o botão não
                    // aparece: um botão que não faz nada é pior que a ausência
                    // dele, e falar alemão com voz portuguesa seria pior ainda.
                    tts?.let { voz ->
                        Surface(
                            onClick = { voz.speak(item.titulo, TextToSpeech.QUEUE_FLUSH, null, "tagarara-ficha") },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Icon(
                                imageVector = Icones.Tocar,
                                contentDescription = "Ouvir pronúncia",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp).padding(8.dp),
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item.ficha?.pronuncia?.takeIf(String::isNotBlank)?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TipoBadge(item.tipo)
                }
                item.ficha?.traducao?.takeIf(String::isNotBlank)?.let {
                    Text(it, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 2.dp))
                }
            }

            // A ficha pode ficar pronta com a tela aberta: quem toca numa linha de
            // Pendentes cai aqui enquanto a IA ainda trabalha. O corpo inteiro
            // cruza de um estado para o outro em vez de ser trocado no lugar —
            // sem isso, a tela pisca de "sendo criada…" para uma página cheia de
            // texto, e o que foi a resposta chegando parece um erro de desenho.
            AnimatedContent(
                targetState = item.status,
                transitionSpec = { fadeIn(tween(Movimento.PADRAO)) togetherWith fadeOut(tween(Movimento.RAPIDO)) },
                label = "corpoDaFicha",
            ) { status -> when (status) {
                StatusEntrada.GERANDO, StatusEntrada.PENDENTE -> Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(18.dp)) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        Text("A ficha está sendo criada…", Modifier.padding(start = 12.dp))
                    }
                }
                StatusEntrada.ERRO -> Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(item.erro ?: "Não foi possível gerar a ficha.", color = MaterialTheme.colorScheme.onErrorContainer)
                        Button(onClick = vm::tentarDeNovo, modifier = Modifier.padding(top = 12.dp)) { Text("Tentar de novo") }
                    }
                }
                StatusEntrada.PRONTA -> FichaPronta(
                    entrada = item,
                    memoria = memoria,
                    expandiu = expandiu,
                    aoAlternarRelacionadas = { expandiu = !expandiu },
                )
            } }
            Spacer(Modifier.navigationBarsPadding().height(36.dp))
        }
    }
}

/**
 * O corpo da ficha depois que a IA respondeu.
 *
 * Saiu de dentro do `when` para virar um filho só: o `AnimatedContent` que cruza
 * os estados da ficha entrega um slot por estado, e a coluna com o mesmo
 * espaçamento da tela é o que mantém os quatro blocos com o respiro que tinham
 * quando eram irmãos diretos.
 */
@Composable
private fun FichaPronta(
    entrada: Entrada,
    memoria: RetencaoAgora?,
    expandiu: Boolean,
    aoAlternarRelacionadas: () -> Unit,
) {
    val ficha = entrada.ficha ?: return
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        memoria?.let { retencao ->
            CartaoDaTela(forma = MaterialTheme.shapes.medium, recheio = PaddingValues(15.dp), modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    RotuloDeSecao("Força de memória", Modifier.weight(1f))
                    Text(
                        "${rotuloDoNivel(retencao.nivel)} · ${retencao.pontos.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = corDoRotuloDoNivel(retencao.nivel),
                    )
                }
                BarraDeMemoria(retencao.pontos, retencao.nivel, Modifier.fillMaxWidth().padding(top = 9.dp))
                Text(
                    text = if (retencao.proximaEmMillis <= 0L) "Está na fila de revisão agora."
                    else "Volta para revisão ${tempoAte(retencao.proximaEmMillis)}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 9.dp),
                )
            }
        }

        entrada.trecho?.takeIf(String::isNotBlank)?.let { trecho ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RotuloDeSecao("Seu contexto")
                TrechoDestacado(trecho)
                Text(
                    text = listOfNotNull(entrada.origem?.takeIf(String::isNotBlank), tempoRelativo(entrada.criadoEm)).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RotuloDeSecao("Definições")
            ficha.definicoes.forEachIndexed { indice, definicao ->
                Text("${indice + 1}. $definicao", style = MaterialTheme.typography.bodyMedium)
            }
            ficha.exemplo.takeIf(String::isNotBlank)?.let {
                Text(
                    "Exemplo: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (ficha.relacionadas.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                RotuloDeSecao("Puxa outras palavras")
                // `animateContentSize` porque "ver mais" muda a altura do bloco:
                // sem ele, o resto da ficha salta para baixo de um quadro para o
                // outro e ninguém vê de onde as pílulas novas saíram.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier.animateContentSize(Movimento.mola()),
                ) {
                    (if (expandiu) ficha.relacionadas else ficha.relacionadas.take(3)).forEach { termo ->
                        Pilula(termo)
                    }
                    if (ficha.relacionadas.size > 3) {
                        Pilula(
                            texto = if (expandiu) "ver menos" else "ver mais",
                            destaque = true,
                            aoClicar = aoAlternarRelacionadas,
                        )
                    }
                }
            }
        }
    }
}

/** O trecho do usuário, com a barra de ameixa que diz "isto é seu, não do dicionário". */
@Composable
private fun TrechoDestacado(trecho: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = contornoDeCartao(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)),
            )
            Text(trecho, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp))
        }
    }
}

/**
 * A voz na língua da **ficha**, e não na do curso aberto.
 *
 * A ficha guarda o par em que nasceu justamente para isto: abrir uma palavra
 * alemã antiga depois de trocar para o espanhol tem que falar alemão. Falar a
 * palavra certa com o sotaque errado é pior do que não falar.
 *
 * `setLanguage` devolve `LANG_MISSING_DATA`/`LANG_NOT_SUPPORTED` quando o
 * aparelho não tem a voz instalada — o mesmo caso do transcritor sem modelo. Aí
 * o botão some, em vez de pronunciar alemão em português.
 */
@Composable
private fun rememberTts(etiqueta: String): TextToSpeech? {
    val contexto = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(contexto, etiqueta) {
        // O callback pode ler o próprio motor porque `onInit` só chega depois de
        // a construção retornar: ela depende de um bind de serviço, que é
        // assíncrono.
        var motor: TextToSpeech? = null
        motor = TextToSpeech(contexto) { status ->
            val disponivel = status == TextToSpeech.SUCCESS &&
                motor?.setLanguage(Locale.forLanguageTag(etiqueta)) !in VOZ_AUSENTE
            tts = motor.takeIf { disponivel }
        }
        onDispose {
            motor.stop()
            motor.shutdown()
            tts = null
        }
    }
    return tts
}

private val VOZ_AUSENTE = setOf(TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED, null)
