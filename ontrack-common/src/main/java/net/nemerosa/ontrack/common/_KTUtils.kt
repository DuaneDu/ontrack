package net.nemerosa.ontrack.common

import java.util.*
import kotlin.reflect.KCallable

/**
 * Creating a list by repeating an element
 */
operator fun <T> T.times(n: Int): List<T> {
    val list = mutableListOf<T>()
    repeat(n) {
        list += this
    }
    return list.toList()
}

/**
 * Combination of predicates
 */
infix fun <T> ((T) -> Boolean).and(other: (T) -> Boolean): (T) -> Boolean = { t ->
    this.invoke(t) && other.invoke(t)
}

/**
 * Creating an optional from a nullable reference
 */
fun <T> T?.asOptional(): Optional<T & Any> = Optional.ofNullable(this)

/**
 * Optional to nullable
 */
fun <T> Optional<T>.getOrNull(): T? = orElse(null)

/**
 * Converts a POJO as a map, using properties as index.
 */
fun <T : Any> T.asMap(vararg properties: KCallable<Any?>): Map<String, Any?> =
    properties.associate { property ->
        property.name to property.call()
    }

/**
 * Runs the code if the condition is met.
 */
fun <T> T.runIf(condition: Boolean, code: T.() -> T) = if (condition) {
    code(this)
} else {
    this
}
