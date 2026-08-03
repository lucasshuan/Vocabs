package com.jean.vocabs.ui.pendentes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.shared.domain.Captura
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.shared.domain.FormatoCaptura
import com.jean.vocabs.shared.domain.StatusCaptura
import com.jean.vocabs.shared.domain.StatusEntrada
import com.jean.vocabs.ui.components.CartaoDaTela
import com.jean.vocabs.ui.components.DiscoDeIcone
import com.jean.vocabs.ui.components.EstadoVazio
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.LinhaDeLista
import com.jean.vocabs.ui.components.Pilula
import com.jean.vocabs.ui.components.RotuloDeSecao
import com.jean.vocabs.ui.components.tempoRelativo
import com.jean.vocabs.ui.components.tituloDaCaptura

@Composable
fun PendentesScreen(
    aoAbrirCaptura: (Captura) -> Unit,
    aoAbrirFicha: (Entrada) -> Unit,
    vm: PendentesViewModel = viewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val capturaMaisAntiga = estado.capturas.minOfOrNull(Captura::criadoEm)
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(9.dp),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
    ) {
        item {
            Text("Pendentes", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 22.dp))
            Text(
                when {
                    estado.capturas.isNotEmpty() -> buildString {
                        append(estado.capturas.size)
                        append(if (estado.capturas.size == 1) " captura crua" else " capturas cruas")
                        capturaMaisAntiga?.let { append(" · a mais antiga ${tempoRelativo(it)}") }
                    }
                    estado.fichas.isNotEmpty() ->
                        "${estado.fichas.size} ${if (estado.fichas.size == 1) "ficha em processamento" else "fichas em processamento"}"
                    else -> "Suas capturas e fichas em processamento aparecem aqui."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp, bottom = 12.dp),
            )
        }

        if (estado.total == 0) {
            item {
                EstadoVazio(
                    icone = Icones.Check,
                    titulo = "Tudo em dia",
                    detalhe = "Nenhuma captura ou ficha esperando.",
                )
            }
        }

        items(estado.capturas, key = { "c${it.id}" }) { captura ->
            LinhaDeLista(
                titulo = tituloDaCaptura(captura),
                detalhe = detalheDaCaptura(captura),
                aoClicar = { aoAbrirCaptura(captura) },
                inicio = {
                    val foto = captura.formato == FormatoCaptura.FOTO
                    DiscoDeIcone(
                        icone = if (foto) Icones.Camera else Icones.Microfone,
                        descricao = if (foto) "Foto" else "Áudio",
                        cor = if (foto) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        fundo = if (foto) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                    )
                },
                fim = {
                    Text(
                        text = tempoRelativo(captura.criadoEm),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }

        if (estado.fichas.isNotEmpty()) {
            item { RotuloDeSecao("Fichas sendo geradas", Modifier.padding(top = 10.dp)) }
            items(estado.fichas, key = { "e${it.id}" }) { entrada ->
                CartaoEntrada(entrada, { aoAbrirFicha(entrada) }, { vm.tentarDeNovo(entrada.id) })
            }
        }
    }
}

private fun detalheDaCaptura(captura: Captura): String = when {
    captura.status == StatusCaptura.TRANSCREVENDO -> "transcrevendo no aparelho…"
    captura.erroTranscricao != null -> "toque para transcrever manualmente"
    captura.formato == FormatoCaptura.FOTO -> "toque para transcrever a foto"
    captura.formato == FormatoCaptura.AUDIO -> "toque para ouvir e transcrever"
    else -> "toque para selecionar os termos"
}

@Composable
private fun CartaoEntrada(entrada: Entrada, aoClicar: () -> Unit, tentar: () -> Unit) {
    CartaoDaTela(
        forma = MaterialTheme.shapes.medium,
        recheio = PaddingValues(horizontal = 15.dp, vertical = 14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.layout.Column(Modifier.weight(1f).clickable(onClick = aoClicar)) {
                Text(entrada.titulo, style = MaterialTheme.typography.titleSmall)
                Text(
                    when (entrada.status) {
                        StatusEntrada.PENDENTE -> "na fila"
                        StatusEntrada.GERANDO -> "gerando ficha…"
                        StatusEntrada.ERRO -> entrada.erro ?: "falha na geração"
                        StatusEntrada.PRONTA -> "ficha pronta"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (entrada.status == StatusEntrada.ERRO) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (entrada.status == StatusEntrada.GERANDO) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            if (entrada.status == StatusEntrada.ERRO) Pilula("tentar de novo", destaque = true, aoClicar = tentar)
        }
    }
}
