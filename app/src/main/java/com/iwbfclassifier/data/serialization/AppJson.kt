package com.iwbfclassifier.data.serialization

import kotlinx.serialization.json.Json

/**
 * Shared JSON config. prettyPrint + encodeDefaults keep the on-disk files
 * human-inspectable (CLAUDE.md / docs/04); ignoreUnknownKeys keeps older or
 * externally-edited files loadable.
 */
val AppJson: Json = Json {
    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true
    isLenient = true
}
