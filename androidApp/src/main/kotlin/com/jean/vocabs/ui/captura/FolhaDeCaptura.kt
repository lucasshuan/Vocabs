package com.jean.vocabs.ui.captura

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jean.vocabs.shared.domain.FormatoCaptura
import com.jean.vocabs.ui.components.BandeiraCircular
import com.jean.vocabs.ui.components.BotaoPrincipal
import com.jean.vocabs.ui.components.CartaoDaTela
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.RotuloDeSecao
import com.jean.vocabs.ui.components.coresDoFormato
import com.jean.vocabs.ui.components.formatarDuracao
import com.jean.vocabs.ui.components.iconeDoFormato
import com.jean.vocabs.ui.components.rotuloDoFormato
import com.jean.vocabs.ui.idiomas.idiomaDe
import kotlinx.coroutines.launch

/**
 * Telas 03/04 do handoff — o conteúdo da folha "Capturar".
 *
 * O idioma vem **antes** do modo, e o último usado já vem marcado: um toque no
 * caso comum, e a garantia de que nada chega a Pendentes sem idioma. É esta
 * ordem que permite o subtexto de cada linha da fila ser a língua e não o estado
 * de transcrição.
 *
 * O modo escolhido veste a própria categoria — fundo no tom claro, contorno e
 * rótulo na cor cheia, disco invertido. Assim a cor diz o tipo e o preenchimento
 * diz a escolha, e a mesma tripla reaparece em Pendentes sem precisar ser
 * reaprendida.
 */
@Composable
fun FolhaDeCaptura(
    cursos: List<String>,
    alvoInicial: String,
    formatoInicial: FormatoCaptura,
    gravando: Boolean,
    segundos: Long,
    aoContinuarComTexto: (alvo: String, trecho: String) -> Unit,
    aoGravar: (alvo: String) -> Unit,
    aoPararDeGravar: () -> Unit,
    aoFotografar: (alvo: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var alvo by remember(alvoInicial) { mutableStateOf(alvoInicial) }
    var formato by remember(formatoInicial) { mutableStateOf(formatoInicial) }
    var trecho by remember { mutableStateOf("") }
    val idioma = idiomaDe(alvo)

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 8.dp),
    ) {
        Text("Capturar", style = MaterialTheme.typography.headlineMedium)

        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            RotuloDeSecao("Guardar em")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                items(cursos.size, key = { cursos[it] }) { indice ->
                    val codigo = cursos[indice]
                    CartaoDeIdioma(
                        codigo = codigo,
                        selecionado = codigo == alvo,
                        aoClicar = { alvo = codigo },
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            RotuloDeSecao("Como")
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                FormatoCaptura.entries.forEach { item ->
                    CartaoDeFormato(
                        formato = item,
                        selecionado = formato == item,
                        aoClicar = { formato = item },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        when (formato) {
            FormatoCaptura.TEXTO -> BlocoDeTexto(
                trecho = trecho,
                aoMudar = { trecho = it },
                aoContinuar = { aoContinuarComTexto(alvo, trecho) },
            )
            FormatoCaptura.AUDIO -> BlocoDeAudio(
                idioma = idioma.nome.lowercase(),
                gravando = gravando,
                segundos = segundos,
                aoAlternar = { if (gravando) aoPararDeGravar() else aoGravar(alvo) },
            )
            FormatoCaptura.FOTO -> BlocoDeAcao(
                formato = FormatoCaptura.FOTO,
                titulo = "Fotografar um trecho",
                detalhe = "O texto é lido no aparelho, sem enviar a imagem para lugar nenhum.",
                acao = "Abrir câmera",
                aoClicar = { aoFotografar(alvo) },
            )
        }

        Spacer(Modifier.navigationBarsPadding().height(8.dp))
    }
}

@Composable
private fun CartaoDeIdioma(codigo: String, selecionado: Boolean, aoClicar: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    val fundo by animateColorAsState(
        targetValue = if (selecionado) cores.secondaryContainer else cores.surface,
        animationSpec = tween(160),
        label = "fundoDoIdioma",
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier
            .width(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(fundo)
            .border(
                width = if (selecionado) 1.5.dp else 1.dp,
                color = if (selecionado) cores.primary else cores.outline,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = aoClicar)
            .padding(horizontal = 6.dp, vertical = 11.dp),
    ) {
        BandeiraCircular(idiomaDe(codigo), tamanho = 28.dp)
        Text(
            text = idiomaDe(codigo).nome,
            style = MaterialTheme.typography.labelMedium,
            color = if (selecionado) cores.primary else cores.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun CartaoDeFormato(
    formato: FormatoCaptura,
    selecionado: Boolean,
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme
    val paleta = coresDoFormato(formato)
    val fundo by animateColorAsState(
        targetValue = if (selecionado) paleta.fundo else cores.surface,
        animationSpec = tween(180),
        label = "fundoDoFormato",
    )
    val disco by animateColorAsState(
        targetValue = if (selecionado) paleta.cor else paleta.fundo,
        animationSpec = tween(180),
        label = "discoDoFormato",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(fundo)
            .border(
                width = if (selecionado) 1.5.dp else 1.dp,
                color = if (selecionado) paleta.cor else cores.outline,
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(onClick = aoClicar)
            .padding(horizontal = 10.dp, vertical = 14.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(34.dp).background(disco, CircleShape)) {
            Icon(
                imageVector = iconeDoFormato(formato),
                contentDescription = null,
                tint = if (selecionado) paleta.fundo else paleta.cor,
                modifier = Modifier.size(19.dp),
            )
        }
        Text(
            text = rotuloDoFormato(formato),
            style = MaterialTheme.typography.titleSmall,
            color = if (selecionado) paleta.cor else cores.onSurface,
        )
    }
}

/**
 * Frases de exemplo pro campo de texto: um trecho plausível, não uma instrução.
 * "Digite ou cole o trecho" já está dito no rótulo da seção — repetir no
 * placeholder é a mesma informação duas vezes e não mostra o que a IA espera:
 * uma frase inteira, com uma expressão capturável dentro dela.
 */
private val ExemplosDeTrecho = listOf(
    "She rolled her eyes and told him to knock it off.",
    "The password was hidden inside a broken vending machine.",
    "He's been dragging his feet on this decision for weeks.",
    "Something about the hallway felt off the moment she stepped in.",
    "I can't believe you pulled that off without any backup.",
    "The recipe calls for a pinch of saffron and a splash of lemon.",
    "He shrugged it off like it was no big deal.",
    "The storm rolled in just as they reached the summit.",
)

@Composable
private fun BlocoDeTexto(trecho: String, aoMudar: (String) -> Unit, aoContinuar: () -> Unit) {
    val prancheta = LocalClipboard.current
    val escopo = rememberCoroutineScope()
    val exemplo = remember { ExemplosDeTrecho.random() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            RotuloDeSecao("Trecho onde apareceu", Modifier.weight(1f))
            BotaoColar {
                escopo.launch {
                    prancheta.getClipEntry()
                        ?.clipData
                        ?.takeIf { it.itemCount > 0 }
                        ?.getItemAt(0)
                        ?.text
                        ?.toString()
                        ?.takeIf { it.isNotBlank() }
                        ?.let(aoMudar)
                }
            }
        }
        OutlinedTextField(
            value = trecho,
            onValueChange = aoMudar,
            placeholder = { Text(exemplo) },
            minLines = 2,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "A frase inteira ajuda a IA a achar o sentido certo depois.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AnimatedVisibility(
            visible = trecho.isNotBlank(),
            enter = fadeIn(tween(160)) + expandVertically(tween(180)),
            exit = fadeOut(tween(120)) + shrinkVertically(tween(140)),
        ) {
            BotaoPrincipal(texto = "Continuar", aoClicar = aoContinuar, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun BotaoColar(aoClicar: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    Surface(
        onClick = aoClicar,
        shape = CircleShape,
        color = cores.secondaryContainer,
        modifier = Modifier.padding(start = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Icon(Icones.Colar, null, tint = cores.primary, modifier = Modifier.size(14.dp))
            Text("Colar", style = MaterialTheme.typography.labelMedium, color = cores.primary)
        }
    }
}

/**
 * O bloco de áudio: um alvo grande, e o lembrete do caminho de um gesto só.
 *
 * O disco é o mesmo verde do modo escolhido, e cresce quando grava — a folha
 * inteira já está vestida de menta, e um segundo sinal de cor não diria nada
 * além do que o tamanho diz.
 */
@Composable
private fun BlocoDeAudio(idioma: String, gravando: Boolean, segundos: Long, aoAlternar: () -> Unit) {
    BlocoDeAcao(
        formato = FormatoCaptura.AUDIO,
        titulo = if (gravando) "Gravando ${formatarDuracao(segundos)}" else "Toque para gravar",
        detalhe = "Segure o + na barra para gravar direto, sem passar por aqui.",
        acao = if (gravando) "Parar e guardar" else "Começar gravação",
        aoClicar = aoAlternar,
        ativo = gravando,
    )
}

@Composable
private fun BlocoDeAcao(
    formato: FormatoCaptura,
    titulo: String,
    detalhe: String,
    acao: String,
    aoClicar: () -> Unit,
    ativo: Boolean = false,
) {
    val paleta = coresDoFormato(formato)
    CartaoDaTela(recheio = androidx.compose.foundation.layout.PaddingValues(18.dp), modifier = Modifier.fillMaxWidth()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(if (ativo) 72.dp else 64.dp)
                .background(paleta.cor, CircleShape),
        ) {
            Icon(
                imageVector = if (ativo) Icones.Parar else iconeDoFormato(formato),
                contentDescription = null,
                tint = paleta.fundo,
                modifier = Modifier.size(26.dp),
            )
        }
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp),
        )
        Text(
            text = detalhe,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp),
        )
        BotaoPrincipal(texto = acao, aoClicar = aoClicar, modifier = Modifier.padding(top = 14.dp))
    }
}

