package com.jean.vocabs.ui.selecionar

import com.jean.vocabs.ui.displayName
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.media.lembrarFoto
import com.jean.vocabs.media.lembrarPerfilDeOnda
import com.jean.vocabs.media.picoDaBarra
import com.jean.vocabs.media.rememberReprodutor
import com.jean.vocabs.shared.domain.AlvoSelecionado
import com.jean.vocabs.shared.domain.CaptureFormat
import com.jean.vocabs.shared.domain.CaptureStatus
import com.jean.vocabs.ui.components.AvisoDuplicata
import com.jean.vocabs.ui.components.BandeiraCircular
import com.jean.vocabs.ui.components.BotaoCircular
import com.jean.vocabs.ui.components.BotaoPrincipal
import com.jean.vocabs.ui.components.CartaoDaTela
import com.jean.vocabs.ui.components.ChipsDeSelecao
import com.jean.vocabs.ui.components.DiscoDeCategoria
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.RotuloDeSecao
import com.jean.vocabs.ui.components.SeletorDeTermos
import com.jean.vocabs.ui.components.contornoDeCartao
import com.jean.vocabs.ui.components.coresDoFormato
import com.jean.vocabs.ui.components.encolheAoTocar
import com.jean.vocabs.ui.components.formatarDuracaoMs
import com.jean.vocabs.ui.components.lembrarToque
import com.jean.vocabs.ui.components.rotuloDoFormato
import com.jean.vocabs.ui.components.tempoRelativo
import com.jean.vocabs.ui.idiomas.idiomaDe
import com.jean.vocabs.ui.theme.LocalTemaEscuro

/**
 * Telas 09/10 do handoff — "O que chamou atenção?".
 *
 * Uma tarefa só: marcar. Serve igual para texto colado, áudio transcrito e foto
 * lida, e é por isso que o nome não é "transcrever" — transcrever é o que a
 * máquina já tentou fazer antes de chegar aqui.
 *
 * A marcação não fica no texto: cada seleção confirmada limpa o trecho e vira
 * etiqueta na lista de baixo. É o que permite ao mesmo trecho render `fence` e
 * `on the fence` sem virar uma sopa de realces sobrepostos.
 *
 * O idioma do topo é o destino, e ainda é trocável aqui — a captura já existe,
 * mas nenhuma ficha nasceu neste par até o "Guardar".
 */
@Composable
fun SelecionarScreen(
    id: Long,
    aoVoltar: () -> Unit,
    aoGuardar: (List<Long>) -> Unit,
    vm: SelecionarViewModel = viewModel(),
) {
    val fluxo = remember(id) { vm.observar(id) }
    val captura by fluxo.collectAsStateWithLifecycle()
    val duplicata by vm.duplicata.collectAsStateWithLifecycle()
    val cursos by vm.cursos.collectAsStateWithLifecycle()
    var trecho by remember { mutableStateOf("") }
    val selecoes = remember { mutableStateListOf<AlvoSelecionado>() }
    var corrigindo by remember { mutableStateOf(false) }
    var confirmarExclusao by remember { mutableStateOf(false) }

    LaunchedEffect(captura?.id, captura?.trecho) {
        trecho = captura?.trecho.orEmpty()
        selecoes.clear()
        corrigindo = trecho.isBlank()
    }
    LaunchedEffect(selecoes.lastOrNull()?.texto, captura?.par?.alvo) {
        vm.procurarDuplicata(selecoes.lastOrNull()?.texto.orEmpty(), captura?.par?.alvo.orEmpty())
    }

    if (confirmarExclusao) {
        AlertDialog(
            onDismissRequest = { confirmarExclusao = false },
            title = { Text("Descartar captura?") },
            text = { Text("A mídia será removida porque ainda não há fichas ligadas a ela.") },
            confirmButton = {
                TextButton(onClick = { vm.excluir(id); confirmarExclusao = false; aoVoltar() }) {
                    Text("Descartar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmarExclusao = false }) { Text("Manter") } },
        )
    }

    val atual = captura

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 8.dp, end = 14.dp, top = 8.dp),
        ) {
            BotaoCircular(Icones.Voltar, "Voltar", aoVoltar, MaterialTheme.colorScheme.onSurface)
            Text(
                text = "O que chamou atenção?",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            atual?.let {
                SeletorDeIdioma(
                    alvo = it.par.alvo,
                    cursos = cursos,
                    aoEscolher = { codigo -> vm.trocarIdioma(id, codigo) },
                )
            }
        }

        if (atual == null) return@Column

        Column(
            verticalArrangement = Arrangement.spacedBy(13.dp),
            modifier = Modifier.padding(horizontal = 20.dp).padding(top = 12.dp),
        ) {
            when (atual.formato) {
                CaptureFormat.PHOTO -> atual.midiaCaminho?.let { PreviaFoto(it) }
                CaptureFormat.AUDIO -> atual.midiaCaminho?.let {
                    PlayerAudio(
                        caminho = it,
                        duracaoMs = atual.duracaoMs,
                        corrigindo = corrigindo,
                        aoCorrigir = { corrigindo = !corrigindo },
                    )
                }
                CaptureFormat.TEXT -> OrigemDoTexto(atual.criadoEm)
            }

            when {
                atual.status == CaptureStatus.TRANSCRIBING -> AvisoDeProcesso(
                    texto = "Transcrição local em andamento…",
                    comProgresso = true,
                )
                atual.erroTranscricao != null -> AvisoDeErro(atual.erroTranscricao.orEmpty())
            }

            if (corrigindo || trecho.isBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RotuloDeSecao("Transcrição")
                    OutlinedTextField(
                        value = trecho,
                        onValueChange = { trecho = it; selecoes.clear() },
                        placeholder = { Text("Digite o trecho manualmente") },
                        minLines = 2,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (trecho.isNotBlank()) {
                        BotaoPrincipal("Pronto, marcar termos", { corrigindo = false })
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SeletorDeTermos(
                        trecho = trecho,
                        aoSelecionar = { alvo ->
                            if (selecoes.none { it.inicio == alvo.inicio && it.fim == alvo.fim }) selecoes += alvo
                        },
                    )
                    Text(
                        text = "Toque ou arraste para marcar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            AnimatedVisibility(
                visible = selecoes.isNotEmpty(),
                enter = fadeIn(tween(160)) + expandVertically(tween(180)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(140)),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    RotuloDeSecao("${selecoes.size} ${if (selecoes.size == 1) "selecionada" else "selecionadas"}")
                    ChipsDeSelecao(selecoes, selecoes::remove)
                }
            }

            duplicata?.let { AvisoDuplicata(it) }

            BotaoPrincipal(
                texto = if (selecoes.isEmpty()) "Selecione o que guardar"
                else "Guardar ${selecoes.size} ${if (selecoes.size == 1) "captura" else "capturas"}",
                aoClicar = { vm.guardar(id, trecho, selecoes.toList(), aoGuardar) },
                habilitado = trecho.isNotBlank() && selecoes.isNotEmpty(),
                modifier = Modifier.padding(top = 4.dp),
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                BotaoDeDescartarCaptura { confirmarExclusao = true }
            }
        }
        Spacer(Modifier.navigationBarsPadding().height(24.dp))
    }
}

/**
 * "Descartar captura" — vermelho suave e com a lixeira.
 *
 * Ele era um `TextButton` em `outline`, que no claro é um lilás de 1 px de
 * contraste: a única saída para quem abriu a captura por engano estava escrita
 * na cor das bordas. Discrição de mais vira invisibilidade, e um botão que não se
 * acha não é secundário — é ausente.
 *
 * Agora ele é uma pílula de fundo avermelhado claro com o texto no vermelho de
 * erro: continua atrás da ação principal em peso — não tem a largura inteira, não
 * é preenchido de cor forte, não fica no caminho do polegar —, mas se acha de
 * relance e diz o que faz antes de ser lido, pela cor e pelo ícone. É a mesma
 * dupla (recipiente claro + tinta de erro) que o arrasto de exclusão em Pendentes
 * usa antes do limiar, e a repetição é de propósito: no app inteiro, este par
 * significa "isto apaga, e ainda dá para voltar atrás".
 *
 * O que ele abre continua sendo a confirmação — aqui não há desfazer, porque sair
 * da tela leva a captura junto.
 */
@Composable
private fun BotaoDeDescartarCaptura(aoClicar: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    // No escuro o `errorContainer` do Material é um vinho quase opaco, pesado
    // demais para uma ação de apoio. O vermelho claro do tema a 14% dá o mesmo
    // recado sobre o fundo escuro sem virar um bloco vermelho no rodapé.
    val fundo = if (LocalTemaEscuro.current) cores.error.copy(alpha = 0.14f) else cores.errorContainer
    val toque = lembrarToque()
    Surface(
        onClick = aoClicar,
        shape = CircleShape,
        color = fundo,
        contentColor = cores.error,
        interactionSource = toque,
        modifier = Modifier.encolheAoTocar(toque, minimo = 0.94f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
        ) {
            Icon(Icones.Lixeira, contentDescription = null, tint = cores.error, modifier = Modifier.size(18.dp))
            Text("Descartar captura", style = MaterialTheme.typography.labelLarge, color = cores.error)
        }
    }
}

/**
 * O chip de idioma do cabeçalho, que abre a lista de cursos.
 *
 * Ele é destino, e não etiqueta: quem gravou com o idioma errado marcado
 * conserta aqui, no último instante em que isso ainda é barato.
 */
@Composable
private fun SeletorDeIdioma(alvo: String, cursos: List<String>, aoEscolher: (String) -> Unit) {
    var aberto by remember { mutableStateOf(false) }
    val cores = MaterialTheme.colorScheme

    Box {
        Surface(
            onClick = { aberto = true },
            shape = CircleShape,
            color = cores.surface,
            border = contornoDeCartao(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(start = 5.dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
            ) {
                BandeiraCircular(idiomaDe(alvo), tamanho = 19.dp)
                Icon(Icones.Expandir, "Trocar idioma", tint = cores.onSurfaceVariant, modifier = Modifier.size(14.dp))
            }
        }
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            cursos.forEach { codigo ->
                DropdownMenuItem(
                    text = { Text(idiomaDe(codigo).displayName) },
                    leadingIcon = { BandeiraCircular(idiomaDe(codigo), tamanho = 20.dp) },
                    trailingIcon = {
                        if (codigo == alvo) Icon(Icones.Check, null, tint = cores.tertiary, modifier = Modifier.size(16.dp))
                    },
                    onClick = { aberto = false; if (codigo != alvo) aoEscolher(codigo) },
                )
            }
        }
    }
}

/** "Texto colado · agora" — de onde este trecho veio, na cor da categoria. */
@Composable
private fun OrigemDoTexto(criadoEm: Long) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DiscoDeCategoria(CaptureFormat.TEXT, tamanho = 22.dp)
        RotuloDeSecao("${rotuloDoFormato(CaptureFormat.TEXT)} colado · ${tempoRelativo(criadoEm)}")
    }
}

@Composable
private fun PreviaFoto(caminho: String) {
    val imagem by lembrarFoto(caminho)
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        imagem?.let { Image(it, "Foto capturada", contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth()) }
            ?: Box(Modifier.height(180.dp))
    }
}

/**
 * Play, onda e duração — e o "corrigir texto" ao lado.
 *
 * Sem sincronia palavra-a-áudio de propósito: ouvir o trecho de novo resolve, e
 * um destaque que acompanha a fala exigiria alinhamento por palavra que a
 * transcrição local não entrega.
 */
@Composable
private fun PlayerAudio(caminho: String, duracaoMs: Long?, corrigindo: Boolean, aoCorrigir: () -> Unit) {
    val player = rememberReprodutor(caminho)
    val paleta = coresDoFormato(CaptureFormat.AUDIO)
    CartaoDaTela(recheio = PaddingValues(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            Surface(onClick = player::alternar, shape = CircleShape, color = paleta.cor) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = if (player.tocando) Icones.Parar else Icones.Tocar,
                        contentDescription = if (player.tocando) "Parar" else "Ouvir",
                        tint = paleta.fundo,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            OndaDeAudio(
                caminho = caminho,
                cor = paleta.cor,
                progresso = player.progresso,
                modifier = Modifier.weight(1f),
            )
            Text(
                // Tocando, o número anda junto com a onda: quem está ouvindo quer
                // saber quanto falta, não quanto o arquivo tem.
                text = if (player.tocando) formatarDuracaoMs(player.posicaoMs)
                else duracaoMs?.let(::formatarDuracaoMs).orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
            RotuloDeSecao("Áudio · transcrito pela IA", Modifier.weight(1f))
            Text(
                text = if (corrigindo) "voltar" else "corrigir texto",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .then(Modifier)
                    .clickableSemRipple(aoCorrigir),
            )
        }
    }
}

@Composable
private fun AvisoDeProcesso(texto: String, comProgresso: Boolean) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
            if (comProgresso) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(texto, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 12.dp))
        }
    }
}

@Composable
private fun AvisoDeErro(texto: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(14.dp),
        )
    }
}

/** O que a barra mais baixa ainda ocupa, para o silêncio virar linha e não sumiço. */
private val ALTURA_MINIMA_DA_ONDA = 3.dp

/** Quanto a barra ainda não tocada se apaga em relação à já ouvida. */
private const val OPACIDADE_POR_TOCAR = 0.3f

/**
 * A onda desenhada, com barra de largura fixa em vez de dez barras esticadas.
 *
 * Repartir a largura disponível entre dez barras funciona na maquete de 340 px e
 * vira dez comprimidos deitados num celular de verdade: o que dá a leitura de
 * "áudio" é a barra fina repetida, não a contagem delas.
 *
 * O relevo vem do arquivo, e o preenchimento vem da agulha: o que já passou fica
 * na cor cheia e o que falta fica apagado. Enquanto o perfil não chegou — ou se o
 * arquivo não for legível — todas as barras ficam na altura mínima; uma onda
 * inventada diria sobre a gravação uma coisa que ninguém mediu.
 */
@Composable
private fun OndaDeAudio(
    caminho: String,
    cor: androidx.compose.ui.graphics.Color,
    progresso: Float,
    modifier: Modifier = Modifier,
) {
    val perfil by lembrarPerfilDeOnda(caminho)
    Canvas(modifier = modifier.height(26.dp)) {
        val largura = 3.dp.toPx()
        val passo = largura * 2
        val minima = ALTURA_MINIMA_DA_ONDA.toPx()
        val quantidade = (size.width / passo).toInt().coerceAtLeast(1)
        val agulha = size.width * progresso
        repeat(quantidade) { indice ->
            val altura = minima + (size.height - minima) * perfil.picoDaBarra(indice, quantidade)
            val x = indice * passo
            drawRoundRect(
                color = cor,
                topLeft = Offset(x, (size.height - altura) / 2f),
                size = Size(largura, altura),
                cornerRadius = CornerRadius(largura / 2f),
                alpha = if (x + largura <= agulha) 1f else OPACIDADE_POR_TOCAR,
            )
        }
    }
}

/** Texto que age sem virar botão: "corrigir texto" é um atalho, não uma ação da tela. */
@Composable
private fun Modifier.clickableSemRipple(aoClicar: () -> Unit): Modifier {
    val interacao = remember { MutableInteractionSource() }
    return clickable(interactionSource = interacao, indication = null, onClick = aoClicar)
}
