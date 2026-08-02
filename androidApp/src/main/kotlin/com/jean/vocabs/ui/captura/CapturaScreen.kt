package com.jean.vocabs.ui.captura

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.ui.components.AvisoDuplicata
import com.jean.vocabs.ui.components.BotaoCircular
import com.jean.vocabs.ui.components.Icones

/**
 * A captura por escrito.
 *
 * Áudio e foto saíram daqui: eles vivem no botão central da barra, alcançáveis
 * de qualquer aba sem trocar de tela. O que sobrou é o caminho mais lento e mais
 * completo dos três — e o único que precisa de teclado.
 */
@Composable
fun CapturaScreen(
    aoCapturarTexto: () -> Unit,
    aoVoltar: () -> Unit,
    vm: CapturaViewModel = viewModel(),
) {
    var trecho by rememberSaveable { mutableStateOf("") }
    var alvo by rememberSaveable { mutableStateOf("") }
    var origem by rememberSaveable { mutableStateOf("") }

    val duplicata by vm.duplicata.collectAsStateWithLifecycle()

    val focoTrecho = remember { FocusRequester() }
    val gerenciadorFoco = LocalFocusManager.current

    // É isto que faz a captura caber em 10 segundos: o teclado já sobe com o
    // cursor no campo certo, sem um toque a mais.
    LaunchedEffect(Unit) { focoTrecho.requestFocus() }

    LaunchedEffect(alvo) { vm.procurarDuplicata(alvo) }

    val podeSalvar = trecho.isNotBlank() && alvo.isNotBlank()

    fun salvarTexto() {
        if (!podeSalvar) return
        vm.salvarTexto(trecho = trecho, alvo = alvo, origem = origem)
        trecho = ""
        alvo = ""
        gerenciadorFoco.clearFocus()
        aoCapturarTexto()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp),
    ) {
        Box(modifier = Modifier.padding(top = 8.dp)) {
            BotaoCircular(icone = Icones.Voltar, descricao = "Voltar", aoClicar = aoVoltar)
        }

        Text(
            text = "Escrever",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = "O que te pegou?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
        )

        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                CampoCaptura(
                    valor = trecho,
                    aoMudar = { trecho = it },
                    rotulo = "Trecho",
                    dica = "A frase onde apareceu",
                    minLinhas = 3,
                    opcoesTeclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.focusRequester(focoTrecho),
                )
                DivisorSuave()
                CampoCaptura(
                    valor = alvo,
                    aoMudar = { alvo = it },
                    rotulo = "Alvo",
                    dica = "O que te pegou (1 palavra ou várias)",
                    umaLinha = true,
                    opcoesTeclado = KeyboardOptions(imeAction = ImeAction.Next),
                    acoesTeclado = KeyboardActions(
                        onNext = { gerenciadorFoco.moveFocus(FocusDirection.Down) },
                    ),
                )
                DivisorSuave()
                CampoCaptura(
                    valor = origem,
                    aoMudar = { origem = it },
                    rotulo = "Origem (opcional)",
                    dica = "jogo, livro, série…",
                    umaLinha = true,
                    opcoesTeclado = KeyboardOptions(imeAction = ImeAction.Done),
                    acoesTeclado = KeyboardActions(onDone = { salvarTexto() }),
                )
            }
        }

        AnimatedVisibility(
            visible = duplicata != null,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(140)),
        ) {
            duplicata?.let { entrada ->
                AvisoDuplicata(
                    entrada = entrada,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        Button(
            onClick = ::salvarTexto,
            enabled = podeSalvar,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .height(56.dp),
        ) {
            Icon(
                imageVector = Icones.Mais,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Salvar captura",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        Text(
            text = "A ficha é gerada sozinha depois. Você pode voltar ao que estava fazendo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )

        Spacer(modifier = Modifier.height(150.dp))
    }
}

/** Campo sem borda dentro do cartão — a moldura é o próprio cartão. */
@Composable
private fun CampoCaptura(
    valor: String,
    aoMudar: (String) -> Unit,
    rotulo: String,
    dica: String,
    modifier: Modifier = Modifier,
    minLinhas: Int = 1,
    umaLinha: Boolean = false,
    opcoesTeclado: KeyboardOptions = KeyboardOptions.Default,
    acoesTeclado: KeyboardActions = KeyboardActions.Default,
) {
    TextField(
        value = valor,
        onValueChange = aoMudar,
        label = { Text(rotulo) },
        placeholder = { Text(dica) },
        minLines = minLinhas,
        singleLine = umaLinha,
        keyboardOptions = opcoesTeclado,
        keyboardActions = acoesTeclado,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun DivisorSuave() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}
