package com.jean.vocabs.ui.ficha

import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.jean.vocabs.shared.domain.StatusEntrada
import com.jean.vocabs.ui.components.BarraDeMemoria
import com.jean.vocabs.ui.components.BotaoCircular
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.TipoBadge
import com.jean.vocabs.ui.components.rotuloDoNivel
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
    val tts = rememberTts()

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
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 18.dp, vertical = 10.dp)) {
            BotaoCircular(Icones.Voltar, "Voltar", aoVoltar)
            Spacer(Modifier.weight(1f))
            androidx.compose.foundation.layout.Box {
                IconButton(onClick = { menu = true }) { Icon(Icones.MaisVertical, "Mais opções") }
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
        Column(Modifier.padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(item.titulo, style = MaterialTheme.typography.displaySmall)
                    item.ficha?.ipa?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                Surface(onClick = { tts?.speak(item.titulo, TextToSpeech.QUEUE_FLUSH, null, "tagarara-ficha") }, shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icones.Som, "Ouvir pronúncia", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(13.dp))
                }
            }
            TipoBadge(item.tipo, Modifier.padding(top = 12.dp))

            when (item.status) {
                StatusEntrada.GERANDO, StatusEntrada.PENDENTE -> Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth().padding(top = 22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(18.dp)) {
                        CircularProgressIndicator(Modifier.height(22.dp), strokeWidth = 2.dp)
                        Text("A ficha está sendo criada…", Modifier.padding(start = 12.dp))
                    }
                }
                StatusEntrada.ERRO -> Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth().padding(top = 22.dp)) {
                    Column(Modifier.padding(18.dp)) {
                        Text(item.erro ?: "Não foi possível gerar a ficha.", color = MaterialTheme.colorScheme.onErrorContainer)
                        Button(onClick = vm::tentarDeNovo, modifier = Modifier.padding(top = 12.dp)) { Text("Tentar de novo") }
                    }
                }
                StatusEntrada.PRONTA -> item.ficha?.let { ficha ->
                    Secao("TRADUÇÃO") { Text(ficha.traducao, style = MaterialTheme.typography.headlineMedium) }
                    memoria?.let { retencao ->
                        Secao("FORÇA DE MEMÓRIA") {
                            BarraDeMemoria(retencao.pontos, retencao.nivel)
                            Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(rotuloDoNivel(retencao.nivel), color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelLarge)
                                Text("${retencao.pontos.toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Secao("SEU CONTEXTO") {
                        Text(item.trecho.orEmpty(), style = MaterialTheme.typography.titleMedium)
                    }
                    Secao("DEFINIÇÕES") {
                        ficha.definicoes.forEachIndexed { indice, definicao ->
                            Row(Modifier.padding(bottom = 10.dp)) {
                                Text("${indice + 1}.", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                                Text(definicao, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                        Text("Exemplo: ${ficha.exemplo}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (ficha.relacionadas.isNotEmpty()) {
                        Secao("TERMOS RELACIONADOS") {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                (if (expandiu) ficha.relacionadas else ficha.relacionadas.take(3)).forEach { termo ->
                                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                                        Text(termo, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                                    }
                                }
                            }
                            if (ficha.relacionadas.size > 3) TextButton(onClick = { expandiu = !expandiu }) { Text(if (expandiu) "Ver menos" else "Ver mais") }
                        }
                    }
                }
            }
            Spacer(Modifier.navigationBarsPadding().height(36.dp))
        }
    }
}

@Composable
private fun Secao(rotulo: String, conteudo: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 24.dp)) {
        Text(rotulo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 9.dp))
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(17.dp)) { conteudo() }
        }
    }
}

@Composable
private fun rememberTts(): TextToSpeech? {
    val contexto = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(contexto) {
        val motor = TextToSpeech(contexto) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
        tts = motor
        onDispose { motor.stop(); motor.shutdown() }
    }
    return tts
}
