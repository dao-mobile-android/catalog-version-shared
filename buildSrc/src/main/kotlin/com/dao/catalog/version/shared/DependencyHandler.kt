package com.dao.catalog.version.shared

fun ConfigField.put(key: String, value: Map<String, String>) {
    invoke(
        "java.util.Map<$NON_NULL  String, $NON_NULL String>",
        key, value.asString()
    )
}

fun Map<String, String>.asString(): String {
    return "\njava.util.Map.ofEntries(\n" +
            entries.joinToString(separator = ",\n") { (k, v) ->
                "${INDENT}java.util.Map.entry(\"$k\", \"$v\")"
            } + ")"
}
