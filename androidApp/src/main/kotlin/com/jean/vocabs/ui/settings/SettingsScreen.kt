package com.jean.vocabs.ui.settings

import com.jean.vocabs.ui.displayName
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.BuildConfig
import com.jean.vocabs.R
import com.jean.vocabs.shared.ThemePreference
import com.jean.vocabs.ui.components.CircularFlag
import com.jean.vocabs.ui.components.InnerHeader
import com.jean.vocabs.ui.components.ScreenCard
import com.jean.vocabs.ui.components.RowChevron
import com.jean.vocabs.ui.components.IconDisc
import com.jean.vocabs.ui.components.AppIcons
import com.jean.vocabs.ui.components.ListRow
import com.jean.vocabs.ui.components.Motion
import com.jean.vocabs.ui.components.SectionLabel
import com.jean.vocabs.ui.components.cardOutline
import com.jean.vocabs.ui.components.entradaSuave
import com.jean.vocabs.ui.components.rememberHaptics
import com.jean.vocabs.ui.languages.languageOf
import java.io.File
import kotlin.math.roundToInt

/**
 * Tela 5b do handoff — "Configurações".
 *
 * Reúne o que vale para o app inteiro e não para um curso: o idioma em que ele
 * fala com você, o tema e a portabilidade dos dados. É a diferença que separa
 * esta tela da Você — lá tudo é por curso e muda conforme o que se estuda; aqui
 * nada depende de qual idioma está aberto.
 *
 * Por isso "Meu idioma" mora aqui e não na Você: o idioma-base é o único ajuste
 * de idioma que não é sobre *um* curso, e ao lado da lista de progresso ele
 * parecia mais uma linha da lista — a única que, em vez de abrir um curso,
 * mudava a interface inteira.
 *
 * A tela é uma pilha de seções, cada uma com rótulo, ícone e um traço separando
 * da seguinte. Com quatro assuntos numa coluna só, o rótulo sozinho não bastava:
 * "Dados" e "Sobre" ficavam a 15 dp um do outro e liam-se como uma lista de seis
 * linhas.
 */
@Composable
fun SettingsScreen(
    aoVoltar: () -> Unit,
    aoTrocarIdiomaNativo: () -> Unit,
    vm: SettingsViewModel = viewModel(),
) {
    val theme by vm.theme.collectAsStateWithLifecycle()
    val native by vm.native.collectAsStateWithLifecycle()
    val exportando by vm.exportando.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val language = languageOf(native)

    val importarArquivo = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            Toast.makeText(contexto, "Importação ainda não disponível — em breve.", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        InnerHeader("Configurações", aoVoltar, Modifier.padding(top = 8.dp))

        // As seções chegam escalonadas, de cima para baixo. É a mesma entrada das
        // outras telas de dentro, e aqui ela faz um trabalho a mais: dá ordem de
        // leitura a quatro blocos que, parados, têm todos o mesmo peso.
        Section(icon = AppIcons.Globo, title = "Language", indice = 0) {
            ListRow(
                aoClicar = aoTrocarIdiomaNativo,
                start = { NativeFlag(native) },
                end = { SwitchPill() },
            ) {
                Text(
                    text = "Meu idioma",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // O nome troca com um fundido: a tela de escolha fecha por cima
                // desta, e sem a transição a única coisa que confirma a troca é um
                // texto que já estava no lugar quando a tela reapareceu.
                AnimatedContent(
                    targetState = language.displayName,
                    transitionSpec = {
                        (fadeIn(tween(Motion.PADRAO)) + scaleIn(tween(Motion.PADRAO), initialScale = 0.92f))
                            .togetherWith(fadeOut(tween(Motion.RAPIDO)))
                    },
                    label = "nomeDoNativo",
                ) { name ->
                    Text(name, style = MaterialTheme.typography.titleSmall)
                }
            }
            SectionNote("As traduções e explicações das fichas saem neste idioma.")
        }

        Divider()

        Section(icon = themeIcon(theme), title = "Aparência", indice = 1) {
            ScreenCard(recheio = PaddingValues(16.dp), modifier = Modifier.fillMaxWidth()) {
                Text("Tema", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Auto segue o aparelho.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
                ThemeSegmented(
                    selecionada = theme,
                    aoEscolher = vm::escolherTema,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        Divider()

        Section(icon = AppIcons.Exportar, title = "Dados", indice = 2) {
            val cores = MaterialTheme.colorScheme
            ListRow(
                aoClicar = {
                    vm.exportar(
                        aoPronto = { file -> share(contexto, file) },
                        aoErro = { Toast.makeText(contexto, it, Toast.LENGTH_LONG).show() },
                    )
                },
                start = { IconDisc(AppIcons.Exportar, null, cor = cores.primary, fundo = cores.primaryContainer) },
                end = {
                    // O indicador entra no lugar da chevron em vez de ao lado
                    // dela: enquanto o ZIP é montado a linha não abre nada, e uma
                    // seta de "isto leva a algum lugar" ali seria uma promessa
                    // falsa por alguns segundos.
                    AnimatedContent(
                        targetState = exportando,
                        transitionSpec = { fadeIn(tween(Motion.RAPIDO)).togetherWith(fadeOut(tween(Motion.RAPIDO))) },
                        label = "fimDaExportacao",
                    ) { emCurso ->
                        if (emCurso) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else RowChevron()
                    }
                },
            ) {
                Text("Exportar meus dados", style = MaterialTheme.typography.titleSmall)
                SwappingDetail(
                    if (exportando) "preparando ZIP…" else "um arquivo com as fichas e as mídias",
                )
            }

            ListRow(
                aoClicar = { importarArquivo.launch(arrayOf("application/zip")) },
                start = { IconDisc(AppIcons.Importar, null, cor = cores.tertiary, fundo = cores.tertiaryContainer) },
                end = { RowChevron() },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Importar meus dados", style = MaterialTheme.typography.titleSmall)
                    ComingSoonBadge(Modifier.padding(start = 8.dp))
                }
                SwappingDetail("de um ZIP exportado pela Vocabu")
            }
        }

        Divider()

        Section(icon = AppIcons.Informacao, title = "Sobre", indice = 3) {
            Signature()
        }

        Spacer(Modifier.navigationBarsPadding().height(110.dp))
    }
}

/**
 * Um bloco da tela: rótulo com ícone, conteúdo, e a entrada escalonada.
 *
 * O ícone é do tamanho do próprio rótulo e na mesma cor — ele marca onde a seção
 * começa quando a rolagem corta o traço de cima, e não deve competir com os
 * discos coloridos das linhas logo abaixo.
 */
@Composable
private fun Section(
    icon: ImageVector,
    title: String,
    indice: Int,
    conteudo: @Composable () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.entradaSuave(indice).fillMaxWidth().padding(top = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Três dos quatro ícones nunca mudam, e para eles isto não anima
            // nada. O de Aparência é o que se move: ele acompanha a escolha —
            // sol, lua, meio disco — e o estouro da mola é a confirmação que
            // sobra para quem tocou em "Escuro" com o dedo em cima do segmentado
            // e não viu a pastilha deslizar por baixo dele.
            AnimatedContent(
                targetState = icon,
                transitionSpec = {
                    (scaleIn(Motion.mola(), initialScale = 0.4f) + fadeIn(tween(Motion.RAPIDO)))
                        .togetherWith(scaleOut(tween(Motion.RAPIDO), targetScale = 0.4f) + fadeOut(tween(Motion.RAPIDO)))
                },
                label = "iconeDaSecao",
            ) { desenho ->
                Icon(
                    imageVector = desenho,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp),
                )
            }
            SectionLabel(title, Modifier.padding(start = 7.dp))
        }
        conteudo()
    }
}

/** O traço entre duas seções, com ar dos dois lados. */
@Composable
private fun Divider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(vertical = 6.dp),
    )
}

/** A frase de rodapé de uma seção — o porquê da linha acima, em cinza-lilás. */
@Composable
private fun SectionNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
    )
}

/**
 * A linha de apoio de uma [LinhaDeLista], trocando de texto por fundido.
 *
 * "um arquivo com as fichas e as mídias" vira "preparando ZIP…" no toque, e o
 * corte seco entre as duas frases se lê como um erro de renderização — é o mesmo
 * texto, na mesma posição, com outro conteúdo.
 */
@Composable
private fun SwappingDetail(text: String) {
    AnimatedContent(
        targetState = text,
        transitionSpec = { fadeIn(tween(Motion.PADRAO)).togetherWith(fadeOut(tween(Motion.RAPIDO))) },
        label = "detalheDaLinha",
    ) { current ->
        Text(
            text = current,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/**
 * A bandeira do idioma-base, com anel.
 *
 * O anel existe porque metade das bandeiras do catálogo tem branco na borda —
 * sobre a superfície branca do cartão claro, o disco perderia o contorno de um
 * lado e pareceria recortado.
 */
@Composable
private fun NativeFlag(codigo: String) {
    val cores = MaterialTheme.colorScheme
    AnimatedContent(
        targetState = codigo,
        transitionSpec = {
            (scaleIn(Motion.molaElastica(), initialScale = 0.5f) + fadeIn(tween(Motion.PADRAO)))
                .togetherWith(scaleOut(tween(Motion.RAPIDO), targetScale = 0.5f) + fadeOut(tween(Motion.RAPIDO)))
        },
        label = "bandeiraDoNativo",
    ) { current ->
        CircularFlag(
            language = languageOf(current),
            tamanho = 34.dp,
            modifier = Modifier.border(1.dp, cores.outline, CircleShape),
        )
    }
}

/** O selo de "existe, mas ainda não" — cinza, minúsculo, sem prometer data. */
@Composable
private fun ComingSoonBadge(modifier: Modifier = Modifier) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = modifier) {
        Text(
            text = "em breve",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/** A pílula "trocar >" da linha do idioma-base. */
@Composable
private fun SwitchPill() {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 11.dp, end = 7.dp, top = 7.dp, bottom = 7.dp),
        ) {
            Text(
                text = "trocar",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                imageVector = AppIcons.Avancar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/**
 * O rodapé da tela: a marca e a versão instalada.
 *
 * Alinhado à esquerda, como as linhas de Dados, e não centralizado. É a última
 * coisa da coluna, e o que fica no fim desta tela cai debaixo do `+` de captura,
 * que flutua no centro da base: uma assinatura centralizada some atrás dele
 * justamente quando a rolagem chega ao fim.
 */
@Composable
private fun Signature() {
    ListRow(
        start = { Image(painterResource(R.drawable.logo_vocabu), null, Modifier.size(34.dp)) },
    ) {
        Text("Vocabu", style = MaterialTheme.typography.titleSmall)
        Text(
            text = "versão ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

private fun share(contexto: android.content.Context, file: File) {
    val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    contexto.startActivity(Intent.createChooser(intent, "Exportar dados da Vocabu"))
}

private fun themeLabel(theme: ThemePreference): String = when (theme) {
    ThemePreference.LIGHT -> "Claro"
    ThemePreference.DARK -> "Escuro"
    ThemePreference.SYSTEM -> "Auto"
}

private fun themeIcon(theme: ThemePreference): ImageVector = when (theme) {
    ThemePreference.LIGHT -> AppIcons.Sol
    ThemePreference.DARK -> AppIcons.Lua
    ThemePreference.SYSTEM -> AppIcons.MeioDisco
}

/**
 * O segmentado de tema: três partes iguais e uma pastilha que desliza entre elas.
 *
 * Partes iguais e não larguras conforme o texto — com "Claro", "Escuro" e "Auto"
 * o resultado seria três alvos de tamanhos diferentes para escolhas do mesmo
 * peso, e o menor deles ficaria abaixo do mínimo de toque.
 *
 * A pastilha é **uma só**, desenhada atrás dos três rótulos e movida por mola.
 * Antes cada opção pintava o próprio fundo, e a troca era um corte seco em dois
 * lugares ao mesmo tempo: uma apagava, outra acendia, e nada dizia que era a
 * mesma seleção mudando de lugar. Deslizando, o gesto tem origem e destino.
 */
@Composable
private fun ThemeSegmented(
    selecionada: ThemePreference,
    aoEscolher: (ThemePreference) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = MaterialTheme.colorScheme
    val opcoes = ThemePreference.entries
    val forma = RoundedCornerShape(11.dp)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = cores.surfaceVariant,
        border = cardOutline(),
        modifier = modifier.fillMaxWidth(),
    ) {
        BoxWithConstraints(Modifier.padding(4.dp)) {
            val largura = maxWidth / opcoes.size
            val destino by animateFloatAsState(
                targetValue = opcoes.indexOf(selecionada).toFloat(),
                animationSpec = Motion.mola(),
                label = "deslizeDoTema",
            )

            // `offset` com lambda: o valor é lido na fase de posicionamento, então
            // a pastilha atravessa a linha sem recompor nem remedir ninguém.
            Box(
                Modifier
                    .offset { IntOffset((destino * largura.toPx()).roundToInt(), 0) }
                    .width(largura)
                    .height(ALTURA_DO_SEGMENTO)
                    .background(cores.primary, forma),
            )

            Row(Modifier.fillMaxWidth()) {
                opcoes.forEach { opcao ->
                    val ativa = opcao == selecionada
                    val tinta by animateColorAsState(
                        targetValue = if (ativa) cores.onPrimary else cores.onSurfaceVariant,
                        animationSpec = tween(Motion.PADRAO),
                        label = "tintaDoSegmento",
                    )
                    val toque = rememberHaptics()
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .width(largura)
                            .height(ALTURA_DO_SEGMENTO)
                            .clip(forma)
                            .clickable(
                                interactionSource = toque,
                                indication = ripple(),
                                onClick = { aoEscolher(opcao) },
                            ),
                    ) {
                        Icon(
                            imageVector = themeIcon(opcao),
                            contentDescription = null,
                            tint = tinta,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = themeLabel(opcao),
                            style = MaterialTheme.typography.labelLarge,
                            color = tinta,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

/** 44 dp: o mínimo de toque, e a altura da pastilha e dos três alvos. */
private val ALTURA_DO_SEGMENTO = 44.dp
