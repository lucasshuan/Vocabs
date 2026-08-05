package com.jean.vocabs.shared

import com.jean.vocabs.shared.domain.CourseSummary
import com.jean.vocabs.shared.domain.LanguagePair
import com.jean.vocabs.shared.domain.VocabRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Every enrolled course, in strip order, with each one's summary.
 *
 * The list comes from the preference rather than from what exists in the
 * database: a newly created course has no words, and building it from the cards
 * would make the language someone just chose vanish the instant after. Ones with
 * no cards yet enter at zero, which is what the empty state needs to draw the
 * skeleton without inventing a number.
 */
fun enrolledCourses(
    repository: VocabRepository,
    preferences: Preferences,
): Flow<List<CourseSummary>> = combine(
    preferences.observeCourses(),
    preferences.observeLanguagePair(),
    repository.observeCourses(),
) { enrolled, languagePair, withCards ->
    enrolled.map { target ->
        val course = LanguagePair(native = languagePair.native, target = target)
        withCards.firstOrNull { it.languagePair == course } ?: CourseSummary(course, total = 0, mastered = 0)
    }
}
