package org.junit.jupiter.api

/**
 * Compatibility shim for contract tests only — DO NOT move to main source.
 *
 * The JUnit 5 Kotlin API (`org.junit.jupiter.api.assertThrows`) does not expose a
 * `(Class<T>, () -> Unit)` overload; its reified form requires `assertThrows<T> { }`.
 * The immutable contract test ColorPlanesTest uses the Java-style call
 * `assertThrows(Foo::class.java) { }` with the Kotlin import, so this bridge adds
 * the missing overload to the same package so the import resolves correctly.
 */
inline fun <reified T : Throwable> assertThrows(type: Class<T>, noinline executable: () -> Unit): T =
    Assertions.assertThrows(type, executable)
