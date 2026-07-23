package com.dao.catalog.version.shared

internal typealias ConfigField = (type: String, name: String, value: String) -> Unit

internal const val NON_NULL = "@org.jetbrains.annotations.NotNull"
internal const val INDENT = "    "
