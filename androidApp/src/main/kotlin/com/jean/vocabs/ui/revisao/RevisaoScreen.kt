package com.jean.vocabs.ui.revisao

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.shared.domain.trechoCloze
import com.jean.vocabs.ui.components.AcaoSecundaria
import com.jean.vocabs.ui.components.BotaoPrincipal
import com.jean.vocabs.ui.components.BotaoCircular
import com.jean.vocabs.ui.components.CartaoDaTela
import com.jean.vocabs.ui.components.CartaoMetrica
import com.jean.vocabs.ui.components.EstadoVazio
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.RotuloDeSecao

private const val LACUNA = "________"

@Composable
fun RevisaoScreen(aoVoltar: () -> Unit, vm: RevisaoViewModel = viewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 8.dp),
        ) {
            BotaoCircular(Icones.Fechar, "Fechar revisão", aoVoltar)
            val cartao = estado as? RevisaoEstado.Cartao
            BarraDeProgresso(
                fracao = cartao?.let { it.posicao.toFloat() / it.total.coerceAtLeast(1) } ?: 0f,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            )
            Text(
                text = cartao?.let { "${it.posicao}/${it.total}" }.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
private fun BarraDeProgresso(fracao: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(6.dp)
            .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(3.dp)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fracao.coerceIn(0f, 1f))
                .height(6.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)),
        )
    }
}

@Composable
private fun Cartao(estado: RevisaoEstado.Cartao, vm: RevisaoViewModel) {
    val teclado = LocalSoftwareKeyboardController.current
    Column(
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        CartaoDaTela(
            forma = MaterialTheme.shapes.extraLarge,
            recheio = PaddingValues(24.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            RotuloDeSecao("COMPLETE O SEU TRECHO")
            Text(
                text = clozeAnotado(trechoCloze(estado.entrada, LACUNA)),
                style = MaterialTheme.typography.headlineSmall.copy(lineHeight = MaterialTheme.typography.headlineLarge.lineHeight),
                modifier = Modifier.padding(vertical = 22.dp),
            )
            Text(
                estado.entrada.ficha?.traducao.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = estado.resposta,
            onValueChange = vm::editarResposta,
            enabled = estado.feedback == null,
            placeholder = { Text("Escreva o termo original") },
            singleLine = false,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { teclado?.hide(); vm.confirmar() }),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )

        estado.feedback?.let { feedback ->
            val acertou = feedback == FeedbackRevisao.CORRETA
            Surface(
                color = if (acertou) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer,
                contentColor = if (acertou) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        when {
                            acertou -> "Boa — você lembrou"
                            feedback == FeedbackRevisao.NAO_LEMBRO -> "Tudo bem. A resposta é:"
                            else -> "Quase. A resposta é:"
                        },
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(estado.entrada.alvo.orEmpty(), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }

        if (estado.feedback == null) {
            BotaoPrincipal("Verificar", { teclado?.hide(); vm.confirmar() }, habilitado = estado.resposta.isNotBlank())
            AcaoSecundaria("Não lembro", { teclado?.hide(); vm.naoLembro() })
        } else {
            BotaoPrincipal("Continuar", vm::avancar)
        }
        Spacer(Modifier.navigationBarsPadding().height(20.dp))
    }
}

/** A lacuna vira um traço de ameixa: no handoff ela é um sublinhado, não underscores. */
@Composable
private fun clozeAnotado(trecho: String) = buildAnnotatedString {
    val corte = trecho.indexOf(LACUNA)
    if (corte < 0) {
        append(trecho)
        return@buildAnnotatedString
    }
    append(trecho.substring(0, corte))
    pushStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline))
    append(LACUNA)
    pop()
    append(trecho.substring(corte + LACUNA.length))
}

@Composable
private fun Vazia(aoVoltar: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EstadoVazio(
            icone = Icones.Check,
            titulo = "Memória em dia",
            detalhe = "Volte quando alguma ficha pedir reforço.",
            acao = { BotaoPrincipal("Voltar", aoVoltar, Modifier.padding(horizontal = 40.dp)) },
        )
    }
}

@Composable
private fun Resumo(estado: RevisaoEstado.Resumo, vm: RevisaoViewModel, aoVoltar: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
    ) {
        Text("Sessão concluída", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 48.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
            CartaoMetrica("${estado.acertos}", "acertos", Modifier.weight(1f), destaque = true)
            CartaoMetrica("${estado.erros}", "para reforçar", Modifier.weight(1f))
            CartaoMetrica("${estado.diasSeguidos}", "dias seguidos", Modifier.weight(1f), destaque = true)
        }
        if (estado.errados.isNotEmpty()) {
            Text(
                "Voltam para a fila: ${estado.errados.distinct().joinToString()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 20.dp),
            )
        }
        BotaoPrincipal("Concluir", aoVoltar, Modifier.padding(top = 28.dp))
        if (estado.restantes > 0) AcaoSecundaria("Mais uma rodada (${estado.restantes})", vm::novaRodada)
        Spacer(Modifier.navigationBarsPadding().height(20.dp))
    }
}
