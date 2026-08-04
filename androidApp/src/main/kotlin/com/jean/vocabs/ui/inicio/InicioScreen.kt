package com.jean.vocabs.ui.inicio

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.R
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.ui.components.AnelDeProgresso
import com.jean.vocabs.ui.components.BotaoPrincipal
import com.jean.vocabs.ui.components.CaixaTracejada
import com.jean.vocabs.ui.components.CartaoDaTela
import com.jean.vocabs.ui.components.DiscoDeIcone
import com.jean.vocabs.ui.components.FaixaDeIdiomas
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.PontosDePagina
import com.jean.vocabs.ui.components.RotuloDeSecao
import com.jean.vocabs.ui.components.contagemAnimada
import com.jean.vocabs.ui.components.entradaSuave
import com.jean.vocabs.ui.components.tempoAte
import com.jean.vocabs.ui.idiomas.idiomaDe

/**
 * Tela 01/02 do handoff — a Início, uma página por curso.
 *
 * É a **única** aba recortada por idioma, e a troca é um deslize. As outras três
 * mostram sempre tudo: um filtro que continuasse ligado ao mudar de aba faria
 * palavras sumirem sem que ninguém tivesse pedido.
 *
 * Deslizar não é só navegar — é trocar o curso aberto. Por isso o botão de
 * revisar e a folha do `+` seguem a página visível sem que nenhum dos dois
 * precise saber que existe um carrossel.
 */
@Composable
fun InicioScreen(
    aoCapturar: () -> Unit,
    aoRevisar: () -> Unit,
    aoAbrirPerfil: () -> Unit,
    aoAdicionarIdioma: () -> Unit,
    vm: InicioViewModel = viewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val paginas = estado.paginas
    val pager = rememberPagerState(pageCount = { paginas.size })

    // Duas direções, e a ordem entre elas importa. O pager nasce na página 0, que
    // quase nunca é o curso aberto — deixá-lo mandar antes de estar posicionado
    // faria abrir o app no inglês trocar o curso para o inglês, em silêncio.
    // Por isso ele só passa a mandar depois do primeiro posicionamento.
    var posicionado by remember { mutableStateOf(false) }

    LaunchedEffect(paginas.size, estado.ativo) {
        if (paginas.isEmpty()) return@LaunchedEffect
        if (!posicionado) {
            pager.scrollToPage(estado.indiceAtivo)
            posicionado = true
        } else if (estado.indiceAtivo != pager.currentPage && !pager.isScrollInProgress) {
            pager.animateScrollToPage(estado.indiceAtivo)
        }
    }

    LaunchedEffect(posicionado) {
        if (!posicionado) return@LaunchedEffect
        snapshotFlow { pager.settledPage }.collect { indice ->
            paginas.getOrNull(indice)?.let { vm.abrirCurso(it.par.alvo) }
        }
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 14.dp),
        ) {
            Image(painterResource(R.drawable.logo_tagarara), stringResource(R.string.logo_description), Modifier.size(34.dp))
            Text("TAGARARA", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 9.dp))
            Spacer(Modifier.weight(1f))
            Surface(onClick = aoAbrirPerfil, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icones.Pessoa,
                        contentDescription = "Você",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        if (estado.temCarrossel) {
            FaixaDeIdiomas(
                cursos = estado.cursos,
                ativo = estado.ativo,
                aoEscolher = vm::abrirCurso,
                aoAdicionar = aoAdicionarIdioma,
                modifier = Modifier.padding(bottom = 14.dp),
            )
        }

        HorizontalPager(
            state = pager,
            beyondViewportPageCount = 1,
            modifier = Modifier.weight(1f),
        ) { indice ->
            paginas.getOrNull(indice)?.let { pagina ->
                PaginaDeCurso(
                    pagina = pagina,
                    aoRevisar = aoRevisar,
                    aoCapturar = aoCapturar,
                )
            }
        }

        // Fora do pager, de propósito: aqui os pontos não pertencem a nenhuma
        // página nem a nenhum cartão, e ficam no mesmo lugar não importa o que
        // muda acima.
        PontosDePagina(
            total = paginas.size,
            atual = pager.currentPage,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 10.dp),
        )
        Spacer(Modifier.navigationBarsPadding().height(ESPACO_DA_BARRA))
    }
}

@Composable
private fun PaginaDeCurso(
    pagina: PaginaDoInicio,
    aoRevisar: () -> Unit,
    aoCapturar: () -> Unit,
) {
    val idioma = idiomaDe(pagina.par.alvo)
    val resumo = pagina.resumo

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        CartaoDaTela(
            forma = MaterialTheme.shapes.extraLarge,
            recheio = PaddingValues(18.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnelDoCurso(pagina)
                Column(Modifier.weight(1f).padding(start = 15.dp)) {
                    Text("Seu ${idioma.nome.lowercase()}", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = detalheDoCurso(resumo.total, resumo.dominadas, resumo.naFila),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            if (resumo.naFila > 0) {
                BotaoPrincipal(
                    texto = "Revisar ${resumo.naFila} ${if (resumo.naFila == 1) "palavra" else "palavras"}",
                    aoClicar = aoRevisar,
                    modifier = Modifier.padding(top = 15.dp),
                )
            } else {
                LinhaDoQueVem(pagina, Modifier.padding(top = 15.dp))
            }
        }

        // As capturas do dia entram escalonadas, de cima para baixo. É a lista
        // que cresce enquanto a pessoa usa o app, e é o único lugar do Início
        // onde ela vê o próprio dia se acumulando — chegar montada faz três
        // capturas parecerem um histórico velho em vez do que aconteceu hoje.
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            RotuloDeSecao("Capturadas hoje em ${idioma.nome.lowercase()}")
            if (pagina.capturadasHoje.isEmpty()) {
                ConviteDeCaptura(idioma.nome.lowercase(), aoCapturar)
            } else {
                pagina.capturadasHoje.forEachIndexed { indice, entrada ->
                    LinhaDeCapturada(entrada, Modifier.entradaSuave(indice))
                }
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}

/**
 * O anel do curso: a força média por dentro, ou o tique quando não há fila.
 *
 * Curso em dia repete no anel o mesmo tique que a bandeira mostra na faixa — é a
 * confirmação de que o selo lá em cima não estava falando de outra coisa.
 */
@Composable
private fun AnelDoCurso(pagina: PaginaDoInicio) {
    val cores = MaterialTheme.colorScheme
    val emDia = pagina.resumo.naFila == 0 && pagina.resumo.total > 0
    AnelDeProgresso(
        fracao = if (emDia) 1f else pagina.forcaMedia / 100f,
        tamanho = 70.dp,
        espessura = 8.dp,
    ) {
        if (emDia) {
            Icon(Icones.Check, null, tint = cores.tertiary, modifier = Modifier.size(26.dp))
        } else {
            // A porcentagem sobe no mesmo tempo em que o arco corre: os dois são
            // a mesma medida, e vê-los chegarem juntos é o que impede o anel de
            // parecer decoração ao redor de um número.
            Text("${contagemAnimada(pagina.forcaMedia, "forcaMedia")}%", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

/** "Próximas 5 em 19h · nada a fazer hoje" — o cartão de um curso sem fila. */
@Composable
private fun LinhaDoQueVem(pagina: PaginaDoInicio, modifier: Modifier = Modifier) {
    val cores = MaterialTheme.colorScheme
    val proxima = pagina.resumo.proximaEmMillis
    CartaoDaTela(
        forma = MaterialTheme.shapes.medium,
        cor = cores.surfaceVariant,
        recheio = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DiscoDeIcone(
                icone = if (proxima == null) Icones.Mais else Icones.Relogio,
                descricao = null,
                cor = cores.onSurfaceVariant,
                fundo = cores.surface,
                tamanho = 34.dp,
            )
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    text = when {
                        proxima == null -> "Nada agendado ainda"
                        pagina.proximasEm24h > 1 -> "Próximas ${pagina.proximasEm24h} ${tempoAte(proxima)}"
                        else -> "Próxima ${tempoAte(proxima)}"
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = if (proxima == null) "capture algo para começar" else "nada a fazer hoje",
                    style = MaterialTheme.typography.bodySmall,
                    color = cores.onSurfaceVariant,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
    }
}

@Composable
private fun LinhaDeCapturada(entrada: Entrada, modifier: Modifier = Modifier) {
    CartaoDaTela(
        forma = MaterialTheme.shapes.small,
        recheio = PaddingValues(horizontal = 15.dp, vertical = 11.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(entrada.titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                text = entrada.ficha?.traducao.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Dia sem captura vira convite, e não tela vazia — o custo de capturar é o ponto do app. */
@Composable
private fun ConviteDeCaptura(idioma: String, aoClicar: () -> Unit) {
    CaixaTracejada(
        modifier = Modifier.fillMaxWidth(),
        recheio = PaddingValues(18.dp),
        aoClicar = aoClicar,
    ) {
        Text(
            text = "Nada ainda. Ouviu alguma coisa hoje que valia guardar?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BotaoPrincipal(
            texto = "Capturar em $idioma",
            aoClicar = aoClicar,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

private fun detalheDoCurso(total: Int, dominadas: Int, naFila: Int): String {
    val estoque = if (total == 0) "nenhuma ficha ainda" else "$total ${if (total == 1) "ficha" else "fichas"} · $dominadas ${if (dominadas == 1) "dominada" else "dominadas"}"
    val fila = when {
        total == 0 -> "capture a primeira"
        naFila == 0 -> "nada esfriou hoje"
        naFila == 1 -> "1 esfriou hoje"
        else -> "$naFila esfriaram hoje"
    }
    return "$estoque\n$fila"
}

/** A barra de baixo mais o vão do botão de captura, que passa dela para cima. */
private val ESPACO_DA_BARRA = 92.dp
