package com.jean.vocabs.ui.progresso

import com.jean.vocabs.ui.displayName
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.shared.domain.Steps
import com.jean.vocabs.shared.domain.DailyQuota
import com.jean.vocabs.shared.domain.CourseSummary
import com.jean.vocabs.ui.components.AcaoSecundaria
import com.jean.vocabs.ui.components.AnelDeProgresso
import com.jean.vocabs.ui.components.BandeiraCircular
import com.jean.vocabs.ui.components.BarraDeFaixas
import com.jean.vocabs.ui.components.CabecalhoDeDentro
import com.jean.vocabs.ui.components.CaixaTracejada
import com.jean.vocabs.ui.components.CartaoDaTela
import com.jean.vocabs.ui.components.DiaDaSemana
import com.jean.vocabs.ui.components.FaixaDaSemana
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.Movimento
import com.jean.vocabs.ui.components.contagemAnimada
import com.jean.vocabs.ui.components.contornoDeCartao
import com.jean.vocabs.ui.components.contornoTracejado
import com.jean.vocabs.ui.components.encolheAoTocar
import com.jean.vocabs.ui.components.fracaoAnimada
import com.jean.vocabs.ui.components.lembrarToque
import com.jean.vocabs.ui.languages.idiomaDe
import kotlinx.coroutines.launch

/**
 * Tela 08 do handoff — "Seu progresso", de um curso.
 *
 * Dois blocos, sempre os mesmos: a semana com a quota do dia e o estoque de
 * palavras. Cada um é a porta de uma tela mais funda, e os dois chevrons são as
 * únicas saídas. A tela não tem porcentagem de acerto, palavras por dia nem
 * melhor sequência — os dias marcados na semana são o único registro de
 * frequência que ela guarda.
 *
 * Sem palavra nenhuma no idioma a estrutura não muda: os mesmos dois cartões
 * ficam tracejados, com os rótulos no lugar e nenhum número inventado. Um
 * esqueleto mostra onde as coisas vão ficar; um "0 de 10" diria que já se
 * falhou em alguma coisa.
 *
 * A pastilha da bandeira é o único indicador de curso e também o botão de troca:
 * um toque abre a gaveta, escolher fecha e recarrega os dois cartões. Trocar
 * aqui **não** troca o curso aberto do app — quem só quis olhar o francês não
 * deve encontrar o `+` e a revisão mudados de idioma depois.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressoScreen(
    target: String?,
    aoVoltar: () -> Unit,
    aoAbrirDiaADia: (String) -> Unit,
    aoAbrirOQueFalta: (String) -> Unit,
    aoAdicionarIdioma: () -> Unit,
    vm: ProgressoViewModel = viewModel(),
) {
    LaunchedEffect(target) { vm.abrir(target) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val courses by vm.courses.collectAsStateWithLifecycle()
    val podeRemover by vm.podeRemover.collectAsStateWithLifecycle()
    val cores = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var confirmarRemocao by remember { mutableStateOf(false) }
    var gavetaAberta by remember { mutableStateOf(false) }
    val estadoDaGaveta = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    /** O curso olhado agora — o da rota, o escolhido na gaveta, ou o aberto. */
    val olhado = estado.languagePair.target
    val vazio = estado.total == 0

    fun fecharGaveta() {
        scope.launch { estadoDaGaveta.hide() }.invokeOnCompletion { gavetaAberta = false }
    }

    if (confirmarRemocao) {
        AlertDialog(
            onDismissRequest = { confirmarRemocao = false },
            title = { Text("Sair do ${idiomaDe(olhado).displayName.lowercase()}?") },
            text = { Text("As fichas continuam guardadas — o idioma volta com tudo se você matricular de novo.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarRemocao = false
                    vm.removerCurso(olhado)
                    aoVoltar()
                }) { Text("Remover", color = cores.error) }
            },
            dismissButton = { TextButton(onClick = { confirmarRemocao = false }) { Text("Manter") } },
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(13.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        CabecalhoDeDentro("Seu progresso", aoVoltar, Modifier.padding(top = 8.dp)) {
            PilulaDoCurso(target = olhado, aberta = gavetaAberta, aoClicar = { gavetaAberta = true })
        }

        CartaoDaSemana(estado = estado, vazio = vazio, aoAbrir = { aoAbrirDiaADia(olhado) })

        CartaoDoEstoque(estado = estado, vazio = vazio, aoAbrir = { aoAbrirOQueFalta(olhado) })

        if (podeRemover) {
            AcaoSecundaria(
                text = "Remover o ${idiomaDe(olhado).displayName.lowercase()} da faixa",
                aoClicar = { confirmarRemocao = true },
            )
        }

        Spacer(Modifier.navigationBarsPadding().height(110.dp))
    }

    if (gavetaAberta) {
        ModalBottomSheet(onDismissRequest = { gavetaAberta = false }, sheetState = estadoDaGaveta) {
            GavetaDeCursos(
                courses = courses,
                olhado = olhado,
                aoEscolher = {
                    vm.abrir(it)
                    fecharGaveta()
                },
                aoAdicionar = {
                    fecharGaveta()
                    aoAdicionarIdioma()
                },
            )
        }
    }
}

/**
 * O primeiro bloco: a semana e a quota de hoje, no mesmo cartão.
 *
 * Os dois estão juntos porque respondem a mesma pergunta em dois prazos —
 * "andei esta semana?" e "andei hoje?". Separá-los faria a quota parecer uma
 * meta à parte, e ela é só o dia de hoje da faixa logo acima.
 */
@Composable
private fun CartaoDaSemana(estado: ProgressoEstado, vazio: Boolean, aoAbrir: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    val conteudo: @Composable ColumnScope.() -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${estado.month} · esta semana",
                style = MaterialTheme.typography.labelMedium,
                color = if (vazio) cores.outline else cores.onSurfaceVariant,
            )
            if (!vazio) {
                Icon(
                    imageVector = Icones.Avancar,
                    contentDescription = null,
                    tint = cores.outline,
                    modifier = Modifier.size(16.dp).padding(start = 2.dp),
                )
            }
        }

        FaixaDaSemana(
            days = estado.semana.mapIndexed { indice, day ->
                DiaDaSemana(
                    sigla = SIGLAS_DA_SEMANA[indice],
                    numero = day.data.dayOfMonth,
                    reviews = day.reviews,
                    today = day.today,
                    futuro = day.futuro,
                )
            },
            tracejada = vazio,
            modifier = Modifier.padding(top = 13.dp),
        )

        Box(Modifier.fillMaxWidth().padding(vertical = 13.dp).height(1.dp).background(cores.outlineVariant))

        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Quota de hoje no ${idiomaDe(estado.languagePair.target).displayName.lowercase()}",
                style = MaterialTheme.typography.titleSmall,
                color = if (vazio) cores.onSurfaceVariant else cores.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = textoDaQuota(estado.quota),
                style = MaterialTheme.typography.bodySmall,
                color = if (vazio) cores.outline else cores.onSurfaceVariant,
            )
        }

        // Sem barra no vazio: uma trilha cinza de ponta a ponta é uma promessa de
        // que existe alguma coisa para preencher hoje, e não existe ainda.
        if (!vazio) {
            val avanco by fracaoAnimada(estado.quota.fracao, "fracaoDaQuota")
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(8.dp)
                    .background(cores.outlineVariant, RoundedCornerShape(4.dp)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(avanco)
                        .height(8.dp)
                        .background(cores.tertiary, RoundedCornerShape(4.dp)),
                )
            }
        }
    }

    if (vazio) {
        CaixaTracejada(
            modifier = Modifier.fillMaxWidth(),
            raio = 22.dp,
            recheio = PaddingValues(16.dp),
            conteudo = conteudo,
        )
    } else {
        CartaoDaTela(
            aoClicar = aoAbrir,
            recheio = PaddingValues(16.dp),
            modifier = Modifier.fillMaxWidth(),
            conteudo = conteudo,
        )
    }
}

/**
 * O segundo bloco: quanto do estoque já é seu.
 *
 * Vazio, ele não mostra "0 de 0" nem um anel zerado — mostra o lugar do anel e
 * diz quando ele começa a existir. A promessa é datada: quatro revisões certas,
 * que é literalmente a escada de [Degraus] do primeiro degrau ao último.
 */
@Composable
private fun CartaoDoEstoque(estado: ProgressoEstado, vazio: Boolean, aoAbrir: () -> Unit) {
    val cores = MaterialTheme.colorScheme

    if (vazio) {
        CaixaTracejada(modifier = Modifier.fillMaxWidth(), raio = 22.dp, recheio = PaddingValues(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.size(78.dp).contornoTracejado(cores.outline, raio = 39.dp, espessura = 2.dp))
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(
                        text = "Palavras que já são suas",
                        style = MaterialTheme.typography.titleSmall,
                        color = cores.onSurfaceVariant,
                    )
                    Text(
                        text = textoDeQuandoAparece(),
                        style = MaterialTheme.typography.bodySmall,
                        color = cores.outline,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
            LegendaDoEstoque(
                rotulos = listOf("dominadas", "familiares", "aprendendo"),
                cor = cores.outline,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
        return
    }

    CartaoDaTela(aoClicar = aoAbrir, recheio = PaddingValues(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            AnelDeProgresso(
                fracao = estado.mastered.toFloat() / estado.total,
                tamanho = 78.dp,
                espessura = 9.dp,
            ) {
                // Conta do zero junto com o arco: é a contagem de conquista
                // acumulada que `contagemAnimada` existe para servir.
                Text(
                    text = "${contagemAnimada(estado.mastered, "mastered")}",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "de ${estado.total}",
                    style = MaterialTheme.typography.labelSmall,
                    color = cores.onSurfaceVariant,
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(tituloDoEstoque(estado.mastered), style = MaterialTheme.typography.titleLarge)
                Text(
                    text = textoDoQuePertoDeVirar(estado.pertoDeVirar.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = cores.onSurfaceVariant,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
            Icon(Icones.Avancar, null, tint = cores.outline, modifier = Modifier.size(20.dp))
        }

        BarraDeFaixas(
            faixas = listOf(
                estado.mastered to cores.tertiary,
                estado.familiares to cores.tertiary.copy(alpha = 0.55f),
                estado.aprendendo to cores.outlineVariant,
            ),
            modifier = Modifier.padding(top = 14.dp),
        )

        LegendaDoEstoque(
            rotulos = listOf(
                "${estado.mastered} dominadas",
                "${estado.familiares} familiares",
                "${estado.aprendendo} aprendendo",
            ),
            cor = cores.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

/**
 * Os três nomes das faixas, espalhados na largura do cartão.
 *
 * Sem quadradinho de cor: a barra logo acima já está na mesma ordem, e um
 * marcador por rótulo repetiria o que ela diz melhor. No estado vazio os mesmos
 * três nomes ficam sozinhos, sem número — é o rótulo do lugar, não um placar.
 */
@Composable
private fun LegendaDoEstoque(rotulos: List<String>, cor: Color, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = modifier.fillMaxWidth()) {
        rotulos.forEach { rotulo ->
            Text(text = rotulo, style = MaterialTheme.typography.bodySmall, color = cor)
        }
    }
}

/**
 * A bandeira do curso no cabeçalho — e o botão que troca de curso.
 *
 * Sem ela seriam telas iguais e nenhum jeito de dizer qual é qual; sem o chevron
 * ela seria só um rótulo, e trocar de idioma exigiria voltar até a tela Você.
 * O chevron aponta para cima enquanto a gaveta está aberta, que é o que promete
 * que outro toque a fecha.
 */
@Composable
private fun PilulaDoCurso(target: String, aberta: Boolean, aoClicar: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    val language = idiomaDe(target)
    val toque = lembrarToque()
    val giro by animateFloatAsState(
        targetValue = if (aberta) 180f else 0f,
        animationSpec = tween(Movimento.PADRAO),
        label = "giroDaPilula",
    )

    Surface(
        onClick = aoClicar,
        shape = CircleShape,
        color = if (aberta) cores.secondaryContainer else cores.surface,
        border = if (aberta) BorderStroke(1.5.dp, cores.primary) else contornoDeCartao(),
        interactionSource = toque,
        modifier = Modifier.encolheAoTocar(toque, minimo = 0.94f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.padding(start = 5.dp, end = 9.dp, top = 5.dp, bottom = 5.dp),
        ) {
            BandeiraCircular(language, tamanho = 20.dp)
            Text(
                text = language.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = if (aberta) cores.primary else cores.onSurfaceVariant,
            )
            Icon(
                imageVector = Icones.Expandir,
                contentDescription = "Trocar idioma",
                tint = cores.primary,
                modifier = Modifier.size(16.dp).graphicsLayer { rotationZ = giro },
            )
        }
    }
}

/**
 * A gaveta da bandeira: os cursos matriculados, com o que cada um já rendeu.
 *
 * Cada linha traz o próprio "9 de 24" porque a pergunta que leva alguém a abrir
 * a gaveta é justamente comparar — e obrigar a entrar em cada idioma para
 * descobrir onde está o progresso seria pedir três viagens para uma resposta.
 */
@Composable
private fun GavetaDeCursos(
    courses: List<CourseSummary>,
    olhado: String,
    aoEscolher: (String) -> Unit,
    aoAdicionar: () -> Unit,
) {
    val cores = MaterialTheme.colorScheme
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Text(
            text = "Ver progresso em",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 5.dp),
        )

        courses.forEach { course ->
            LinhaDaGaveta(
                course = course,
                escolhido = course.languagePair.target == olhado,
                aoClicar = { aoEscolher(course.languagePair.target) },
            )
        }

        CaixaTracejada(
            modifier = Modifier.fillMaxWidth(),
            aoClicar = aoAdicionar,
            recheio = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(32.dp).background(cores.surfaceVariant, CircleShape),
                ) {
                    Icon(Icones.Mais, null, tint = cores.primary, modifier = Modifier.size(18.dp))
                }
                Text("Adicionar idioma", style = MaterialTheme.typography.titleSmall, color = cores.primary)
            }
        }

        Spacer(Modifier.navigationBarsPadding().height(14.dp))
    }
}

@Composable
private fun LinhaDaGaveta(course: CourseSummary, escolhido: Boolean, aoClicar: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    val language = idiomaDe(course.languagePair.target)
    val toque = lembrarToque()

    Surface(
        onClick = aoClicar,
        shape = RoundedCornerShape(18.dp),
        color = if (escolhido) cores.secondaryContainer else cores.surface,
        border = if (escolhido) BorderStroke(1.5.dp, cores.primary) else contornoDeCartao(),
        interactionSource = toque,
        modifier = Modifier.fillMaxWidth().encolheAoTocar(toque),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            BandeiraCircular(language, tamanho = 32.dp)
            Column(Modifier.weight(1f)) {
                Text(language.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = resumoDoCurso(course),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (escolhido) cores.primary else cores.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(22.dp)
                    .then(
                        if (escolhido) Modifier.background(cores.primary, CircleShape)
                        else Modifier.border(2.dp, cores.outline, CircleShape),
                    ),
            ) {
                if (escolhido) {
                    Icon(Icones.Check, null, tint = cores.onPrimary, modifier = Modifier.size(13.dp))
                }
            }
        }
    }
}

internal fun rotuloDeSequencia(days: Int): String =
    if (days == 1) "1 dia seguido" else "$days dias seguidos"

/**
 * O número da quota, à direita do rótulo: "6 de 10".
 *
 * Travessão quando o dia não pediu nada — seja porque não há palavra nenhuma,
 * seja porque nenhuma venceu hoje. "0 de 0" seria um placar de uma partida que
 * não houve.
 */
internal fun textoDaQuota(quota: DailyQuota): String =
    if (quota.total == 0) "—" else "${quota.done} de ${quota.total}"

/** A linha do anel vazio: quando é que ele passa a existir. */
internal fun textoDeQuandoAparece(): String =
    "aparece depois de ${NUMEROS_POR_EXTENSO[Steps.TOTAL - 1].lowercase()} revisões"

/** "9 de 24 já são suas" — ou a falta delas, sem número inventado. */
internal fun resumoDoCurso(course: CourseSummary): String =
    if (course.total == 0) "nenhuma palavra ainda" else "${course.mastered} de ${course.total} já são suas"

private val NUMEROS_POR_EXTENSO = listOf(
    "Nenhuma", "Uma", "Duas", "Três", "Quatro", "Cinco", "Seis", "Sete", "Oito", "Nove", "Dez",
)

/**
 * "Nove palavras já são suas".
 *
 * Por extenso até dez porque é assim que se lê em voz alta uma conquista; a
 * partir daí o algarismo volta, que é como se lê um número grande.
 */
internal fun tituloDoEstoque(mastered: Int): String = when {
    mastered == 0 -> "Nenhuma palavra é sua ainda"
    mastered == 1 -> "Uma palavra já é sua"
    mastered <= 10 -> "${NUMEROS_POR_EXTENSO[mastered]} palavras já são suas"
    else -> "$mastered palavras já são suas"
}

internal fun textoDoQuePertoDeVirar(quantas: Int): String = when (quantas) {
    0 -> "Nenhuma está perto de virar."
    1 -> "1 está perto de virar."
    else -> "$quantas estão perto de virar."
}
