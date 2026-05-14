package com.oskar.retrolauncher.system

/**
 * T1.44 — placeholder for the system flavor source set.
 *
 * This file exists so the `system` variant has at least one compilable source
 * file. When the platform keystore is absent the source set is excluded and
 * this file is never compiled.
 *
 * Future tasks (T1.45–T1.49) will add HiddenApi helpers, embedding service,
 * and platform-signed launcher bootstrapping here.
 */
object SystemScaffold {
    /** Non-null sentinel to suppress "unused" lint. */
    val tag: String = "RetroSystem"
}
