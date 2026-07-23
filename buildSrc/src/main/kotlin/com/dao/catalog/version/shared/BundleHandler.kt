package com.dao.catalog.version.shared

import org.jetbrains.annotations.NotNull

fun ConfigField.put(key: String, value: Map<String, Map<String, String>>) {
    invoke(
        "java.util.Map<$NON_NULL String, java.util.Map<$NON_NULL String, $NON_NULL String>>",
        key, value.asString()
    )
}

private fun Map<String, Map<String, String>>.asString(): String {
    return "\njava.util.Map.of(\n" +
            entries.joinToString(separator = ",\n") { (k, v) ->
                "${INDENT}\"$k\",${INDENT}${v.asString()}"
            } + ")"
}
