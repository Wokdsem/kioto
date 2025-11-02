package com.wokdsem.kioto

/**
 * Declares a context key that can be used to provide a value in a [NodeContext].
 *
 * @param T The type of the value that can be provided.
 *
 * @see contextKeyOf
 * @see ProvidedValue
 */
public class ProvidableContext<T> internal constructor()

/**
 * Create a [ProvidableContext] instance. This is used to provide values in a [NodeContext].
 *
 * @param T The type of the value that can be provided.
 * @return A [ProvidableContext] instance for the specified type.
 *
 * @see ProvidedValue
 * @see ProvidableContext
 */
public fun <T> contextKeyOf(): ProvidableContext<T> = ProvidableContext()

/**
 * Set of provided values that a [NodeNav] uses to provide context to [Node]s.
 * Use [context] to create an instance of this class.
 *
 * @see context
 */
public class NodeContext internal constructor(
    internal val values: Array<out ProvidedValue<*>>
)

/**
 * Create a [NodeContext] with the provided values. This is used to provide context to [Node]s in a [NodeNav].
 *
 * @param values The values to be provided in the context.
 * @return A [NodeContext] instance containing the provided values.
 *
 * @see ProvidedValue
 */
public fun context(vararg values: ProvidedValue<*>): NodeContext = NodeContext(values)

/**
 * Holds an association between a [ProvidableContext] and a factory function that provides a value of type [T].
 * Use [provides] function to create an instance of this class.
 *
 * @see ProvidableContext
 * @see provides
 */
public class ProvidedValue<T> internal constructor(
    internal val providableContext: ProvidableContext<T>,
    internal val factory: () -> T
)

/**
 * Associates a [ProvidableContext] with a factory function that provides a value of type [T].
 *
 * @param T The type of the value that can be provided.
 * @param factory A function that provides a value of type [T].
 * @return A [ProvidedValue] instance that holds the association between the context and the factory function.
 *
 * @see ProvidableContext
 */
public infix fun <T> ProvidableContext<T>.provides(factory: () -> T): ProvidedValue<T> = ProvidedValue(providableContext = this, factory = factory)
