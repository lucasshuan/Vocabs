package com.jean.vocabs.ui.captura

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.shared.domain.AlvoSelecionado
import com.jean.vocabs.shared.domain.FormatoCaptura
import com.jean.vocabs.ui.components.AvisoDuplicata
import com.jean.vocabs.ui.components.BotaoCircular
import com.jean.vocabs.ui.components.BotaoPrincipal
import com.jean.vocabs.ui.components.ChipsDeSelecao
import com.jean.vocabs.ui.components.DiscoDeIcone
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.PilulaSelecionavel
import com.jean.vocabs.ui.components.RotuloDeSecao
import com.jean.vocabs.ui.components.SeletorDeTermos
import com.jean.vocabs.ui.components.formatarDuracao

/**
 * O nome de cada aba. Não há enum próprio para elas: as abas **são** os formatos
 * de captura, e um segundo enum com os mesmos três nomes só criaria a chance de
 * um ganhar um caso a mais que o outro não tem.
 */
fun rotuloDoFormato(formato: FormatoCaptura): String = when (formato) {
    FormatoCaptura.TEXTO -> "Texto"
    FormatoCaptura.AUDIO -> "Áudio"
    FormatoCaptura.FOTO -> "Foto"
}

@Composable
fun CapturaScreen(
    aoCapturarTexto: () -> Unit,
    aoVoltar: () -> Unit,
    formatoInicial: FormatoCaptura = FormatoCaptura.TEXTO,
    vm: CapturaViewModel = viewModel(),
) {
    // A escolha do leque é só o ponto de partida: uma vez dentro, as abas
    // continuam valendo, e quem entrou por Foto sem gostar do resultado troca
    // para Texto sem sair e voltar.
    var aba by remember { mutableStateOf(formatoInicial) }
    var trecho by remember { mutableStateOf("") }
    val selecoes = remember { mutableStateListOf<AlvoSelecionado>() }
    val duplicata by vm.duplicata.collectAsStateWithLifecycle()
    val captura = rememberCapturaRapida(
        aoSalvarMidia = vm::salvarMidia,
        aoCapturado = { aoCapturarTexto() },
    )

    LaunchedEffect(selecoes.lastOrNull()?.texto) {
        vm.procurarDuplicata(selecoes.lastOrNull()?.texto.orEmpty())
    }

    fun salvarTexto() {
        if (trecho.isBlank() || selecoes.isEmpty()) return
        vm.salvarTexto(trecho, selecoes.toList())
        aoCapturarTexto()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 14.dp, top = 12.dp, bottom = 4.dp),
        ) {
            Text("Capturar", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            BotaoCircular(Icones.Fechar, "Fechar captura", aoVoltar)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            FormatoCaptura.entries.forEach { item ->
                PilulaSelecionavel(
                    rotulo = rotuloDoFormato(item),
                    selecionada = aba == item,
                    aoClicar = { aba = item },
                    forma = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(46.dp),
                )
            }
        }

        when (aba) {
            FormatoCaptura.TEXTO -> ConteudoTexto(
                trecho = trecho,
                selecoes = selecoes,
                aoMudarTrecho = {
                    trecho = it
                    selecoes.clear()
                },
                aoSelecionar = { alvo ->
                    if (selecoes.none { it.inicio == alvo.inicio && it.fim == alvo.fim }) selecoes += alvo
                },
                aoRemover = selecoes::remove,
            )
            FormatoCaptura.AUDIO -> ConteudoMidia(
                icone = Icones.Microfone,
                titulo = if (captura.gravando) "Gravando ${formatarDuracao(captura.segundos)}" else "Gravar um trecho",
                detalhe = "O reconhecimento local tenta transcrever em inglês. Você sempre poderá editar.",
                acao = if (captura.gravando) "Parar e salvar" else "Começar gravação",
                aoClicar = { if (captura.gravando) captura.pararAudio() else captura.gravarAudio() },
            )
            FormatoCaptura.FOTO -> ConteudoMidia(
                icone = Icones.Camera,
                titulo = "Fotografar um trecho",
                detalhe = "O texto é lido no aparelho pelo modelo latino, sem enviar a imagem.",
                acao = "Abrir câmera",
                aoClicar = captura::tirarFoto,
            )
        }

        if (aba == FormatoCaptura.TEXTO) {
            duplicata?.let { AvisoDuplicata(it, Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
            BotaoPrincipal(
                texto = if (selecoes.isEmpty()) "Selecione o que guardar" else "Guardar ${selecoes.size} ${if (selecoes.size == 1) "captura" else "capturas"}",
                aoClicar = ::salvarTexto,
                habilitado = trecho.isNotBlank() && selecoes.isNotEmpty(),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }

        Spacer(Modifier.navigationBarsPadding().height(24.dp))
    }
}

@Composable
private fun ConteudoTexto(
    trecho: String,
    selecoes: List<AlvoSelecionado>,
    aoMudarTrecho: (String) -> Unit,
    aoSelecionar: (AlvoSelecionado) -> Unit,
    aoRemover: (AlvoSelecionado) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        RotuloDeSecao("Trecho onde apareceu")
        OutlinedTextField(
            value = trecho,
            onValueChange = aoMudarTrecho,
            placeholder = { Text("The plan went haywire when the power cut out.") },
            minLines = 2,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )

        if (trecho.isNotBlank()) {
            RotuloDeSecao("Selecione o que chamou atenção", Modifier.padding(top = 18.dp, bottom = 9.dp))
            SeletorDeTermos(trecho, aoSelecionar)
            Text(
                "Toque numa palavra ou arraste por várias.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (selecoes.isNotEmpty()) {
            RotuloDeSecao("${selecoes.size} ${if (selecoes.size == 1) "selecionada" else "selecionadas"}", Modifier.padding(top = 18.dp, bottom = 9.dp))
            ChipsDeSelecao(selecoes, aoRemover)
        }
    }
}

@Composable
private fun ConteudoMidia(
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    detalhe: String,
    acao: String,
    aoClicar: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 30.dp),
    ) {
        DiscoDeIcone(
            icone = icone,
            descricao = null,
            cor = MaterialTheme.colorScheme.primary,
            fundo = MaterialTheme.colorScheme.secondaryContainer,
            tamanho = 92.dp,
        )
        Text(titulo, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 20.dp))
        Text(
            detalhe,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        BotaoPrincipal(acao, aoClicar, Modifier.padding(top = 24.dp))
    }
}
