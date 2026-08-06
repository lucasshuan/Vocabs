package io.github.lucasshuan.vocabu.ui

import androidx.compose.ui.res.stringResource
import io.github.lucasshuan.vocabu.R
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
import io.github.lucasshuan.vocabu.ui.capture.CaptureHub
import io.github.lucasshuan.vocabu.ui.capture.CaptureViewModel
import io.github.lucasshuan.vocabu.ui.capture.Notice
import io.github.lucasshuan.vocabu.ui.capture.NoticeStrip
import io.github.lucasshuan.vocabu.ui.capture.TextDrawer
import io.github.lucasshuan.vocabu.ui.capture.rememberQuickCapture
import io.github.lucasshuan.vocabu.ui.card.CardScreen
import io.github.lucasshuan.vocabu.ui.components.BAR_HEIGHT
import io.github.lucasshuan.vocabu.ui.components.AppIcons
import io.github.lucasshuan.vocabu.ui.components.BottomBar
import io.github.lucasshuan.vocabu.ui.components.Motion
import io.github.lucasshuan.vocabu.ui.components.Tab
import io.github.lucasshuan.vocabu.ui.home.HomeScreen
import io.github.lucasshuan.vocabu.ui.languages.NewLanguageScreen
import io.github.lucasshuan.vocabu.ui.languages.languageOf
import io.github.lucasshuan.vocabu.ui.pending.PendingScreen
import io.github.lucasshuan.vocabu.ui.pending.PendingViewModel
import io.github.lucasshuan.vocabu.ui.pending.UndoStrip
import io.github.lucasshuan.vocabu.ui.profile.ProfileScreen
import io.github.lucasshuan.vocabu.ui.progress.DayByDayScreen
import io.github.lucasshuan.vocabu.ui.progress.ProgressScreen
import io.github.lucasshuan.vocabu.ui.progress.WhatsLeftScreen
import io.github.lucasshuan.vocabu.ui.review.ReviewScreen
import io.github.lucasshuan.vocabu.ui.saved.SavedScreen
import io.github.lucasshuan.vocabu.ui.select.SelectScreen
import io.github.lucasshuan.vocabu.ui.settings.SettingsScreen
import io.github.lucasshuan.vocabu.ui.words.WordsScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private object Routes {
    // Shared by the route pattern and the read that follows it. Spelled out
    // twice, changing one makes the read return null at runtime.
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

    // Open over a tab: the bar stays visible and keeps marking the tab they came
    // from, so "Your progress" reads as part of Profile.
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

    // The same instance Pending uses: it holds the suspended deletion, and the
    // badge and the list have to agree on what was already swiped away.
    val pendingVm: PendingViewModel = viewModel()
    val captureVm: CaptureViewModel = viewModel()
    val pendingTotal by pendingVm.total.collectAsStateWithLifecycle()
    val pendingDeletion by pendingVm.deletion.collectAsStateWithLifecycle()
    val capture by captureVm.state.collectAsStateWithLifecycle()

    val barVisible = route !in Routes.fullScreen
    val currentTab = Routes.tabFor(route)

    // State, not a destination: it opens over any tab without pushing onto the
    // stack, and system back must close it.
    var drawerOpen by remember { mutableStateOf(false) }
    val drawerState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // One at a time, replaced rather than stacked: three captures in a row want
    // the button free for the fourth, not three confirmations.
    var notice by remember { mutableStateOf<Notice?>(null) }

    val recording = rememberQuickCapture(
        target = capture.languagePair.target,
        onSave = { format, path, durationMs, captureTarget ->
            val key = System.nanoTime()
            notice = Notice.Saved(key, format, durationMs, captureTarget)
            captureVm.saveMedia(format, path, durationMs, captureTarget) { id ->
                // The id lands a few millis later, and only on the same card.
                val current = notice
                if (current is Notice.Saved && current.key == key) notice = current.copy(captureId = id)
            }
        },
        onNotice = { text -> notice = Notice.Message(System.nanoTime(), text) },
    )

    fun closeDrawer() {
        scope.launch { drawerState.hide() }.invokeOnCompletion { drawerOpen = false }
    }

    // The `graphicsLayer` joins the chain only for the gesture: a composition
    // layer over the whole `NavHost` is cheap for two seconds, not forever.
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
        label = "backgroundBlur",
    )

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                // The content stays composed under the fan and the recording, so
                // while the hub owns the screen it leaves the reader's reach.
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
            // Four screens sharing a background and a bar make a pure crossfade
            // read as repainting in place, so the new one arrives at 98%.
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
                    // Only how the screen was entered: the flag drawer changes
                    // the course looked at without leaving the destination.
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
                        // Selection leaves the stack too: back from the
                        // confirmation leads where the capture started.
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
                // Goes with the bar: the full screens' footers already carry a
                // primary action, and the card would land on top of it.
                notice = notice.takeIf { barVisible },
                onSelect = { id ->
                    notice = null
                    nav.navigate(Routes.select(id))
                },
                onExpire = { key -> if (notice?.key == key) notice = null },
                modifier = Modifier.padding(horizontal = 14.dp).padding(bottom = 8.dp),
            )

            // Here, not inside Pending: switching tabs would otherwise take the
            // second chance away from someone who just swiped by accident.
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
                    leftTabs = listOf(Tab(Routes.HOME, AppIcons.House, stringResource(R.string.tab_home)), Tab(Routes.WORDS, AppIcons.Cards, stringResource(R.string.tab_words))),
                    rightTabs = listOf(Tab(Routes.PENDING, AppIcons.Clock, stringResource(R.string.tab_pending), pendingTotal), Tab(Routes.PROFILE, AppIcons.Person, stringResource(R.string.tab_profile))),
                    currentRoute = currentTab,
                    onNavigate = nav::goToTab,
                )
            }
        }

        // Slides by the bar's height, not the screen's: outside a gesture the
        // only visible part is the `+`, docked to the bar it travels with.
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
 * Entrances outlast exits, here and everywhere: opening means staying, and
 * whoever closes has already decided to leave.
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
