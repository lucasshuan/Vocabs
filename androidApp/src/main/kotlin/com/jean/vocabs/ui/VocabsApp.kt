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

    // Inner pages open over a tab. The bar stays visible and keeps marking the tab
    // they came from, which is what makes "Your progress" feel like part of
    // Profile rather than a new place.
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

    fun tabFor(route: String?): String? = route?.let { innerPages[it] ?: it }
}

/** No id contains a comma, and a route argument does not need escaping. */
private const val ID_SEPARATOR = ","

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabsApp() {
    val nav = rememberNavController()
    val stack by nav.currentBackStackEntryAsState()
    val route = stack?.destination?.route
    val scope = rememberCoroutineScope()

    // The same instance the Pending screen uses: the tab badge and the list have
    // to agree on what was already swiped away, and this ViewModel holds the
    // suspended deletion.
    val pendingVm: PendingViewModel = viewModel()
    val captureVm: CaptureViewModel = viewModel()
    val pendingTotal by pendingVm.total.collectAsStateWithLifecycle()
    val pendingDeletion by pendingVm.deletion.collectAsStateWithLifecycle()
    val capture by captureVm.state.collectAsStateWithLifecycle()

    val barVisible = route !in Routes.fullScreen
    val currentTab = Routes.tabFor(route)

    // State, not a navigation destination: it has to open over any tab without
    // pushing onto the stack, and system back must close it.
    var drawerOpen by remember { mutableStateOf(false) }
    val drawerState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    /**
     * Captures in a row **replace** the previous notice and restart the 5 s rather
     * than stacking cards: three captures in a row want the button free for the
     * fourth, not three confirmations.
     */
    var notice by remember { mutableStateOf<Notice?>(null) }

    val recording = rememberQuickCapture(
        target = capture.languagePair.target,
        onSave = { format, path, durationMs, captureTarget ->
            val key = System.nanoTime()
            notice = Notice.Saved(key, format, durationMs, captureTarget)
            captureVm.saveMedia(format, path, durationMs, captureTarget) { id ->
                // The id arrives from the database a few milliseconds later, and
                // only enters the card if that card is still the same one.
                val current = notice
                if (current is Notice.Saved && current.key == key) notice = current.copy(captureId = id)
            }
        },
        onNotice = { text -> notice = Notice.Message(System.nanoTime(), text) },
    )

    fun closeDrawer() {
        scope.launch { drawerState.hide() }.invokeOnCompletion { drawerOpen = false }
    }

    // The `graphicsLayer` only joins the chain while the gesture lasts: a
    // composition layer over the whole `NavHost` is cheap to hold for two seconds
    // and expensive to hold forever.
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
                // While the hub owns the screen, what is behind leaves the screen
                // reader's reach: the content stays composed under the fan and
                // the recording.
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
            // A pure crossfade between four screens that share a background and a
            // bottom bar does not read as a switch — it reads as the content
            // being repainted in place, so the new screen arrives at 98%.
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
                    // The route's target is only how the screen was entered: the
                    // flag drawer changes the course being looked at without
                    // leaving the destination.
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
                        // The selection leaves the stack too: back from the
                        // confirmation should lead where the capture started.
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
            NoticeStrip(
                // Gone along with the bar: on the full screens the footer already
                // has a primary action, and the card would land on top of it.
                notice = notice.takeIf { barVisible },
                onSelect = { id ->
                    notice = null
                    nav.navigate(Routes.select(id))
                },
                onExpire = { key -> if (notice?.key == key) notice = null },
                modifier = Modifier.padding(horizontal = 14.dp).padding(bottom = 8.dp),
            )

            // Undo lives here rather than inside Pending: against the bar it does
            // not fight the capture notice for space, and switching tabs does not
            // take the second chance away from someone who just swiped by accident.
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

        // The hub fills the screen and draws over everything, but intercepts no
        // touch beyond its own `+`. It slides by the bar's height rather than the
        // screen's: the only visible part outside a gesture is the `+`, which has
        // to travel with the bar it is docked to.
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
 * The entrance is deliberately longer than the exit, and it is that way
 * throughout the app: opening a screen means staying, and there is time to watch
 * it arrive; closing means the decision is made.
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
