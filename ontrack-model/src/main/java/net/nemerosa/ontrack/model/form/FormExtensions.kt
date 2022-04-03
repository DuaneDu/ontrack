package net.nemerosa.ontrack.model.form

import net.nemerosa.ontrack.model.annotations.getPropertyDescription
import net.nemerosa.ontrack.model.annotations.getPropertyLabel
import kotlin.reflect.KProperty1

fun <T> Form.textField(property: KProperty1<T, String?>, value: String?): Form =
    with(
        Text.of(property.name)
            .label(getPropertyLabel(property))
            .help(getPropertyDescription(property))
            .optional(property.returnType.isMarkedNullable)
            .value(value)
    )

fun <T> Form.yesNoField(property: KProperty1<T, Boolean?>, value: Boolean?): Form =
    with(
        YesNo.of(property.name)
            .label(getPropertyLabel(property))
            .help(getPropertyDescription(property))
            .optional(property.returnType.isMarkedNullable)
            .value(value)
    )

fun <T> Form.intField(property: KProperty1<T, kotlin.Int?>, value: kotlin.Int?): Form =
    with(
        Int.of(property.name)
            .label(getPropertyLabel(property))
            .help(getPropertyDescription(property))
            .optional(property.returnType.isMarkedNullable)
            .value(value)
    )

fun <T> Form.longField(property: KProperty1<T, Long>, value: Long?): Form =
    with(
        Int.of(property.name)
            .label(getPropertyLabel(property))
            .help(getPropertyDescription(property))
            .optional(property.returnType.isMarkedNullable)
            .value(value)
    )
