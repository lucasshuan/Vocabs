package com.jean.vocabs.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.jean.vocabs.ui.captura.CapturaScreen
import com.jean.vocabs.ui.components.Aba
import com.jean.vocabs.ui.components.BarraInferior
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.configuracoes.ConfiguracoesScreen
import com.jean.vocabs.ui.ficha.FichaScreen
import com.jean.vocabs.ui.home.HomeScreen
import com.jean.vocabs.ui.idiomas.NovoIdiomaScreen
import com.jean.vocabs.ui.inicio.InicioScreen
import com.jean.vocabs.ui.pendentes.PendentesScreen
import com.jean.vocabs.ui.pendentes.PendentesViewModel
import com.jean.vocabs.ui.perfil.PerfilScreen
import com.jean.vocabs.ui.processar.ProcessarScreen
import com.jean.vocabs.ui.progresso.DiaADiaScreen
import com.jean.vocabs.ui.progresso.OQueFaltaScreen
import com.jean.vocabs.ui.progresso.ProgressoScreen
import com.jean.vocabs.ui.revisao.RevisaoScreen
import kotlinx.coroutines.launch

private object Rotas {
    const val INICIO = "inicio"
    const val PALAVRAS = "palavras"
    const val PENDENTES = "pendentes"
    const val PERFIL = "perfil"
    const val CAPTURA = "captura"
    const val REVISAO = "revisao"
    const val FICHA = "ficha/{id}"
    const val PROCESSAR = "processar/{id}"

    // Páginas de dentro: abrem por cima da aba, com voltar no topo. A barra
    // continua visível e marcando a aba de origem — é o que o handoff pede, e é
    // o que faz "Progresso" parecer parte do Perfil e não um lugar novo.
    const val PROGRESSO = "progresso"
    const val DIA_A_DIA = "dia-a-dia"
    const val O_QUE_FALTA = "o-que-falta"
    const val CONFIGURACOES = "configuracoes"
    const val NOVO_IDIOMA = "novo-idioma"
    const val IDIOMA_NATIVO = "idioma-nativo"

    fun ficha(id: Long) = "ficha/$id"
    fun processar(id: Long) = "processar/$id"
    val telaCheia = setOf(CAPTURA, REVISAO, FICHA, PROCESSAR, NOVO_IDIOMA, IDIOMA_NATIVO)

    private val paginasDeDentro = mapOf(
        PROGRESSO to PERFIL,
        DIA_A_DIA to PERFIL,
        O_QUE_FALTA to PERFIL,
        CONFIGURACOES to PERFIL,
    )

    /** Que aba a barra de baixo acende para esta rota. */
    fun abaDe(rota: String?): String? = rota?.let { paginasDeDentro[it] ?: it }
}

@Composable
fun VocabsApp() {
    val nav = rememberNavController()
    val pilha by nav.currentBackStackEntryAsState()
    val rota = pilha?.destination?.route
    val snackbar = remember { SnackbarHostState() }
    val escopo = rememberCoroutineScope()
    val pendentesVm: PendentesViewModel = viewModel()
    val totalPendentes by pendentesVm.total.collectAsStateWithLifecycle()
    val barraVisivel = rota !in Rotas.telaCheia
    // Numa página de dentro, quem fica aceso na barra é a aba de onde ela abriu.
    // Sem isto, entrar em Progresso apagaria a barra inteira e a pessoa perderia
    // a referência de onde está.
    val abaAtual = Rotas.abaDe(rota)

    fun avisar(texto: String) {
        escopo.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(texto)
        }
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = nav,
            startDestination = Rotas.INICIO,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(140)) },
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
                HomeScreen(aoAbrirFicha = { nav.navigate(Rotas.ficha(it)) })
            }
            composable(Rotas.PENDENTES) {
                PendentesScreen(
                    aoAbrirCaptura = { nav.navigate(Rotas.processar(it.id)) },
                    aoAbrirFicha = { nav.navigate(Rotas.ficha(it.id)) },
                )
            }
            composable(Rotas.PERFIL) {
                PerfilScreen(
                    aoAbrirProgresso = { nav.navigate(Rotas.PROGRESSO) },
                    aoAbrirConfiguracoes = { nav.navigate(Rotas.CONFIGURACOES) },
                    aoAbrirNovoIdioma = { nav.navigate(Rotas.NOVO_IDIOMA) },
                    aoTrocarIdiomaNativo = { nav.navigate(Rotas.IDIOMA_NATIVO) },
                )
            }
            composable(Rotas.PROGRESSO) {
                ProgressoScreen(
                    aoVoltar = { nav.popBackStack() },
                    aoAbrirDiaADia = { nav.navigate(Rotas.DIA_A_DIA) },
                    aoAbrirOQueFalta = { nav.navigate(Rotas.O_QUE_FALTA) },
                )
            }
            composable(Rotas.DIA_A_DIA) {
                DiaADiaScreen(
                    aoVoltar = { nav.popBackStack() },
                    aoAbrirFicha = { nav.navigate(Rotas.ficha(it)) },
                )
            }
            composable(Rotas.O_QUE_FALTA) {
                OQueFaltaScreen(
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
            composable(Rotas.CAPTURA, enterTransition = { subir() }, popExitTransition = { descer() }) {
                CapturaScreen(
                    aoCapturarTexto = {
                        nav.popBackStack()
                        avisar("Captura salva. O processamento continua em segundo plano.")
                    },
                    aoVoltar = { nav.popBackStack() },
                )
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
                Rotas.PROCESSAR,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
                enterTransition = { subir() },
                popExitTransition = { descer() },
            ) { entrada ->
                ProcessarScreen(entrada.arguments?.getLong("id") ?: 0, { nav.popBackStack() })
            }
        }

        Column(modifier = Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
            SnackbarHost(snackbar, Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
            AnimatedVisibility(
                visible = barraVisivel,
                enter = slideInVertically(tween(220)) { it } + fadeIn(tween(220)),
                exit = slideOutVertically(tween(170)) { it } + fadeOut(tween(170)),
            ) {
                BarraInferior(
                    abasEsquerda = listOf(Aba(Rotas.INICIO, Icones.Casa, "Início"), Aba(Rotas.PALAVRAS, Icones.Cartas, "Palavras")),
                    abasDireita = listOf(Aba(Rotas.PENDENTES, Icones.Relogio, "Pendentes", totalPendentes), Aba(Rotas.PERFIL, Icones.Pessoa, "Perfil")),
                    rotaAtual = abaAtual,
                    aoNavegar = nav::irParaAba,
                    aoCapturar = { nav.navigate(Rotas.CAPTURA) },
                )
            }
        }
    }
}

private fun subir() = slideInVertically(tween(300, easing = FastOutSlowInEasing)) { it / 5 } + fadeIn(tween(300))
private fun descer() = slideOutVertically(tween(220)) { it / 5 } + fadeOut(tween(220))

private fun NavHostController.irParaAba(rota: String) {
    navigate(rota) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
