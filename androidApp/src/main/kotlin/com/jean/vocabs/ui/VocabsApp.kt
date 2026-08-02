package com.jean.vocabs.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.ficha.FichaScreen
import com.jean.vocabs.ui.home.HomeScreen
import com.jean.vocabs.ui.inbox.InboxScreen
import com.jean.vocabs.ui.inbox.InboxViewModel
import com.jean.vocabs.ui.processar.ProcessarScreen
import com.jean.vocabs.ui.revisao.RevisaoScreen
import kotlinx.coroutines.launch

private object Rotas {
    const val PALAVRAS = "palavras"
    const val INBOX = "inbox"
    const val CAPTURA = "captura"
    const val REVISAO = "revisao"
    const val FICHA = "ficha/{id}"
    const val PROCESSAR = "processar/{id}"
    fun ficha(id: Long) = "ficha/$id"
    fun processar(id: Long) = "processar/$id"

    /** Telas que ocupam a tela inteira: a barra de abas sai de cena nelas. */
    val emTelaCheia = setOf(FICHA, PROCESSAR, REVISAO)
}

@Composable
fun VocabsApp() {
    val nav = rememberNavController()
    val pilha by nav.currentBackStackEntryAsState()
    val rotaAtual = pilha?.destination?.route

    val snackbar = remember { SnackbarHostState() }
    val escopo = rememberCoroutineScope()

    // Escopado à Activity de propósito: a contagem alimenta o selo da barra, que
    // vive fora dos destinos de navegação.
    val inboxVm: InboxViewModel = viewModel()
    val pendentes by inboxVm.total.collectAsStateWithLifecycle()

    fun avisar(texto: String) {
        escopo.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(texto)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = nav,
            startDestination = Rotas.PALAVRAS,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 24 }
            },
            exitTransition = { fadeOut(tween(150)) },
            popEnterTransition = { fadeIn(tween(260)) },
            popExitTransition = { fadeOut(tween(150)) },
        ) {
            composable(Rotas.PALAVRAS) {
                HomeScreen(
                    aoAbrirFicha = { id -> nav.navigate(Rotas.ficha(id)) },
                    // navigate puro, nunca irParaAba: aquele helper faz
                    // popUpTo(startDestination) e o "voltar" da sessão sairia do app.
                    aoRevisar = { nav.navigate(Rotas.REVISAO) },
                )
            }

            composable(Rotas.INBOX) {
                InboxScreen(aoAbrirEntrada = { entrada -> nav.abrir(entrada) })
            }

            composable(Rotas.CAPTURA) {
                CapturaScreen(
                    aoCapturarTexto = {
                        nav.irParaAba(Rotas.PALAVRAS)
                        avisar("Capturado! A ficha chega sozinha.")
                    },
                    aoCapturarMidia = { formato ->
                        nav.irParaAba(Rotas.INBOX)
                        avisar(
                            when (formato) {
                                FormatoCaptura.AUDIO -> "Áudio guardado. Transcreva quando puder."
                                FormatoCaptura.FOTO -> "Foto guardada. Transcreva quando puder."
                                FormatoCaptura.TEXTO -> "Capturado!"
                            },
                        )
                    },
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

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            BarraDeAbas(
                visivel = rotaAtual !in Rotas.emTelaCheia,
                rotaAtual = rotaAtual,
                pendentes = pendentes,
                aoNavegar = { rota -> nav.irParaAba(rota) },
            )
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

@Composable
private fun BarraDeAbas(
    visivel: Boolean,
    rotaAtual: String?,
    pendentes: Int,
    aoNavegar: (String) -> Unit,
) {
    AnimatedVisibility(
        visible = visivel,
        enter = slideInVertically(tween(260)) { it * 2 } + fadeIn(tween(260)),
        exit = slideOutVertically(tween(200)) { it * 2 } + fadeOut(tween(200)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp, top = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 10.dp,
            ) {
                Row(modifier = Modifier.padding(6.dp)) {
                    ItemAba(
                        selecionado = rotaAtual == Rotas.PALAVRAS,
                        icone = Icones.Cartas,
                        rotulo = "Palavras",
                        aoClicar = { aoNavegar(Rotas.PALAVRAS) },
                    )
                    ItemAba(
                        selecionado = rotaAtual == Rotas.INBOX,
                        icone = Icones.Inbox,
                        rotulo = "Inbox",
                        selo = pendentes,
                        aoClicar = { aoNavegar(Rotas.INBOX) },
                    )
                    ItemAba(
                        selecionado = rotaAtual == Rotas.CAPTURA,
                        icone = Icones.CapturarCirculo,
                        rotulo = "Capturar",
                        aoClicar = { aoNavegar(Rotas.CAPTURA) },
                    )
                }
            }
        }
    }
}

/** Pílula que expande mostrando o rótulo quando a aba está ativa. */
@Composable
private fun ItemAba(
    selecionado: Boolean,
    icone: ImageVector,
    rotulo: String,
    aoClicar: () -> Unit,
    selo: Int = 0,
) {
    val cores = MaterialTheme.colorScheme
    val corConteudo by animateColorAsState(
        targetValue = if (selecionado) cores.primary else cores.onSurfaceVariant,
        animationSpec = tween(220),
        label = "corAba",
    )
    val corFundo by animateColorAsState(
        targetValue = if (selecionado) cores.primary.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = tween(220),
        label = "fundoAba",
    )

    Surface(
        onClick = aoClicar,
        shape = RoundedCornerShape(24.dp),
        color = corFundo,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .animateContentSize(tween(220))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Box {
                Icon(
                    imageVector = icone,
                    contentDescription = rotulo,
                    tint = corConteudo,
                    modifier = Modifier.size(22.dp),
                )
                SeloDeContagem(
                    quantidade = selo,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
            AnimatedVisibility(
                visible = selecionado,
                enter = expandHorizontally(tween(220)) + fadeIn(tween(220)),
                exit = shrinkHorizontally(tween(160)) + fadeOut(tween(120)),
            ) {
                Text(
                    text = rotulo,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = corConteudo,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

/**
 * O backlog do inbox à vista o tempo todo.
 *
 * O documento de métricas trata essa fila como o alerta mais importante do app:
 * se ela só cresce, a captura está andando mais rápido que o processamento.
 */
@Composable
private fun SeloDeContagem(quantidade: Int, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = quantidade > 0,
        enter = scaleIn(tween(240)) + fadeIn(tween(240)),
        exit = scaleOut(tween(160)) + fadeOut(tween(160)),
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                // Puxa o selo para fora do ícone sem alterar o tamanho da pílula.
                .offset(x = 6.dp, y = (-5).dp)
                .defaultMinSize(minWidth = 15.dp, minHeight = 15.dp)
                .background(MaterialTheme.colorScheme.tertiary, CircleShape),
        ) {
            Text(
                text = if (quantidade > 9) "9+" else "$quantidade",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 0.sp,
                ),
                color = MaterialTheme.colorScheme.onTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 3.dp),
            )
        }
    }
}
