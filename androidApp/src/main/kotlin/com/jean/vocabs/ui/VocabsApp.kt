package com.jean.vocabs.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jean.vocabs.shared.domain.FormatoCaptura
import com.jean.vocabs.ui.captura.CapturaViewModel
import com.jean.vocabs.ui.captura.FolhaDeCaptura
import com.jean.vocabs.ui.captura.rememberCapturaRapida
import com.jean.vocabs.ui.components.ALTURA_DA_BARRA
import com.jean.vocabs.ui.components.Aba
import com.jean.vocabs.ui.components.BarraInferior
import com.jean.vocabs.ui.components.BotaoDeCaptura
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.MINIMO_DE_GRAVACAO_MS
import com.jean.vocabs.ui.components.Movimento
import com.jean.vocabs.ui.configuracoes.ConfiguracoesScreen
import com.jean.vocabs.ui.ficha.FichaScreen
import com.jean.vocabs.ui.guardado.GuardadoScreen
import com.jean.vocabs.ui.home.HomeScreen
import com.jean.vocabs.ui.idiomas.NovoIdiomaScreen
import com.jean.vocabs.ui.inicio.InicioScreen
import com.jean.vocabs.ui.pendentes.PendentesScreen
import com.jean.vocabs.ui.pendentes.PendentesViewModel
import com.jean.vocabs.ui.perfil.PerfilScreen
import com.jean.vocabs.ui.progresso.DiaADiaScreen
import com.jean.vocabs.ui.progresso.OQueFaltaScreen
import com.jean.vocabs.ui.progresso.ProgressoScreen
import com.jean.vocabs.ui.revisao.RevisaoScreen
import com.jean.vocabs.ui.selecionar.SelecionarScreen
import kotlinx.coroutines.launch

private object Rotas {
    const val INICIO = "inicio"
    const val PALAVRAS = "palavras"
    const val PENDENTES = "pendentes"
    const val PERFIL = "perfil"
    const val REVISAO = "revisao"
    const val FICHA = "ficha/{id}"
    const val SELECIONAR = "selecionar/{id}"
    const val GUARDADO = "guardado/{ids}"

    // Páginas de dentro: abrem por cima da aba, com voltar no topo. A barra
    // continua visível e marcando a aba de origem — é o que faz "Seu progresso"
    // parecer parte da Você e não um lugar novo.
    const val PROGRESSO = "progresso/{alvo}"
    const val DIA_A_DIA = "dia-a-dia/{alvo}"
    const val O_QUE_FALTA = "o-que-falta/{alvo}"
    const val CONFIGURACOES = "configuracoes"
    const val NOVO_IDIOMA = "novo-idioma"
    const val IDIOMA_NATIVO = "idioma-nativo"

    fun ficha(id: Long) = "ficha/$id"
    fun selecionar(id: Long) = "selecionar/$id"
    fun guardado(ids: List<Long>) = "guardado/${ids.joinToString(SEPARADOR_DE_IDS)}"
    fun progresso(alvo: String) = "progresso/$alvo"
    fun diaADia(alvo: String) = "dia-a-dia/$alvo"
    fun oQueFalta(alvo: String) = "o-que-falta/$alvo"

    val telaCheia = setOf(REVISAO, FICHA, SELECIONAR, GUARDADO, NOVO_IDIOMA, IDIOMA_NATIVO)

    private val paginasDeDentro = mapOf(
        PROGRESSO to PERFIL,
        DIA_A_DIA to PERFIL,
        O_QUE_FALTA to PERFIL,
        CONFIGURACOES to PERFIL,
    )

    /** Que aba a barra de baixo acende para esta rota. */
    fun abaDe(rota: String?): String? = rota?.let { paginasDeDentro[it] ?: it }
}

/** Vírgula não aparece em id nenhum, e o argumento de rota não precisa ser escapado. */
private const val SEPARADOR_DE_IDS = ","

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabsApp() {
    val nav = rememberNavController()
    val pilha by nav.currentBackStackEntryAsState()
    val rota = pilha?.destination?.route
    val snackbar = remember { SnackbarHostState() }
    val escopo = rememberCoroutineScope()

    val pendentesVm: PendentesViewModel = viewModel()
    val capturaVm: CapturaViewModel = viewModel()
    val totalPendentes by pendentesVm.total.collectAsStateWithLifecycle()
    val captura by capturaVm.estado.collectAsStateWithLifecycle()

    val barraVisivel = rota !in Rotas.telaCheia
    val abaAtual = Rotas.abaDe(rota)

    // A folha é estado, e não destino de navegação: ela precisa poder abrir por
    // cima de qualquer aba sem empurrar nada para a pilha, e o voltar do sistema
    // deve fechá-la — que é o que o `ModalBottomSheet` já faz.
    var folhaAberta by remember { mutableStateOf(false) }
    var formatoDaFolha by remember { mutableStateOf(FormatoCaptura.TEXTO) }
    val estadoDaFolha = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    /**
     * O idioma da gravação é decidido no momento em que ela começa.
     *
     * Guardado num `var` e não lido do estado na hora de salvar porque a pessoa
     * pode trocar de página do carrossel enquanto o áudio corre — e o destino
     * tem que ser o que estava marcado quando o dedo desceu.
     */
    var alvoDaGravacao by remember { mutableStateOf("") }

    val gravacao = rememberCapturaRapida(
        aoSalvarMidia = { formato, caminho, duracaoMs ->
            capturaVm.salvarMidia(formato, caminho, duracaoMs, alvoDaGravacao.ifBlank { captura.par.alvo })
        },
        aoCapturado = { formato ->
            avisar(escopo, snackbar, mensagemDeCaptura(formato))
        },
    )

    fun fecharFolha() {
        escopo.launch { estadoDaFolha.hide() }.invokeOnCompletion { folhaAberta = false }
    }

    fun abrirFolha(formato: FormatoCaptura = FormatoCaptura.TEXTO) {
        formatoDaFolha = formato
        folhaAberta = true
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = nav,
            startDestination = Rotas.INICIO,
            modifier = Modifier.fillMaxSize(),
            // A troca de aba é um fundido com um respiro de escala: a tela nova
            // chega em 98% e assenta. Um fundido puro entre quatro telas que têm
            // o mesmo fundo e a mesma barra embaixo não se lê como troca — se lê
            // como o conteúdo sendo repintado no lugar.
            enterTransition = { fadeIn(tween(Movimento.PADRAO)) + scaleIn(tween(Movimento.PADRAO), initialScale = 0.98f) },
            exitTransition = { fadeOut(tween(Movimento.RAPIDO)) },
        ) {
            composable(Rotas.INICIO) {
                InicioScreen(
                    aoCapturar = { abrirFolha() },
                    aoAbrirPendentes = { nav.irParaAba(Rotas.PENDENTES) },
                    aoRevisar = { nav.navigate(Rotas.REVISAO) },
                    aoAbrirPerfil = { nav.irParaAba(Rotas.PERFIL) },
                    aoAdicionarIdioma = { nav.navigate(Rotas.NOVO_IDIOMA) },
                )
            }
            composable(Rotas.PALAVRAS) {
                HomeScreen(aoAbrirFicha = { nav.navigate(Rotas.ficha(it)) })
            }
            composable(Rotas.PENDENTES) {
                PendentesScreen(
                    aoAbrirCaptura = { nav.navigate(Rotas.selecionar(it.id)) },
                    aoAbrirFicha = { nav.navigate(Rotas.ficha(it.id)) },
                )
            }
            composable(Rotas.PERFIL) {
                PerfilScreen(
                    aoAbrirProgresso = { nav.navigate(Rotas.progresso(it)) },
                    aoAbrirConfiguracoes = { nav.navigate(Rotas.CONFIGURACOES) },
                    aoAbrirNovoIdioma = { nav.navigate(Rotas.NOVO_IDIOMA) },
                    aoTrocarIdiomaNativo = { nav.navigate(Rotas.IDIOMA_NATIVO) },
                )
            }
            composable(Rotas.PROGRESSO, arguments = listOf(navArgument("alvo") { type = NavType.StringType })) { entrada ->
                val alvo = entrada.arguments?.getString("alvo")
                ProgressoScreen(
                    alvo = alvo,
                    aoVoltar = { nav.popBackStack() },
                    aoAbrirDiaADia = { nav.navigate(Rotas.diaADia(alvo.orEmpty())) },
                    aoAbrirOQueFalta = { nav.navigate(Rotas.oQueFalta(alvo.orEmpty())) },
                )
            }
            composable(Rotas.DIA_A_DIA, arguments = listOf(navArgument("alvo") { type = NavType.StringType })) { entrada ->
                DiaADiaScreen(
                    alvo = entrada.arguments?.getString("alvo"),
                    aoVoltar = { nav.popBackStack() },
                    aoAbrirFicha = { nav.navigate(Rotas.ficha(it)) },
                )
            }
            composable(Rotas.O_QUE_FALTA, arguments = listOf(navArgument("alvo") { type = NavType.StringType })) { entrada ->
                OQueFaltaScreen(
                    alvo = entrada.arguments?.getString("alvo"),
                    aoVoltar = { nav.popBackStack() },
                    aoAbrirFicha = { nav.navigate(Rotas.ficha(it)) },
                )
            }
            composable(Rotas.CONFIGURACOES) {
                ConfiguracoesScreen(aoVoltar = { nav.popBackStack() })
            }
            composable(Rotas.NOVO_IDIOMA, enterTransition = { subir() }, popExitTransition = { descer() }) {
                NovoIdiomaScreen(aoVoltar = { nav.popBackStack() })
            }
            composable(Rotas.IDIOMA_NATIVO, enterTransition = { subir() }, popExitTransition = { descer() }) {
                NovoIdiomaScreen(aoVoltar = { nav.popBackStack() }, paraNativo = true)
            }
            composable(Rotas.REVISAO, enterTransition = { subir() }, popExitTransition = { descer() }) {
                RevisaoScreen(aoVoltar = { nav.popBackStack() })
            }
            composable(
                Rotas.FICHA,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
                enterTransition = { subir() },
                popExitTransition = { descer() },
            ) { entrada ->
                FichaScreen(entrada.arguments?.getLong("id") ?: 0, { nav.popBackStack() })
            }
            composable(
                Rotas.SELECIONAR,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
                enterTransition = { subir() },
                popExitTransition = { descer() },
            ) { entrada ->
                SelecionarScreen(
                    id = entrada.arguments?.getLong("id") ?: 0,
                    aoVoltar = { nav.popBackStack() },
                    aoGuardar = { ids ->
                        // A seleção sai da pilha junto: voltar da confirmação
                        // deve levar de onde a captura começou, e não a um
                        // trecho que já virou ficha.
                        nav.navigate(Rotas.guardado(ids)) {
                            popUpTo(Rotas.SELECIONAR) { inclusive = true }
                        }
                    },
                )
            }
            composable(
                Rotas.GUARDADO,
                arguments = listOf(navArgument("ids") { type = NavType.StringType }),
                enterTransition = { subir() },
                popExitTransition = { descer() },
            ) { entrada ->
                GuardadoScreen(
                    ids = entrada.arguments?.getString("ids").orEmpty()
                        .split(SEPARADOR_DE_IDS)
                        .mapNotNull(String::toLongOrNull),
                    aoFechar = { nav.popBackStack() },
                    aoCapturarOutra = { nav.popBackStack(); abrirFolha() },
                    aoVerFichas = { nav.popBackStack(); nav.irParaAba(Rotas.PALAVRAS) },
                    aoRevisar = { nav.popBackStack(); nav.navigate(Rotas.REVISAO) },
                )
            }
        }

        Column(modifier = Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
            SnackbarHost(snackbar, Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
            AnimatedVisibility(
                visible = barraVisivel,
                enter = slideInVertically(tween(Movimento.PADRAO)) { it } + fadeIn(tween(Movimento.PADRAO)),
                exit = slideOutVertically(tween(Movimento.RAPIDO)) { it } + fadeOut(tween(Movimento.RAPIDO)),
            ) {
                BarraInferior(
                    abasEsquerda = listOf(Aba(Rotas.INICIO, Icones.Casa, "Início"), Aba(Rotas.PALAVRAS, Icones.Cartas, "Palavras")),
                    abasDireita = listOf(Aba(Rotas.PENDENTES, Icones.Relogio, "Pendentes", totalPendentes), Aba(Rotas.PERFIL, Icones.Pessoa, "Perfil")),
                    rotaAtual = abaAtual,
                    aoNavegar = nav::irParaAba,
                )
            }
        }

        // O botão fica por cima da barra, ancorado no vão que ela deixa: a mesma
        // altura e os mesmos insets reproduzem o centro do slot vazio, e daqui a
        // sombra e o rótulo de gravação podem passar da borda sem ser recortados.
        AnimatedVisibility(
            visible = barraVisivel,
            enter = slideInVertically(tween(Movimento.PADRAO)) { it } + fadeIn(tween(Movimento.PADRAO)),
            exit = slideOutVertically(tween(Movimento.RAPIDO)) { it } + fadeOut(tween(Movimento.RAPIDO)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.navigationBarsPadding().height(ALTURA_DA_BARRA),
            ) {
                BotaoDeCaptura(
                    gravando = gravacao.gravando,
                    segundos = gravacao.segundos,
                    aoTocar = { abrirFolha() },
                    aoComecarAGravar = {
                        alvoDaGravacao = captura.par.alvo
                        gravacao.gravarAudio()
                    },
                    aoTerminarDeGravar = {
                        if (gravacao.segundos * 1_000L < MINIMO_DE_GRAVACAO_MS) gravacao.cancelarAudio()
                        else gravacao.pararAudio()
                    },
                )
            }
        }
    }

    if (folhaAberta) {
        ModalBottomSheet(
            onDismissRequest = { folhaAberta = false },
            sheetState = estadoDaFolha,
        ) {
            FolhaDeCaptura(
                cursos = captura.cursos,
                alvoInicial = captura.par.alvo,
                formatoInicial = formatoDaFolha,
                gravando = gravacao.gravando,
                segundos = gravacao.segundos,
                aoContinuarComTexto = { alvo, trecho ->
                    capturaVm.salvarTrecho(trecho, alvo) { id ->
                        fecharFolha()
                        nav.navigate(Rotas.selecionar(id))
                    }
                },
                aoGravar = { alvo ->
                    alvoDaGravacao = alvo
                    gravacao.gravarAudio()
                },
                aoPararDeGravar = {
                    gravacao.pararAudio()
                    fecharFolha()
                },
                aoFotografar = { alvo ->
                    alvoDaGravacao = alvo
                    gravacao.tirarFoto()
                    fecharFolha()
                },
            )
        }
    }
}

private fun mensagemDeCaptura(formato: FormatoCaptura): String = when (formato) {
    FormatoCaptura.AUDIO -> "Áudio na fila. A transcrição continua em segundo plano."
    FormatoCaptura.FOTO -> "Foto na fila. A leitura continua em segundo plano."
    FormatoCaptura.TEXTO -> "Captura salva."
}

private fun avisar(
    escopo: kotlinx.coroutines.CoroutineScope,
    snackbar: SnackbarHostState,
    texto: String,
) {
    escopo.launch {
        snackbar.currentSnackbarData?.dismiss()
        snackbar.showSnackbar(texto)
    }
}

/**
 * As telas cheias sobem por cima da aba e descem de volta.
 *
 * A entrada é mais longa que a saída de propósito, e é assim em todo o app: quem
 * abre uma tela vai ficar nela e tem tempo de ver a chegada; quem fecha já
 * decidiu sair, e cada milissegundo a mais ali é espera pura.
 */
private fun subir() =
    slideInVertically(tween(Movimento.PADRAO, easing = FastOutSlowInEasing)) { it / 5 } + fadeIn(tween(Movimento.PADRAO))

private fun descer() =
    slideOutVertically(tween(Movimento.RAPIDO)) { it / 5 } + fadeOut(tween(Movimento.RAPIDO))

private fun NavHostController.irParaAba(rota: String) {
    navigate(rota) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
