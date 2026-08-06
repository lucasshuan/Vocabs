package io.github.lucasshuan.vocabu.shared

import io.github.lucasshuan.vocabu.shared.domain.CourseSummary
import io.github.lucasshuan.vocabu.shared.domain.VocabRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * The list comes from the preference, not the database: built from cards, a
 * language just chosen would vanish the instant after. Courses with none enter
 * at zero, which is what the empty state draws.
 */
fun enrolledCourses(
    repository: VocabRepository,
    preferences: Preferences,
): Flow<List<CourseSummary>> = combine(
    preferences.observeCourses(),
    repository.observeCourses(),
) { enrolled, withCards ->
    enrolled.map { target ->
        withCards.firstOrNull { it.target == target } ?: CourseSummary(target, total = 0, mastered = 0)
    }
}
