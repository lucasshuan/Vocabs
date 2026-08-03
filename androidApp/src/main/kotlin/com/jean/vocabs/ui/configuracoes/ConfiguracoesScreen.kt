package com.jean.vocabs.ui.configuracoes

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.shared.PreferenciaDeTema
import com.jean.vocabs.ui.components.CabecalhoDeDentro
import com.jean.vocabs.ui.components.CartaoDaTela
import com.jean.vocabs.ui.components.ChevronDeLinha
import com.jean.vocabs.ui.components.LinhaDeLista
import com.jean.vocabs.ui.components.RotuloDeSecao
import com.jean.vocabs.ui.components.contornoDeCartao
import java.io.File

/**
 * Tela 5b do handoff — "Configurações".
 *
 * Reúne o que não tem nada a ver com idioma nenhum: o segmentado de tema e as
 * ações de portabilidade dos dados. Enfiar qualquer uma delas na tela Você
 * deixaria a faixa de idiomas dividindo espaço com preferências que não mudam
 * conforme o curso aberto.
 */
@Composable
fun ConfiguracoesScreen(aoVoltar: () -> Unit, vm: ConfiguracoesViewModel = viewModel()) {
    val tema by vm.tema.collectAsStateWithLifecycle()
    val exportando by vm.exportando.collectAsStateWithLifecycle()
    val contexto = LocalContext.current

    val importarArquivo = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            Toast.makeText(contexto, "Importação ainda não disponível — em breve.", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(15.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        CabecalhoDeDentro("Configurações", aoVoltar, Modifier.padding(top = 8.dp))

        CartaoDaTela(recheio = PaddingValues(16.dp), modifier = Modifier.fillMaxWidth()) {
            RotuloDeSecao("Aparência")
            Text("Tema", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 10.dp))
            Text(
                text = "Auto segue o aparelho.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            Segmentado(
                opcoes = PreferenciaDeTema.entries,
                selecionada = tema,
                rotulo = ::rotuloDoTema,
                aoEscolher = vm::escolherTema,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        RotuloDeSecao("Dados")

        LinhaDeLista(
            titulo = "Exportar meus dados",
            detalhe = if (exportando) "preparando ZIP…" else null,
            aoClicar = {
                vm.exportar(
                    aoPronto = { arquivo -> compartilhar(contexto, arquivo) },
                    aoErro = { Toast.makeText(contexto, it, Toast.LENGTH_LONG).show() },
                )
            },
            fim = {
                if (exportando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else ChevronDeLinha()
            },
        )

        LinhaDeLista(
            titulo = "Importar meus dados",
            aoClicar = { importarArquivo.launch(arrayOf("application/zip")) },
            fim = { ChevronDeLinha() },
        )
    }
}

private fun compartilhar(contexto: android.content.Context, arquivo: File) {
    val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", arquivo)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    contexto.startActivity(Intent.createChooser(intent, "Exportar dados da Tagarara"))
}

private fun rotuloDoTema(tema: PreferenciaDeTema): String = when (tema) {
    PreferenciaDeTema.CLARO -> "Claro"
    PreferenciaDeTema.ESCURO -> "Escuro"
    PreferenciaDeTema.AUTO -> "Auto"
}

/**
 * O segmentado de largura total: as opções repartem a linha em partes iguais.
 *
 * Partes iguais e não larguras conforme o texto — com "Claro", "Escuro" e "Auto"
 * o resultado seria três alvos de tamanhos diferentes para escolhas do mesmo
 * peso, e o menor deles ficaria abaixo do mínimo de toque.
 */
@Composable
private fun <T> Segmentado(
    opcoes: List<T>,
    selecionada: T,
    rotulo: (T) -> String,
    aoEscolher: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = cores.surfaceVariant,
        border = contornoDeCartao(),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            opcoes.forEach { opcao ->
                val ativa = opcao == selecionada
                Surface(
                    onClick = { aoEscolher(opcao) },
                    shape = RoundedCornerShape(11.dp),
                    color = if (ativa) cores.primary else cores.surfaceVariant,
                    modifier = Modifier.weight(1f).height(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = rotulo(opcao),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (ativa) cores.onPrimary else cores.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
