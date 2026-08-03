package com.jean.vocabs.ui.configuracoes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.shared.PreferenciaDeTema
import com.jean.vocabs.ui.components.CabecalhoDeDentro
import com.jean.vocabs.ui.components.CartaoDaTela
import com.jean.vocabs.ui.components.RotuloDeSecao
import com.jean.vocabs.ui.components.contornoDeCartao

/**
 * Tela 5b do handoff — "Configurações".
 *
 * Só tema por enquanto, e a tela existe assim mesmo: o segmentado precisa de um
 * lugar, e enfiá-lo na tela Você deixaria a faixa de idiomas dividindo espaço
 * com uma preferência que não tem nada a ver com idioma nenhum.
 */
@Composable
fun ConfiguracoesScreen(aoVoltar: () -> Unit, vm: ConfiguracoesViewModel = viewModel()) {
    val tema by vm.tema.collectAsStateWithLifecycle()

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
    }
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
