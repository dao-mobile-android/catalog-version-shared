plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

private val plugins: MutableMap<String, Map<String, String>> = mutableMapOf()
private val libraries: MutableMap<String, Map<String, String>> = mutableMapOf()
private val bundles: MutableMap<String, Map<String, Map<String, String>>> = mutableMapOf()

private fun mapOfStrings(map: Map<String, String>): String {
    if (map.isEmpty()) return "java.util.Map.of()"
    val indent = "      "
    return "java.util.Map.ofEntries(\n" +
            map.entries.joinToString(separator = ",\n") { (k, v) ->
                "${indent}java.util.Map.entry(\"$k\", \"$v\")"
            } + ")"
}

private fun mapOfMaps(map: Map<String, Map<String, String>>): String {
    if (map.isEmpty()) return "java.util.Map.of()"
    val indent = "      "
    return "java.util.Map.ofEntries(\n" +
            map.entries.joinToString(separator = ",\n") { (k, v) ->
                "${indent}java.util.Map.entry(\"$k\", ${mapOfStrings(v)})"
            } + ")"
}

private fun Map<String, Map<String, String>>.asMapString(): String {
    val indent = "      "
    return "java.util.Map.of(\n" +
            entries.joinToString(separator = ",\n") { (key, map) ->
                "${indent}\"$key\",\n${indent}${mapOfStrings(map)}"
            } + ")"
}

private fun Map<String, Map<String, Map<String, String>>>.asBundleString(): String {
    val indent = "      "
    return "java.util.Map.of(\n" +
            entries.joinToString(separator = ",\n") { (key, map) ->
                "${indent}\"$key\",\n${indent}${mapOfMaps(map)}"
            } + ")"
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
            catalog.findBundle(alias).get().get().associate { dependency ->
                val aliasInCatalog = libraryAliasMap[dependency.module.toString()] ?: "unknown"
                val depString =
                    resolved[dependency.module.toString()]?.name ?: dependency.toString()
                aliasInCatalog to depString
            }.toSortedMap()
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
            "java.util.Map<String, java.util.Map<String, java.util.Map<String, String>>>",
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
