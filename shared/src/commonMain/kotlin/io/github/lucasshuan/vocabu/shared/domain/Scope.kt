package io.github.lucasshuan.vocabu.shared.domain

/**
 * Which slice of the database a read sees.
 *
 * Three slices coexist since language stopped cutting the whole app: Home shows
 * one course per page, Words and Pending show all of them, and "Your progress"
 * shows a course that is not necessarily the open one.
 *
 * A defaulted parameter rather than three families of methods, so forgetting to
 * choose lands on the active course — the right answer on most screens.
 */
sealed interface Scope {

    data object ActiveCourse : Scope

    /** A named course, open or not, so "Your progress · French" can exist without switching. */
    data class Course(val target: String) : Scope

    data object All : Scope
}
