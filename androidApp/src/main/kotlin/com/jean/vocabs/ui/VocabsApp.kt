package com.jean.vocabs.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.shared.domain.FormatoCaptura
import com.jean.vocabs.ui.captura.CapturaScreen
import com.jean.vocabs.ui.captura.CapturaViewModel
import com.jean.vocabs.ui.captura.rememberCapturaRapida
import com.jean.vocabs.ui.components.Aba
import com.jean.vocabs.ui.components.BarraInferior
import com.jean.vocabs.ui.components.CENTRO_DO_BOTAO_DO_TOPO
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.LequeDeCaptura
import com.jean.vocabs.ui.ficha.FichaScreen
import com.jean.vocabs.ui.home.HomeScreen
import com.jean.vocabs.ui.inicio.InicioScreen
import com.jean.vocabs.ui.pendentes.PendentesScreen
import com.jean.vocabs.ui.pendentes.PendentesViewModel
import com.jean.vocabs.ui.perfil.PerfilScreen
import com.jean.vocabs.ui.processar.ProcessarScreen
import com.jean.vocabs.ui.revisao.RevisaoScreen
import kotlinx.coroutines.launch

private object Rotas {
    /**
     * A primeira aba e o destino inicial.
     *
     * O app abria em "Palavras", o que fazia toda captura custar um toque a mais
     * — atrito contra o princípio 1 do produto, "captura antes de tudo". Hoje a
     * captura mora no botão central da barra, alcançável de qualquer aba.
     */
    const val INICIO = "inicio"
    const val PALAVRAS = "palavras"
    const val PENDENTES = "pendentes"
    const val PERFIL = "perfil"

    const val CAPTURA = "captura"
    const val REVISAO = "revisao"
    const val FICHA = "ficha/{id}"
    const val PROCESSAR = "processar/{id}"
    fun ficha(id: Long) = "ficha/$id"
    fun processar(id: Long) = "processar/$id"

    /** Telas que ocupam a tela inteira: a barra sai de cena nelas. */
    val emTelaCheia = setOf(FICHA, PROCESSAR, REVISAO, CAPTURA)
}

@Composable
fun VocabsApp() {
    val nav = rememberNavController()
    val pilha by nav.currentBackStackEntryAsState()
    val rotaAtual = pilha?.destination?.route

    val snackbar = remember { SnackbarHostState() }
    val escopo = rememberCoroutineScope()

    // Escopados à Activity de propósito: alimentam a barra, que vive fora dos
    // destinos de navegação.
    val pendentesVm: PendentesViewModel = viewModel()
    val pendentes by pendentesVm.total.collectAsStateWithLifecycle()
    val capturaVm: CapturaViewModel = viewModel()

    var lequeAberto by remember { mutableStateOf(false) }

    fun avisar(texto: String) {
        escopo.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(texto)
        }
    }

    fun avisarMidia(formato: FormatoCaptura) = avisar(
        when (formato) {
            FormatoCaptura.AUDIO -> "Áudio guardado. Transcreva quando puder."
            FormatoCaptura.FOTO -> "Foto guardada. Transcreva quando puder."
            FormatoCaptura.TEXTO -> "Capturado!"
        },
    )

    // A captura vive aqui, e não numa tela: o botão que a dispara é da barra, e
    // o estado precisa sobreviver à troca de aba — sair de uma aba no meio de
    // uma gravação não pode perdê-la.
    val captura = rememberCapturaRapida(
        aoSalvarMidia = { formato, caminho -> capturaVm.salvarMidia(formato, caminho, "") },
        aoCapturado = { formato ->
            lequeAberto = false
            nav.irParaAba(Rotas.PENDENTES)
            avisarMidia(formato)
        },
    )

    val barraVisivel = rotaAtual !in Rotas.emTelaCheia

    // O "voltar" do sistema fecha o leque antes de sair da tela: enquanto há um
    // menu aberto na frente, ele é o que a pessoa espera fechar.
    BackHandler(enabled = lequeAberto) { lequeAberto = false }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = nav,
            startDestination = Rotas.INICIO,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 24 }
            },
            exitTransition = { fadeOut(tween(150)) },
            popEnterTransition = { fadeIn(tween(260)) },
            popExitTransition = { fadeOut(tween(150)) },
        ) {
            composable(Rotas.INICIO) {
                InicioScreen(
                    aoEscrever = { nav.navigate(Rotas.CAPTURA) },
                    aoAbrirPalavras = { nav.irParaAba(Rotas.PALAVRAS) },
                    aoAbrirPendentes = { nav.irParaAba(Rotas.PENDENTES) },
                    aoRevisar = { nav.navigate(Rotas.REVISAO) },
                    aoAbrirPerfil = { nav.irParaAba(Rotas.PERFIL) },
                )
            }

            composable(Rotas.PALAVRAS) {
                HomeScreen(
                    aoAbrirFicha = { id -> nav.navigate(Rotas.ficha(id)) },
                    // navigate puro, nunca irParaAba: aquele helper faz
                    // popUpTo(startDestination) e o "voltar" da sessão sairia do app.
                    aoRevisar = { nav.navigate(Rotas.REVISAO) },
                )
            }

            composable(Rotas.PENDENTES) {
                PendentesScreen(aoAbrirEntrada = { entrada -> nav.abrir(entrada) })
            }

            composable(Rotas.PERFIL) { PerfilScreen() }

            composable(
                route = Rotas.CAPTURA,
                enterTransition = { subir() },
                popExitTransition = { descer() },
            ) {
                CapturaScreen(
                    aoCapturarTexto = {
                        nav.popBackStack()
                        avisar("Capturado! A ficha chega sozinha.")
                    },
                    aoVoltar = { nav.popBackStack() },
                )
            }

            composable(
                route = Rotas.REVISAO,
                enterTransition = { subir() },
                popExitTransition = { descer() },
            ) {
                RevisaoScreen(aoVoltar = { nav.popBackStack() })
            }

            // Ficha e processamento sobem por cima de tudo, como uma folha.
            composable(
                route = Rotas.FICHA,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
                enterTransition = { subir() },
                popExitTransition = { descer() },
            ) { entrada ->
                FichaScreen(
                    id = entrada.arguments?.getLong("id") ?: 0L,
                    aoVoltar = { nav.popBackStack() },
                )
            }

            composable(
                route = Rotas.PROCESSAR,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
                enterTransition = { subir() },
                popExitTransition = { descer() },
            ) { entrada ->
                ProcessarScreen(
                    id = entrada.arguments?.getLong("id") ?: 0L,
                    aoVoltar = { nav.popBackStack() },
                )
            }
        }

        // Escurece o conteúdo enquanto o leque está aberto, e fecha ao toque.
        // Sem ele, tocar "atrás" das opções acionaria a tela por baixo.
        AnimatedVisibility(
            visible = lequeAberto,
            enter = fadeIn(tween(240)),
            exit = fadeOut(tween(160)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { lequeAberto = false },
            )
        }

        // Fora da Column da barra: o leque não pode empurrar layout ao abrir,
        // então ele desenha por cima, ancorado onde o botão central está.
        if (barraVisivel) {
            // Ancorado no centro do botão: a barra tem 64dp de conteúdo mais o
            // inset, e o botão fica a 35dp do topo dela.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 64.dp - CENTRO_DO_BOTAO_DO_TOPO + 14.dp),
            ) {
                LequeDeCaptura(
                    aberto = lequeAberto,
                    aoGravarAudio = {
                        lequeAberto = false
                        captura.gravarAudio()
                    },
                    aoTirarFoto = {
                        lequeAberto = false
                        captura.tirarFoto()
                    },
                    aoEscrever = {
                        lequeAberto = false
                        nav.navigate(Rotas.CAPTURA)
                    },
                )
            }
        }

        // Sem navigationBarsPadding aqui: a barra desenha por baixo da barra de
        // gestos de propósito e aplica o inset no próprio conteúdo.
        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            AnimatedVisibility(
                visible = barraVisivel,
                enter = slideInVertically(tween(260)) { it * 2 } + fadeIn(tween(260)),
                exit = slideOutVertically(tween(200)) { it * 2 } + fadeOut(tween(200)),
            ) {
                BarraInferior(
                    abasEsquerda = listOf(
                        Aba(Rotas.INICIO, Icones.Casa, "Início"),
                        Aba(Rotas.PALAVRAS, Icones.Cartas, "Palavras"),
                    ),
                    abasDireita = listOf(
                        Aba(Rotas.PENDENTES, Icones.Relogio, "Pendentes", selo = pendentes),
                        Aba(Rotas.PERFIL, Icones.Pessoa, "Perfil"),
                    ),
                    rotaAtual = rotaAtual,
                    lequeAberto = lequeAberto,
                    gravando = captura.gravando,
                    segundosGravados = captura.segundos,
                    aoNavegar = { rota ->
                        lequeAberto = false
                        nav.irParaAba(rota)
                    },
                    aoAlternarLeque = { lequeAberto = !lequeAberto },
                    aoPararGravacao = { captura.pararAudio() },
                )
            }
        }
    }
}

private fun subir() =
    slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it / 5 } + fadeIn(tween(320))

private fun descer() =
    slideOutVertically(tween(240)) { it / 5 } + fadeOut(tween(240))

/** Rascunho vai para a transcrição; o resto já tem ficha (ou erro) para mostrar. */
private fun NavHostController.abrir(entrada: Entrada) {
    val rota =
        if (entrada.precisaTranscricao) Rotas.processar(entrada.id) else Rotas.ficha(entrada.id)
    navigate(rota)
}

/** Troca de aba sem empilhar destinos, preservando o estado de cada uma. */
private fun NavHostController.irParaAba(rota: String) {
    navigate(rota) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
