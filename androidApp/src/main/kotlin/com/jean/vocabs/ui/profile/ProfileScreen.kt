package com.jean.vocabs.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jean.vocabs.shared.domain.CourseSummary
import com.jean.vocabs.ui.components.AiUsageRow
import com.jean.vocabs.ui.components.AppIcons
import com.jean.vocabs.ui.components.CircularFlag
import com.jean.vocabs.ui.components.DashedBox
import com.jean.vocabs.ui.components.IconDisc
import com.jean.vocabs.ui.components.ListRow
import com.jean.vocabs.ui.components.RowChevron
import com.jean.vocabs.ui.components.ScreenCard
import com.jean.vocabs.ui.components.SectionLabel
import com.jean.vocabs.ui.components.animatedCount
import com.jean.vocabs.ui.components.animatedFraction
import com.jean.vocabs.ui.displayName
import com.jean.vocabs.ui.languages.languageOf

/**
 * Tela 07 do handoff — "Você".
 *
 * Total antes da quebra: sequência e estoque são hábito, e hábito não pertence a
 * um curso. Cada linha de idioma abre o "Seu progresso" **daquele** curso, com
 * semana, quota e estoque próprios — e sem trocar o curso aberto por baixo de
 * quem só queria olhar.
 *
 * A troca de idioma saiu daqui: ela agora é o deslize da Início. O idioma-base
 * saiu depois, para Configurações — ele não é sobre curso nenhum, e ao pé de uma
 * lista em que cada linha abre um curso ele lia-se como mais uma delas. O que
 * sobrou desse assunto nesta tela é adicionar e remover.
 */
@Composable
fun ProfileScreen(
    onOpenProgress: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNewLanguage: () -> Unit,
    vm: ProfileViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Text("Você", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 22.dp))

        ScreenCard(filling = PaddingValues(16.dp), modifier = Modifier.fillMaxWidth()) {
            Text("No total", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "somando todos os idiomas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Os três contam do zero. São o balanço do hábito inteiro, somando
            // todos os idiomas, e é a única tela do app em que esses números são
            // o assunto e não um detalhe de apoio — aqui a contagem é o conteúdo.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
                SummaryNumber(
                    value = "${animatedCount(state.totalMastered, "masteredTotal")}",
                    label = if (state.totalMastered == 1) "dominada" else "dominadas",
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f),
                )
                VerticalDivider()
                SummaryNumber(
                    value = "${animatedCount(state.dayStreak, "totalDayStreak")}",
                    label = if (state.dayStreak == 1) "dia seguido" else "dias seguidos",
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                VerticalDivider()
                SummaryNumber(
                    value = "${animatedCount(state.totalCards, "cardsTotal")}",
                    label = if (state.totalCards == 1) "ficha" else "fichas",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        ProgressByLanguage(
            courses = state.courses,
            onOpen = onOpenProgress,
            onAdd = onOpenNewLanguage,
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        AiUsageRow(used = state.aiUsage.used, limit = state.aiUsage.limit)

        // O subtexto não é enfeite: o idioma-base era uma linha desta tela e agora
        // está um toque mais fundo. Sem citá-lo aqui, quem o procura onde ele
        // estava não tem nenhuma pista de para onde olhar.
        ListRow(
            title = "Configurações",
            detail = "meu idioma, tema e meus dados",
            onClick = onOpenSettings,
            start = { IconDisc(AppIcons.Gear, null, color = MaterialTheme.colorScheme.onSurfaceVariant, background = MaterialTheme.colorScheme.surfaceVariant) },
            end = { RowChevron() },
        )
        Spacer(Modifier.navigationBarsPadding().height(110.dp))
    }
}

/**
 * A lista de cursos numa caixa com rolagem própria.
 *
 * Sem o teto de altura, quem estuda seis idiomas empurraria "Gerações por IA" e
 * "Configurações" para fora da primeira tela — e essas linhas são justamente as
 * que ninguém procura rolando, porque não mudam nunca.
 */
@Composable
private fun ProgressByLanguage(
    courses: List<CourseSummary>,
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val scrollable = courses.size > 3

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            SectionLabel("Progresso por idioma", Modifier.weight(1f))
            if (scrollable) {
                Text("role para ver", style = MaterialTheme.typography.bodySmall, color = colors.outline)
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surfaceVariant, RoundedCornerShape(22.dp))
                .heightIn(max = MAX_LIST_HEIGHT)
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
        ) {
            courses.forEach { course ->
                CourseRow(course) { onOpen(course.languagePair.target) }
            }
            DashedBox(
                modifier = Modifier.fillMaxWidth(),
                onClick = onAdd,
                filling = PaddingValues(horizontal = 15.dp, vertical = 11.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(30.dp).background(colors.surface, CircleShape),
                    ) {
                        Icon(AppIcons.Plus, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = "Adicionar idioma",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.primary,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CourseRow(course: CourseSummary, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val fraction by animatedFraction(
        target = if (course.total == 0) 0f else course.mastered.toFloat() / course.total,
        label = "fracaoDoCurso",
    )

    ListRow(
        onClick = onClick,
        start = { CircularFlag(languageOf(course.languagePair.target), size = 30.dp) },
        end = { RowChevron() },
    ) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = languageOf(course.languagePair.target).displayName,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${course.mastered} de ${course.total}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 5.dp)
                .height(6.dp)
                .background(colors.outlineVariant, CircleShape),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .background(colors.tertiary, CircleShape),
            )
        }
    }
}

@Composable
private fun SummaryNumber(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = color)
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun VerticalDivider() {
    Box(Modifier.width(1.dp).height(34.dp).background(MaterialTheme.colorScheme.outlineVariant))
}

/** Três linhas e meia: a meia diz que há mais, e o rodapé de conta continua na tela. */
private val MAX_LIST_HEIGHT = 232.dp
