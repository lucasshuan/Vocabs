package com.jean.vocabs.shared

import com.jean.vocabs.shared.domain.ParIdiomas
import com.jean.vocabs.shared.domain.ResumoCurso
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
fun cursosMatriculados(
    repositorio: VocabRepository,
    preferencias: Preferencias,
): Flow<List<ResumoCurso>> = combine(
    preferencias.observarCursos(),
    preferencias.observarPar(),
    repositorio.observarCursos(),
) { matriculados, par, comFichas ->
    matriculados.map { alvo ->
        val curso = ParIdiomas(nativo = par.nativo, alvo = alvo)
        comFichas.firstOrNull { it.par == curso } ?: ResumoCurso(curso, total = 0, dominadas = 0)
    }
}
