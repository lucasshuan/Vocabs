package com.jean.vocabs.ui.idiomas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.contracts.Idioma
import com.jean.vocabs.ui.components.BandeiraCircular
import com.jean.vocabs.ui.components.BotaoPrincipal
import com.jean.vocabs.ui.components.CabecalhoDeDentro
import com.jean.vocabs.ui.components.CartaoDaTela
import com.jean.vocabs.ui.components.EstadoVazio
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.RotuloDeSecao
import com.jean.vocabs.ui.components.contornoDeCartao

/**
 * Tela 5c do handoff — "Novo idioma".
 *
 * Serve às duas escolhas de idioma, e não a uma só: matricular num curso novo e
 * trocar o idioma de partida são a mesma lista, a mesma busca e a mesma linha
 * selecionável. Duplicá-la para mudar o título e o verbo do botão seria manter
 * duas telas iguais que divergiriam no primeiro ajuste.
 */
@Composable
fun NovoIdiomaScreen(
    aoVoltar: () -> Unit,
    paraNativo: Boolean = false,
    vm: NovoIdiomaViewModel = viewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    var busca by remember { mutableStateOf("") }
    var escolhido by remember { mutableStateOf<Idioma?>(null) }

    val disponiveis = remember(estado, busca) { vm.disponiveis().buscar(busca) }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize().statusBarsPadding().imePadding().padding(horizontal = 20.dp),
    ) {
        CabecalhoDeDentro(
            titulo = if (paraNativo) "Qual é o seu idioma?" else "Qual idioma aprender?",
            aoVoltar = aoVoltar,
            modifier = Modifier.padding(top = 8.dp),
        )

        if (!paraNativo && estado.jaTem.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                RotuloDeSecao("Você já tem")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    estado.jaTem.forEach { idioma -> PilulaDeIdioma(idioma) }
                }
            }
        }

        CampoDeBusca(valor = busca, aoMudar = { busca = it })

        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            RotuloDeSecao("Escolha agora", Modifier.weight(1f))
            Text(
                text = if (disponiveis.size == 1) "1 idioma" else "${disponiveis.size} idiomas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }

        if (disponiveis.isEmpty()) {
            EstadoVazio(
                icone = Icones.Lupa,
                titulo = "Nada com esse nome",
                detalhe = "Tente outro trecho do nome do idioma.",
                modifier = Modifier.weight(1f),
            )
        } else {
            CartaoDaTela(recheio = PaddingValues(0.dp), modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn {
                    items(disponiveis, key = { it.codigo }) { idioma ->
                        LinhaDeIdioma(
                            idioma = idioma,
                            selecionado = idioma.codigo == escolhido?.codigo,
                            aoClicar = { escolhido = idioma },
                        )
                    }
                }
            }
        }

        BotaoPrincipal(
            texto = escolhido
                ?.let { if (paraNativo) "Usar ${it.nome.lowercase()}" else "Começar ${it.nome.lowercase()}" }
                ?: "Escolha um idioma",
            habilitado = escolhido != null,
            aoClicar = {
                escolhido?.let { idioma ->
                    if (paraNativo) vm.trocarNativo(idioma.codigo) else vm.matricular(idioma.codigo)
                    aoVoltar()
                }
            },
            modifier = Modifier.padding(bottom = 6.dp).navigationBarsPadding(),
        )
    }
}

@Composable
private fun PilulaDeIdioma(idioma: Idioma) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, border = contornoDeCartao()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.padding(start = 7.dp, end = 12.dp, top = 7.dp, bottom = 7.dp),
        ) {
            BandeiraCircular(idioma, tamanho = 20.dp)
            Text(
                text = idioma.nome,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LinhaDeIdioma(idioma: Idioma, selecionado: Boolean, aoClicar: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    Surface(
        onClick = aoClicar,
        color = if (selecionado) cores.secondaryContainer else cores.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
            ) {
                BandeiraCircular(idioma, tamanho = 30.dp)
                Text(idioma.nome, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                if (selecionado) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(22.dp).background(cores.primary, CircleShape),
                    ) {
                        Icon(Icones.Check, null, tint = cores.onPrimary, modifier = Modifier.size(14.dp))
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(cores.outlineVariant))
        }
    }
}

/**
 * O campo de busca.
 *
 * `BasicTextField` e não `OutlinedTextField`: o handoff pede um cartão de 14 de
 * raio com a lupa dentro, e o campo do Material traz consigo label flutuante,
 * contorno próprio e uma altura mínima que não cabem nesse desenho.
 */
@Composable
private fun CampoDeBusca(valor: String, aoMudar: (String) -> Unit) {
    val cores = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.small,
        color = cores.surface,
        border = contornoDeCartao(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier.padding(horizontal = 14.dp).height(48.dp),
        ) {
            Icon(Icones.Lupa, null, tint = cores.outline, modifier = Modifier.size(18.dp))
            Box(Modifier.weight(1f)) {
                if (valor.isEmpty()) {
                    Text("Buscar idioma", style = MaterialTheme.typography.bodyMedium, color = cores.outline)
                }
                BasicTextField(
                    value = valor,
                    onValueChange = aoMudar,
                    singleLine = true,
                    textStyle = LocalTextStyle.current.merge(
                        MaterialTheme.typography.bodyMedium.copy(color = cores.onSurface),
                    ),
                    cursorBrush = SolidColor(cores.primary),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
