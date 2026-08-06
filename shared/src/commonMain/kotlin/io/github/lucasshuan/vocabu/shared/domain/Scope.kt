package io.github.lucasshuan.vocabu.shared.domain

/**
 * A defaulted parameter rather than three families of methods, so forgetting to
 * choose lands on the active course — the right answer on most screens.
 */
sealed interface Scope {

    data object ActiveCourse : Scope

    /** A named course, so "Your progress · French" can exist without switching. */
    data class Course(val target: String) : Scope

    data object All : Scope
}
