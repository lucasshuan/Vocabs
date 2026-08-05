package com.jean.vocabs.shared

import com.jean.vocabs.shared.domain.LanguagePair
import com.jean.vocabs.shared.domain.CourseSummary
import com.jean.vocabs.shared.domain.VocabRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Todos os cursos matriculados, na ordem da faixa e com o resumo de cada um.
 *
 * A lista vem da preferência, e não do que existe no banco: um curso recém
 * criado não tem palavra nenhuma, e montá-la a partir das fichas faria o idioma
 * que a pessoa acabou de escolher desaparecer no instante seguinte. Os que ainda
 * não têm ficha entram zerados, que é o que o estado vazio precisa para desenhar
 * o esqueleto sem inventar número.
 *
 * A tela Você e a gaveta de "Seu progresso" mostram a mesma lista, e lê-la de
 * dois jeitos faria os mesmos idiomas aparecerem em quantidades diferentes em
 * duas telas que abrem uma da outra.
 */
fun enrolledCourses(
    repository: VocabRepository,
    preferences: Preferences,
): Flow<List<CourseSummary>> = combine(
    preferences.observeCourses(),
    preferences.observeLanguagePair(),
    repository.observeCourses(),
) { matriculados, languagePair, comFichas ->
    matriculados.map { target ->
        val course = LanguagePair(native = languagePair.native, target = target)
        comFichas.firstOrNull { it.languagePair == course } ?: CourseSummary(course, total = 0, mastered = 0)
    }
}
