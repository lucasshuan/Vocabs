package com.jean.vocabs.ui

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import com.jean.vocabs.ui.capture.Aviso
import com.jean.vocabs.ui.capture.CapturaViewModel
import com.jean.vocabs.ui.capture.FaixaDeAviso
import com.jean.vocabs.ui.capture.GavetaDeTexto
import com.jean.vocabs.ui.capture.HubDeCaptura
import com.jean.vocabs.ui.capture.rememberCapturaRapida
import com.jean.vocabs.ui.components.ALTURA_DA_BARRA
import com.jean.vocabs.ui.components.Aba
import com.jean.vocabs.ui.components.BarraInferior
import com.jean.vocabs.ui.components.Icones
import com.jean.vocabs.ui.components.Movimento
import com.jean.vocabs.ui.configuracoes.ConfiguracoesScreen
import com.jean.vocabs.ui.card.FichaScreen
import com.jean.vocabs.ui.guardado.GuardadoScreen
import com.jean.vocabs.ui.home.HomeScreen
import com.jean.vocabs.ui.languages.NovoIdiomaScreen
import com.jean.vocabs.ui.languages.idiomaDe
import com.jean.vocabs.ui.start.InicioScreen
import com.jean.vocabs.ui.pendentes.FaixaDeDesfazer
import com.jean.vocabs.ui.pendentes.PendentesScreen
import com.jean.vocabs.ui.pendentes.PendentesViewModel
import com.jean.vocabs.ui.perfil.PerfilScreen
import com.jean.vocabs.ui.progresso.DiaADiaScreen
import com.jean.vocabs.ui.progresso.OQueFaltaScreen
import com.jean.vocabs.ui.progresso.ProgressoScreen
import com.jean.vocabs.ui.revisao.RevisaoScreen
import com.jean.vocabs.ui.selecionar.SelecionarScreen
import kotlinx.coroutines.delay
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

    fun card(id: Long) = "ficha/$id"
    fun selecionar(id: Long) = "selecionar/$id"
    fun guardado(ids: List<Long>) = "guardado/${ids.joinToString(SEPARADOR_DE_IDS)}"
    fun progresso(target: String) = "progresso/$target"
    fun diaADia(target: String) = "dia-a-dia/$target"
    fun oQueFalta(target: String) = "o-que-falta/$target"

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
    val scope = rememberCoroutineScope()

    // A mesma instância que a tela de Pendentes usa, e não a dela por baixo: o
    // selo da aba e a lista precisam concordar sobre o que já foi arrastado para
    // fora, e é este ViewModel que segura a exclusão em suspenso.
    val pendentesVm: PendentesViewModel = viewModel()
    val capturaVm: CapturaViewModel = viewModel()
    val totalPendentes by pendentesVm.total.collectAsStateWithLifecycle()
    val exclusaoPendente by pendentesVm.exclusao.collectAsStateWithLifecycle()
    val capture by capturaVm.estado.collectAsStateWithLifecycle()

    val barraVisivel = rota !in Rotas.telaCheia
    val abaAtual = Rotas.abaDe(rota)

    // A gaveta é estado, e não destino de navegação: ela precisa poder abrir por
    // cima de qualquer aba sem empurrar nada para a pilha, e o voltar do sistema
    // deve fechá-la — que é o que o `ModalBottomSheet` já faz.
    var gavetaAberta by remember { mutableStateOf(false) }
    val estadoDaGaveta = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    /**
     * O aviso de baixo, um de cada vez.
     *
     * Capturas em sequência **substituem** o aviso anterior e reiniciam os 5 s,
     * em vez de empilhar cartões: quem captura três coisas seguidas não quer ler
     * três confirmações, quer o botão livre para a quarta.
     */
    var aviso by remember { mutableStateOf<Aviso?>(null) }

    val gravacao = rememberCapturaRapida(
        target = capture.languagePair.target,
        aoGuardar = { format, path, durationMs, alvoDaCaptura ->
            val key = System.nanoTime()
            aviso = Aviso.Guardado(key, format, durationMs, alvoDaCaptura)
            capturaVm.salvarMidia(format, path, durationMs, alvoDaCaptura) { id ->
                // O id chega do banco alguns milissegundos depois. Só entra no
                // cartão se ele ainda for o mesmo — uma captura seguinte já pode
                // ter tomado o lugar dele.
                val current = aviso
                if (current is Aviso.Guardado && current.key == key) aviso = current.copy(captureId = id)
            }
        },
        aoRecado = { text -> aviso = Aviso.Recado(System.nanoTime(), text) },
    )

    fun fecharGaveta() {
        scope.launch { estadoDaGaveta.hide() }.invokeOnCompletion { gavetaAberta = false }
    }

    // O fundo desfoca enquanto o leque está aberto ou a gravação corre. O
    // `graphicsLayer` só entra na cadeia enquanto o gesto dura: uma camada de
    // composição sobre o `NavHost` inteiro é barata de manter por dois segundos e
    // cara de manter para sempre.
    var emGesto by remember { mutableStateOf(false) }
    var desfocando by remember { mutableStateOf(false) }
    LaunchedEffect(emGesto) {
        if (emGesto) {
            desfocando = true
        } else {
            delay(Movimento.PADRAO.toLong() + 80)
            desfocando = false
        }
    }
    val desfoque = animateFloatAsState(
        targetValue = if (emGesto) 1f else 0f,
        animationSpec = tween(Movimento.PADRAO),
        label = "desfoqueDoFundo",
    )

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = nav,
            startDestination = Rotas.INICIO,
            modifier = Modifier
                .fillMaxSize()
                // Enquanto o hub tem a tela, o que está atrás sai do alcance do
                // leitor de tela: o conteúdo continua composto debaixo do leque e
                // da gravação, e sem isto o TalkBack andaria por uma tela que a
                // pessoa não está mais vendo.
                .then(if (emGesto) Modifier.clearAndSetSemantics {} else Modifier)
                .then(
                    if (desfocando && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.graphicsLayer {
                            val raio = 15.dp.toPx() * desfoque.value
                            renderEffect = if (raio > 0.2f) BlurEffect(raio, raio, TileMode.Decal) else null
                        }
                    } else {
                        Modifier
                    },
                ),
            // A troca de aba é um fundido com um respiro de escala: a tela nova
            // chega em 98% e assenta. Um fundido puro entre quatro telas que têm
            // o mesmo fundo e a mesma barra embaixo não se lê como troca — se lê
            // como o conteúdo sendo repintado no lugar.
            enterTransition = { fadeIn(tween(Movimento.PADRAO)) + scaleIn(tween(Movimento.PADRAO), initialScale = 0.98f) },
            exitTransition = { fadeOut(tween(Movimento.RAPIDO)) },
        ) {
            composable(Rotas.INICIO) {
                InicioScreen(
                    aoCapturar = { gavetaAberta = true },
                    aoRevisar = { nav.navigate(Rotas.REVISAO) },
                    aoAbrirPerfil = { nav.irParaAba(Rotas.PERFIL) },
                    aoAdicionarIdioma = { nav.navigate(Rotas.NOVO_IDIOMA) },
                )
            }
            composable(Rotas.PALAVRAS) {
                HomeScreen(aoAbrirFicha = { nav.navigate(Rotas.card(it)) })
            }
            composable(Rotas.PENDENTES) {
                PendentesScreen(
                    aoAbrirCaptura = { nav.navigate(Rotas.selecionar(it.id)) },
                    aoAbrirFicha = { nav.navigate(Rotas.card(it.id)) },
                    vm = pendentesVm,
                )
            }
            composable(Rotas.PERFIL) {
                PerfilScreen(
                    aoAbrirProgresso = { nav.navigate(Rotas.progresso(it)) },
                    aoAbrirConfiguracoes = { nav.navigate(Rotas.CONFIGURACOES) },
                    aoAbrirNovoIdioma = { nav.navigate(Rotas.NOVO_IDIOMA) },
                )
            }
            composable(Rotas.PROGRESSO, arguments = listOf(navArgument("alvo") { type = NavType.StringType })) { entry ->
                ProgressoScreen(
                    // O alvo da rota é só por onde a tela entra: a gaveta da
                    // bandeira troca o curso olhado sem sair do destino, e é o
                    // curso de agora que as telas de dentro precisam receber.
                    target = entry.arguments?.getString("alvo"),
                    aoVoltar = { nav.popBackStack() },
                    aoAbrirDiaADia = { nav.navigate(Rotas.diaADia(it)) },
                    aoAbrirOQueFalta = { nav.navigate(Rotas.oQueFalta(it)) },
                    aoAdicionarIdioma = { nav.navigate(Rotas.NOVO_IDIOMA) },
                )
            }
            composable(Rotas.DIA_A_DIA, arguments = listOf(navArgument("alvo") { type = NavType.StringType })) { entry ->
                DiaADiaScreen(
                    target = entry.arguments?.getString("alvo"),
                    aoVoltar = { nav.popBackStack() },
                    aoAbrirFicha = { nav.navigate(Rotas.card(it)) },
                )
            }
            composable(Rotas.O_QUE_FALTA, arguments = listOf(navArgument("alvo") { type = NavType.StringType })) { entry ->
                OQueFaltaScreen(
                    target = entry.arguments?.getString("alvo"),
                    aoVoltar = { nav.popBackStack() },
                    aoAbrirFicha = { nav.navigate(Rotas.card(it)) },
                )
            }
            composable(Rotas.CONFIGURACOES) {
                ConfiguracoesScreen(
                    aoVoltar = { nav.popBackStack() },
                    aoTrocarIdiomaNativo = { nav.navigate(Rotas.IDIOMA_NATIVO) },
                )
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
            ) { entry ->
                FichaScreen(entry.arguments?.getLong("id") ?: 0, { nav.popBackStack() })
            }
            composable(
                Rotas.SELECIONAR,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
                enterTransition = { subir() },
                popExitTransition = { descer() },
            ) { entry ->
                SelecionarScreen(
                    id = entry.arguments?.getLong("id") ?: 0,
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
            ) { entry ->
                GuardadoScreen(
                    ids = entry.arguments?.getString("ids").orEmpty()
                        .split(SEPARADOR_DE_IDS)
                        .mapNotNull(String::toLongOrNull),
                    aoFechar = { nav.popBackStack() },
                    aoCapturarOutra = { nav.popBackStack(); gavetaAberta = true },
                    aoVerFichas = { nav.popBackStack(); nav.irParaAba(Rotas.PALAVRAS) },
                    aoRevisar = { nav.popBackStack(); nav.navigate(Rotas.REVISAO) },
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .then(if (emGesto) Modifier.clearAndSetSemantics {} else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // O aviso flutua sobre o conteúdo, encostado na barra: nada na tela
            // se move para acomodá-lo, e o indicador de página continua onde
            // estava, por baixo dele.
            FaixaDeAviso(
                // Some junto com a barra: nas telas cheias — Selecionar,
                // Guardado, Revisão — o rodapé já tem uma ação principal, e o
                // cartão pousaria em cima dela.
                aviso = aviso.takeIf { barraVisivel },
                aoSelecionar = { id ->
                    aviso = null
                    nav.navigate(Rotas.selecionar(id))
                },
                aoExpirar = { key -> if (aviso?.key == key) aviso = null },
                modifier = Modifier.padding(horizontal = 14.dp).padding(bottom = 8.dp),
            )

            // O desfazer da exclusão vive aqui em cima, e não dentro de
            // Pendentes, por duas razões: encostado na barra ele não briga com o
            // aviso de captura por espaço — os dois se empilham —, e trocar de
            // aba não tira a segunda chance da mão de quem acabou de arrastar
            // sem querer. Só some nas telas cheias, onde o rodapé já tem dono.
            FaixaDeDesfazer(
                exclusao = exclusaoPendente.takeIf { barraVisivel },
                aoDesfazer = pendentesVm::desfazer,
                modifier = Modifier.padding(horizontal = 14.dp).padding(bottom = 8.dp),
            )
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

        // O hub de captura ocupa a tela inteira e desenha por cima de tudo: o
        // leque, o véu e a gravação passam da borda da barra, e nada disso
        // caberia dentro do vão que ela deixa. Ele não intercepta toque nenhum
        // além do próprio `+`.
        // Entra e sai deslizando a mesma distância que a barra, e não a altura da
        // tela: o hub é do tamanho da tela, mas a única coisa visível dele fora
        // de um gesto é o `+` — e ele tem que descer junto com a barra em que
        // está encaixado, não vindo de um andar abaixo.
        val passoDaBarra = with(LocalDensity.current) { ALTURA_DA_BARRA.roundToPx() }
        AnimatedVisibility(
            visible = barraVisivel,
            enter = fadeIn(tween(Movimento.PADRAO)) + slideInVertically(tween(Movimento.PADRAO)) { passoDaBarra },
            exit = fadeOut(tween(Movimento.RAPIDO)) + slideOutVertically(tween(Movimento.RAPIDO)) { passoDaBarra },
            modifier = Modifier.fillMaxSize(),
        ) {
            HubDeCaptura(
                capture = gravacao,
                language = idiomaDe(capture.languagePair.target),
                aoAbrirTexto = { gavetaAberta = true },
                aoMudarDeFase = { emGesto = it },
            )
        }
    }

    if (gavetaAberta) {
        ModalBottomSheet(
            onDismissRequest = { gavetaAberta = false },
            sheetState = estadoDaGaveta,
        ) {
            GavetaDeTexto(
                aoGuardar = { snippet ->
                    capturaVm.salvarTrecho(snippet, capture.languagePair.target) { id ->
                        fecharGaveta()
                        nav.navigate(Rotas.selecionar(id))
                    }
                },
            )
        }
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
