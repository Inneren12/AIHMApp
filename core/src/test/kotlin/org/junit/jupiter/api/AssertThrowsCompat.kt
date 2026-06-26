package org.junit.jupiter.api

/** Bridges the Java-style assertThrows(Class<T>) { } call used in contract tests. */
inline fun <reified T : Throwable> assertThrows(type: Class<T>, noinline executable: () -> Unit): T =
    Assertions.assertThrows(type, executable)
