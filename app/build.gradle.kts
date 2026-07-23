import java.util.Optional

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

private val plugins: MutableMap<String, Map<String, String>> = mutableMapOf()
private val libraries: MutableMap<String, Map<String, String>> = mutableMapOf()
private val bundles: MutableMap<String, Map<String, List<String>>> = mutableMapOf()

private fun mapOfStrings(map: Map<String, String>): String {
    if (map.isEmpty()) return "java.util.Map.of()"
    return "java.util.Map.ofEntries(${
        map.entries.joinToString(separator = ",\n") { (k, v) ->
            "java.util.Map.entry(\"$k\", \"$v\")"
        }
    })"
}

private fun mapOfLists(map: Map<String, List<String>>): String {
    if (map.isEmpty()) return "java.util.Map.of()"
    return "java.util.Map.ofEntries(${
        map.entries.joinToString(separator = ",\n") { (k, v) ->
            "java.util.Map.entry(\"$k\", java.util.List.of(${v.joinToString { "\"$it\"" }}))"
        }
    })"
}

private fun Map<String, Map<String, String>>.asMapString(): String {
    return """
        java.util.Map.of(${
        entries
            .joinToString(separator = ",\n") { (key, map) ->
                """
                "$key",
                ${mapOfStrings(map)}
            """
            }
    })
    """.trimIndent()
}

private fun Map<String, Map<String, List<String>>>.asBundleString(): String {
    return """
        java.util.Map.of(${
        entries
            .joinToString(separator = ",\n") { (key, map) ->
                """
                "$key",
                ${mapOfLists(map)}
            """
            }
    })
    """.trimIndent()
}

project.extensions.getByType<VersionCatalogsExtension>()
    .forEach { catalog ->
        val configuration = project.configurations.detachedConfiguration()

        catalog.libraryAliases.forEach { alias ->
            catalog.findLibrary(alias).ifPresent { provider ->
                configuration.dependencies.add(
                    if (alias.endsWith(".bom")) {
                        project.dependencies.platform(provider.get())
                    } else {
                        project.dependencies.create(provider.get())
                    }
                )
            }
        }

        val resolved = configuration.resolvedConfiguration.lenientConfiguration
            .allModuleDependencies.associateBy { it.module.id.module.toString() }

        val libraryAliasMap = catalog.libraryAliases.associateBy { alias ->
            catalog.findLibrary(alias).get().get().module.toString()
        }

        plugins["PLUGINS"] = catalog.pluginAliases.associateWith { alias ->
            catalog.findPlugin(alias).get().get().toString()
        }.toSortedMap()

        libraries["LIBRARIES"] = catalog.libraryAliases.associateWith { alias ->
            val dependency = catalog.findLibrary(alias).get().get()
            resolved[dependency.module.toString()]?.name ?: dependency.toString()
        }.toSortedMap()

        bundles["BUNDLES"] = catalog.bundleAliases.associateWith { alias ->
            catalog.findBundle(alias).get().get().map { dependency ->
                val aliasInCatalog = libraryAliasMap[dependency.module.toString()] ?: "unknown"
                val depString = resolved[dependency.module.toString()]?.name ?: dependency.toString()
                "$aliasInCatalog|$depString"
            }.sorted()
        }.toSortedMap()
    }

android {
    namespace = "com.dao.catalog.version.shared"

    defaultConfig {
        versionCode = 1

        versionName = "1.0.0"
        applicationId = "com.dao.catalog.version.shared"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "java.util.Map<String, java.util.Map<String, String>>",
            "PLUGINS",
            plugins.asMapString()
        )

        buildConfigField(
            "java.util.Map<String, java.util.Map<String, String>>",
            "LIBRARIES",
            libraries.asMapString()
        )

        buildConfigField(
            "java.util.Map<String, java.util.Map<String, java.util.List<String>>>",
            "BUNDLES",
            bundles.asBundleString()
        )
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.android.lifecycle.runtime)

    testImplementation(libs.test.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.test.compose.ui.junit4)
    androidTestImplementation(libs.test.espresso.core)
    androidTestImplementation(libs.android.test.junit)
    debugImplementation(libs.test.compose.ui.manifest)
    debugImplementation(libs.compose.ui.tooling)
}
