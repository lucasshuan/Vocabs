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
import com.jean.vocabs.ui.capture.CaptureHub
import com.jean.vocabs.ui.capture.CaptureViewModel
import com.jean.vocabs.ui.capture.Notice
import com.jean.vocabs.ui.capture.NoticeStrip
import com.jean.vocabs.ui.capture.TextDrawer
import com.jean.vocabs.ui.capture.rememberQuickCapture
import com.jean.vocabs.ui.card.CardScreen
import com.jean.vocabs.ui.components.BAR_HEIGHT
import com.jean.vocabs.ui.components.AppIcons
import com.jean.vocabs.ui.components.BottomBar
import com.jean.vocabs.ui.components.Motion
import com.jean.vocabs.ui.components.Tab
import com.jean.vocabs.ui.home.HomeScreen
import com.jean.vocabs.ui.languages.NewLanguageScreen
import com.jean.vocabs.ui.languages.languageOf
import com.jean.vocabs.ui.pending.PendingScreen
import com.jean.vocabs.ui.pending.PendingViewModel
import com.jean.vocabs.ui.pending.UndoStrip
import com.jean.vocabs.ui.profile.ProfileScreen
import com.jean.vocabs.ui.progress.DayByDayScreen
import com.jean.vocabs.ui.progress.ProgressScreen
import com.jean.vocabs.ui.progress.WhatsLeftScreen
import com.jean.vocabs.ui.review.ReviewScreen
import com.jean.vocabs.ui.saved.SavedScreen
import com.jean.vocabs.ui.select.SelectScreen
import com.jean.vocabs.ui.settings.SettingsScreen
import com.jean.vocabs.ui.words.WordsScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private object Routes {
    // The argument names are shared by the route pattern and the read that
    // follows it. Spelled out twice they are two string literals nothing checks:
    // change one and the read returns null at runtime.
    const val ARG_ID = "id"
    const val ARG_IDS = "ids"
    const val ARG_TARGET = "target"

    const val HOME = "home"
    const val WORDS = "words"
    const val PENDING = "pending"
    const val PROFILE = "profile"
    const val REVIEW = "review"
    const val CARD = "card/{$ARG_ID}"
    const val SELECT = "select/{$ARG_ID}"
    const val SAVED = "saved/{$ARG_IDS}"

    // Páginas de dentro: abrem por cima da aba, com voltar no topo. A barra
    // continua visível e marcando a aba de origem — é o que faz "Seu progresso"
    // parecer parte da Você e não um lugar novo.
    const val PROGRESS = "progress/{$ARG_TARGET}"
    const val DAY_BY_DAY = "day-by-day/{$ARG_TARGET}"
    const val WHATS_LEFT = "whats-left/{$ARG_TARGET}"
    const val SETTINGS = "settings"
    const val NEW_LANGUAGE = "new-language"
    const val NATIVE_LANGUAGE = "native-language"

    fun card(id: Long) = "card/$id"
    fun select(id: Long) = "select/$id"
    fun saved(ids: List<Long>) = "saved/${ids.joinToString(ID_SEPARATOR)}"
    fun progress(target: String) = "progress/$target"
    fun dayByDay(target: String) = "day-by-day/$target"
    fun whatsLeft(target: String) = "whats-left/$target"

    val fullScreen = setOf(REVIEW, CARD, SELECT, SAVED, NEW_LANGUAGE, NATIVE_LANGUAGE)

    private val innerPages = mapOf(
        PROGRESS to PROFILE,
        DAY_BY_DAY to PROFILE,
        WHATS_LEFT to PROFILE,
        SETTINGS to PROFILE,
    )

    /** Que aba a barra de baixo acende para esta rota. */
    fun tabFor(route: String?): String? = route?.let { innerPages[it] ?: it }
}

/** Vírgula não aparece em id nenhum, e o argumento de rota não precisa ser escapado. */
private const val ID_SEPARATOR = ","

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabsApp() {
    val nav = rememberNavController()
    val stack by nav.currentBackStackEntryAsState()
    val route = stack?.destination?.route
    val scope = rememberCoroutineScope()

    // A mesma instância que a tela de Pendentes usa, e não a dela por baixo: o
    // selo da aba e a lista precisam concordar sobre o que já foi arrastado para
    // fora, e é este ViewModel que segura a exclusão em suspenso.
    val pendingVm: PendingViewModel = viewModel()
    val captureVm: CaptureViewModel = viewModel()
    val pendingTotal by pendingVm.total.collectAsStateWithLifecycle()
    val pendingDeletion by pendingVm.deletion.collectAsStateWithLifecycle()
    val capture by captureVm.state.collectAsStateWithLifecycle()

    val barVisible = route !in Routes.fullScreen
    val currentTab = Routes.tabFor(route)

    // A gaveta é estado, e não destino de navegação: ela precisa poder abrir por
    // cima de qualquer aba sem empurrar nada para a pilha, e o voltar do sistema
    // deve fechá-la — que é o que o `ModalBottomSheet` já faz.
    var drawerOpen by remember { mutableStateOf(false) }
    val drawerState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    /**
     * O aviso de baixo, um de cada vez.
     *
     * Capturas em sequência **substituem** o aviso anterior e reiniciam os 5 s,
     * em vez de empilhar cartões: quem captura três coisas seguidas não quer ler
     * três confirmações, quer o botão livre para a quarta.
     */
    var notice by remember { mutableStateOf<Notice?>(null) }

    val recording = rememberQuickCapture(
        target = capture.languagePair.target,
        onSave = { format, path, durationMs, captureTarget ->
            val key = System.nanoTime()
            notice = Notice.Saved(key, format, durationMs, captureTarget)
            captureVm.saveMedia(format, path, durationMs, captureTarget) { id ->
                // O id chega do banco alguns milissegundos depois. Só entra no
                // cartão se ele ainda for o mesmo — uma captura seguinte já pode
                // ter tomado o lugar dele.
                val current = notice
                if (current is Notice.Saved && current.key == key) notice = current.copy(captureId = id)
            }
        },
        onNotice = { text -> notice = Notice.Message(System.nanoTime(), text) },
    )

    fun closeDrawer() {
        scope.launch { drawerState.hide() }.invokeOnCompletion { drawerOpen = false }
    }

    // O fundo desfoca enquanto o leque está aberto ou a gravação corre. O
    // `graphicsLayer` só entra na cadeia enquanto o gesto dura: uma camada de
    // composição sobre o `NavHost` inteiro é barata de manter por dois segundos e
    // cara de manter para sempre.
    var inGesture by remember { mutableStateOf(false) }
    var blurring by remember { mutableStateOf(false) }
    LaunchedEffect(inGesture) {
        if (inGesture) {
            blurring = true
        } else {
            delay(Motion.DEFAULT.toLong() + 80)
            blurring = false
        }
    }
    val blur = animateFloatAsState(
        targetValue = if (inGesture) 1f else 0f,
        animationSpec = tween(Motion.DEFAULT),
        label = "desfoqueDoFundo",
    )

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                // Enquanto o hub tem a tela, o que está atrás sai do alcance do
                // leitor de tela: o conteúdo continua composto debaixo do leque e
                // da gravação, e sem isto o TalkBack andaria por uma tela que a
                // pessoa não está mais vendo.
                .then(if (inGesture) Modifier.clearAndSetSemantics {} else Modifier)
                .then(
                    if (blurring && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.graphicsLayer {
                            val radius = 15.dp.toPx() * blur.value
                            renderEffect = if (radius > 0.2f) BlurEffect(radius, radius, TileMode.Decal) else null
                        }
                    } else {
                        Modifier
                    },
                ),
            // A troca de aba é um fundido com um respiro de escala: a tela nova
            // chega em 98% e assenta. Um fundido puro entre quatro telas que têm
            // o mesmo fundo e a mesma barra embaixo não se lê como troca — se lê
            // como o conteúdo sendo repintado no lugar.
            enterTransition = { fadeIn(tween(Motion.DEFAULT)) + scaleIn(tween(Motion.DEFAULT), initialScale = 0.98f) },
            exitTransition = { fadeOut(tween(Motion.FAST)) },
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onCapture = { drawerOpen = true },
                    onReview = { nav.navigate(Routes.REVIEW) },
                    onOpenProfile = { nav.goToTab(Routes.PROFILE) },
                    onAddLanguage = { nav.navigate(Routes.NEW_LANGUAGE) },
                )
            }
            composable(Routes.WORDS) {
                WordsScreen(onOpenCard = { nav.navigate(Routes.card(it)) })
            }
            composable(Routes.PENDING) {
                PendingScreen(
                    onOpenCapture = { nav.navigate(Routes.select(it.id)) },
                    onOpenCard = { nav.navigate(Routes.card(it.id)) },
                    vm = pendingVm,
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onOpenProgress = { nav.navigate(Routes.progress(it)) },
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                    onOpenNewLanguage = { nav.navigate(Routes.NEW_LANGUAGE) },
                )
            }
            composable(Routes.PROGRESS, arguments = listOf(navArgument(Routes.ARG_TARGET) { type = NavType.StringType })) { entry ->
                ProgressScreen(
                    // O alvo da rota é só por onde a tela entra: a gaveta da
                    // bandeira troca o curso olhado sem sair do destino, e é o
                    // curso de agora que as telas de dentro precisam receber.
                    target = entry.arguments?.getString(Routes.ARG_TARGET),
                    onBack = { nav.popBackStack() },
                    onOpenDayByDay = { nav.navigate(Routes.dayByDay(it)) },
                    onOpenWhatsLeft = { nav.navigate(Routes.whatsLeft(it)) },
                    onAddLanguage = { nav.navigate(Routes.NEW_LANGUAGE) },
                )
            }
            composable(Routes.DAY_BY_DAY, arguments = listOf(navArgument(Routes.ARG_TARGET) { type = NavType.StringType })) { entry ->
                DayByDayScreen(
                    target = entry.arguments?.getString(Routes.ARG_TARGET),
                    onBack = { nav.popBackStack() },
                    onOpenCard = { nav.navigate(Routes.card(it)) },
                )
            }
            composable(Routes.WHATS_LEFT, arguments = listOf(navArgument(Routes.ARG_TARGET) { type = NavType.StringType })) { entry ->
                WhatsLeftScreen(
                    target = entry.arguments?.getString(Routes.ARG_TARGET),
                    onBack = { nav.popBackStack() },
                    onOpenCard = { nav.navigate(Routes.card(it)) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { nav.popBackStack() },
                    onSwitchNativeLanguage = { nav.navigate(Routes.NATIVE_LANGUAGE) },
                )
            }
            composable(Routes.NEW_LANGUAGE, enterTransition = { up() }, popExitTransition = { down() }) {
                NewLanguageScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.NATIVE_LANGUAGE, enterTransition = { up() }, popExitTransition = { down() }) {
                NewLanguageScreen(onBack = { nav.popBackStack() }, forNative = true)
            }
            composable(Routes.REVIEW, enterTransition = { up() }, popExitTransition = { down() }) {
                ReviewScreen(onBack = { nav.popBackStack() })
            }
            composable(
                Routes.CARD,
                arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.LongType }),
                enterTransition = { up() },
                popExitTransition = { down() },
            ) { entry ->
                CardScreen(entry.arguments?.getLong(Routes.ARG_ID) ?: 0, { nav.popBackStack() })
            }
            composable(
                Routes.SELECT,
                arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.LongType }),
                enterTransition = { up() },
                popExitTransition = { down() },
            ) { entry ->
                SelectScreen(
                    id = entry.arguments?.getLong(Routes.ARG_ID) ?: 0,
                    onBack = { nav.popBackStack() },
                    onSave = { ids ->
                        // A seleção sai da pilha junto: voltar da confirmação
                        // deve levar de onde a captura começou, e não a um
                        // trecho que já virou ficha.
                        nav.navigate(Routes.saved(ids)) {
                            popUpTo(Routes.SELECT) { inclusive = true }
                        }
                    },
                )
            }
            composable(
                Routes.SAVED,
                arguments = listOf(navArgument(Routes.ARG_IDS) { type = NavType.StringType }),
                enterTransition = { up() },
                popExitTransition = { down() },
            ) { entry ->
                SavedScreen(
                    ids = entry.arguments?.getString(Routes.ARG_IDS).orEmpty()
                        .split(ID_SEPARATOR)
                        .mapNotNull(String::toLongOrNull),
                    onClose = { nav.popBackStack() },
                    onCaptureAnother = { nav.popBackStack(); drawerOpen = true },
                    onViewCards = { nav.popBackStack(); nav.goToTab(Routes.WORDS) },
                    onReview = { nav.popBackStack(); nav.navigate(Routes.REVIEW) },
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .then(if (inGesture) Modifier.clearAndSetSemantics {} else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // O aviso flutua sobre o conteúdo, encostado na barra: nada na tela
            // se move para acomodá-lo, e o indicador de página continua onde
            // estava, por baixo dele.
            NoticeStrip(
                // Some junto com a barra: nas telas cheias — Selecionar,
                // Guardado, Revisão — o rodapé já tem uma ação principal, e o
                // cartão pousaria em cima dela.
                notice = notice.takeIf { barVisible },
                onSelect = { id ->
                    notice = null
                    nav.navigate(Routes.select(id))
                },
                onExpire = { key -> if (notice?.key == key) notice = null },
                modifier = Modifier.padding(horizontal = 14.dp).padding(bottom = 8.dp),
            )

            // O desfazer da exclusão vive aqui em cima, e não dentro de
            // Pendentes, por duas razões: encostado na barra ele não briga com o
            // aviso de captura por espaço — os dois se empilham —, e trocar de
            // aba não tira a segunda chance da mão de quem acabou de arrastar
            // sem querer. Só some nas telas cheias, onde o rodapé já tem dono.
            UndoStrip(
                deletion = pendingDeletion.takeIf { barVisible },
                onUndo = pendingVm::undo,
                modifier = Modifier.padding(horizontal = 14.dp).padding(bottom = 8.dp),
            )
            AnimatedVisibility(
                visible = barVisible,
                enter = slideInVertically(tween(Motion.DEFAULT)) { it } + fadeIn(tween(Motion.DEFAULT)),
                exit = slideOutVertically(tween(Motion.FAST)) { it } + fadeOut(tween(Motion.FAST)),
            ) {
                BottomBar(
                    leftTabs = listOf(Tab(Routes.HOME, AppIcons.House, "Início"), Tab(Routes.WORDS, AppIcons.Cards, "Palavras")),
                    rightTabs = listOf(Tab(Routes.PENDING, AppIcons.Clock, "Pendentes", pendingTotal), Tab(Routes.PROFILE, AppIcons.Person, "Perfil")),
                    currentRoute = currentTab,
                    onNavigate = nav::goToTab,
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
        val barStep = with(LocalDensity.current) { BAR_HEIGHT.roundToPx() }
        AnimatedVisibility(
            visible = barVisible,
            enter = fadeIn(tween(Motion.DEFAULT)) + slideInVertically(tween(Motion.DEFAULT)) { barStep },
            exit = fadeOut(tween(Motion.FAST)) + slideOutVertically(tween(Motion.FAST)) { barStep },
            modifier = Modifier.fillMaxSize(),
        ) {
            CaptureHub(
                capture = recording,
                language = languageOf(capture.languagePair.target),
                onOpenText = { drawerOpen = true },
                onPhaseChange = { inGesture = it },
            )
        }
    }

    if (drawerOpen) {
        ModalBottomSheet(
            onDismissRequest = { drawerOpen = false },
            sheetState = drawerState,
        ) {
            TextDrawer(
                onSave = { snippet ->
                    captureVm.saveSnippet(snippet, capture.languagePair.target) { id ->
                        closeDrawer()
                        nav.navigate(Routes.select(id))
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
private fun up() =
    slideInVertically(tween(Motion.DEFAULT, easing = FastOutSlowInEasing)) { it / 5 } + fadeIn(tween(Motion.DEFAULT))

private fun down() =
    slideOutVertically(tween(Motion.FAST)) { it / 5 } + fadeOut(tween(Motion.FAST))

private fun NavHostController.goToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
