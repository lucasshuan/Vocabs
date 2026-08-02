package com.jean.vocabs.ui.revisao

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.shared.domain.trechoCloze
import com.jean.vocabs.ui.components.BotaoCircular
import com.jean.vocabs.ui.components.Icones

@Composable
fun RevisaoScreen(aoVoltar: () -> Unit, vm: RevisaoViewModel = viewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp)) {
            BotaoCircular(Icones.Fechar, "Fechar revisão", aoVoltar)
            Text("Revisão", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 14.dp).weight(1f))
            (estado as? RevisaoEstado.Cartao)?.let { Text("${it.posicao}/${it.total}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        when (val atual = estado) {
            RevisaoEstado.Carregando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            RevisaoEstado.Vazia -> Vazia(aoVoltar)
            is RevisaoEstado.Cartao -> Cartao(atual, vm)
            is RevisaoEstado.Resumo -> Resumo(atual, vm, aoVoltar)
        }
    }
}

@Composable
private fun Cartao(estado: RevisaoEstado.Cartao, vm: RevisaoViewModel) {
    val teclado = LocalSoftwareKeyboardController.current
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
    ) {
        Text("COMPLETE O TRECHO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 26.dp))
        Text(
            trechoCloze(estado.entrada),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 14.dp, bottom = 22.dp),
        )
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text(estado.entrada.ficha?.traducao.orEmpty(), style = MaterialTheme.typography.titleMedium)
                Text("Use a tradução como pista; escreva o termo original.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
            }
        }
        OutlinedTextField(
            value = estado.resposta,
            onValueChange = vm::editarResposta,
            enabled = estado.feedback == null,
            label = { Text("Sua resposta") },
            singleLine = false,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { teclado?.hide(); vm.confirmar() }),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
        )

        estado.feedback?.let { feedback ->
            val acertou = feedback == FeedbackRevisao.CORRETA
            Surface(
                color = if (acertou) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer,
                contentColor = if (acertou) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(if (acertou) "Boa — você lembrou" else if (feedback == FeedbackRevisao.NAO_LEMBRO) "Tudo bem. A resposta é:" else "Quase. A resposta é:", style = MaterialTheme.typography.titleMedium)
                    Text(estado.entrada.alvo.orEmpty(), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }

        if (estado.feedback == null) {
            Button(onClick = { teclado?.hide(); vm.confirmar() }, enabled = estado.resposta.isNotBlank(), modifier = Modifier.fillMaxWidth().padding(top = 18.dp).height(56.dp)) { Text("Conferir") }
            TextButton(onClick = { teclado?.hide(); vm.naoLembro() }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Não lembro") }
        } else {
            Button(onClick = vm::avancar, modifier = Modifier.fillMaxWidth().padding(top = 18.dp).height(56.dp)) { Text("Continuar") }
        }
        Spacer(Modifier.navigationBarsPadding().height(30.dp))
    }
}

@Composable
private fun Vazia(aoVoltar: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(76.dp).background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape)) {
            Icon(Icones.Check, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(32.dp))
        }
        Text("Memória em dia", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 18.dp))
        Text("Volte quando alguma ficha pedir reforço.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = aoVoltar, modifier = Modifier.padding(top = 22.dp)) { Text("Voltar") }
    }
}

@Composable
private fun Resumo(estado: RevisaoEstado.Resumo, vm: RevisaoViewModel, aoVoltar: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text("Sessão concluída", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 48.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
            ResumoNumero("${estado.acertos}", "acertos", Modifier.weight(1f), true)
            ResumoNumero("${estado.erros}", "para reforçar", Modifier.weight(1f), false)
            ResumoNumero("${estado.diasSeguidos}", "dias", Modifier.weight(1f), true)
        }
        if (estado.errados.isNotEmpty()) {
            Text("Revistas novamente: ${estado.errados.distinct().joinToString()}", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 20.dp))
        }
        Button(onClick = aoVoltar, modifier = Modifier.fillMaxWidth().padding(top = 28.dp).height(56.dp)) { Text("Concluir") }
        if (estado.restantes > 0) TextButton(onClick = vm::novaRodada) { Text("Mais uma rodada (${estado.restantes})") }
    }
}

@Composable
private fun ResumoNumero(valor: String, rotulo: String, modifier: Modifier, positivo: Boolean) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 16.dp, horizontal = 2.dp)) {
            Text(valor, style = MaterialTheme.typography.headlineSmall, color = if (positivo) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface)
            Text(rotulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
