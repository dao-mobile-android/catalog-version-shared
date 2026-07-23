import java.util.Optional

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

//private val dependencies = immutableMapBuilder {
//    project.extensions.getByType<VersionCatalogsExtension>()
//        .forEach { catalog ->
//            val configuration = project.configurations.detachedConfiguration()
//
//            catalog.libraryAliases.forEach { alias ->
//                catalog.findLibrary(alias).ifPresent { provider ->
//                    configuration.dependencies.add(
//                        if (alias.endsWith(".bom")) {
//                            project.dependencies.platform(provider.get())
//                        } else {
//                            project.dependencies.create(provider.get())
//                        }
//                    )
//                }
//            }
//
//            val resolved = configuration.resolvedConfiguration.lenientConfiguration
//                .allModuleDependencies.associateBy { it.module.id.module.toString() }
//
//            put(
//                "PLUGINS",
//                catalog.pluginAliases
//                    .asSequence()
//                    .map(catalog::findPlugin)
//                    .map(Optional<Provider<PluginDependency>>::get)
//                    .map(Provider<PluginDependency>::get)
//                    .map(PluginDependency::toString)
//                    .sorted()
//                    .toList()
//            )
//
//            put(
//                "LIBRARIES",
//                catalog.libraryAliases
//                    .asSequence()
//                    .map(catalog::findLibrary)
//                    .filter(Optional<*>::isPresent)
//                    .map(Optional<Provider<MinimalExternalModuleDependency>>::get)
//                    .map(Provider<MinimalExternalModuleDependency>::get)
//                    .mapNotNull { dependency -> resolved[dependency.module.toString()] }
//                    .map(ResolvedDependency::getName)
//                    .sorted()
//                    .toList()
//            )
//
//            put(
//                "BUNDLES",
//                catalog.bundleAliases
//                    .asSequence()
//                    .map(catalog::findBundle)
//                    .map(Optional<Provider<ExternalModuleDependencyBundle>>::get)
//                    .flatMap(Provider<ExternalModuleDependencyBundle>::get)
//                    .map(MinimalExternalModuleDependency::getName)
//                    .sorted()
//                    .toList()
//            )
//        }
//}.let { dependencies ->
//    """
//        java.util.Map.of(${
//        dependencies.entries
//            .joinToString(separator = ",\n") { (key, values) ->
//                """
//                    "$key",
//                    ${values.joinToString(prefix = "java.util.List.of(\"", postfix = "\")", separator = "\", \"")}
//                """
//            }
//    })
//    """.trimIndent()
//}

private val plugins: MutableMap<String, List<String>> = mutableMapOf()
private val libraries: MutableMap<String, List<String>> = mutableMapOf()
private val bundles: MutableMap<String, Map<String, List<String>>> = mutableMapOf()

private fun Map<String, List<String>>.asString(): String {
    return """
        java.util.Map.of(${
        entries
            .joinToString(separator = ",\n") { (key, values) ->
                """
                "$key",
                ${
                    values.joinToString(
                        prefix = "java.util.List.of(\"",
                        postfix = "\")",
                        separator = "\", \""
                    )
                }
            """
            }
    })
    """.trimIndent()
}

private fun Map<String, Map<String, List<String>>>.asContentString(): String {
    return """
        java.util.Map.of(${
        entries
            .joinToString(separator = ",\n") { (key, values) ->
                """
                    "$key",
                    java.util.Map.of(${
                    values.entries
                        .joinToString(separator = ",\n") { (key, values) ->
                            """
                                "$key",
                                ${
                                    values.joinToString(
                                    prefix = "\"\\n",
                                    postfix = "\"",
                                    separator = "\\n",
                                    transform = { "• $it" }
                                )
                            }
                            """
                        }
                })
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

        plugins["PLUGINS"] = catalog.pluginAliases
            .asSequence()
            .map(catalog::findPlugin)
            .map(Optional<Provider<PluginDependency>>::get)
            .map(Provider<PluginDependency>::get)
            .map(PluginDependency::toString)
            .sorted()
            .toList()

        libraries["LIBRARIES"] = catalog.libraryAliases
            .asSequence()
            .map(catalog::findLibrary)
            .filter(Optional<*>::isPresent)
            .map(Optional<Provider<MinimalExternalModuleDependency>>::get)
            .map(Provider<MinimalExternalModuleDependency>::get)
            .mapNotNull { dependency -> resolved[dependency.module.toString()] }
            .map(ResolvedDependency::getName)
            .sorted()
            .toList()

        bundles["BUNDLES"] = catalog.bundleAliases.associateWith { alias ->
            catalog.findBundle(alias)
                .map(Provider<ExternalModuleDependencyBundle>::get)
                .map { bundle -> bundle.map(MinimalExternalModuleDependency::getName) }
                .map(List<String>::sorted)
                .get()
        }
    }

android {
    namespace = "com.dao.catalog.version.shared"

    defaultConfig {
        versionCode = 1

        versionName = "1.0.0"
        applicationId = "com.dao.catalog.version.shared"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

//        buildConfigField(
//            "java.util.Map<String, java.util.List<String>>",
//            "CATALOG_VERSIONS",
//            plugins.asString()
//        )

        buildConfigField(
            "java.util.Map<String, java.util.List<String>>",
            "PLUGINS",
            plugins.asString()
        )

        buildConfigField(
            "java.util.Map<String, java.util.List<String>>",
            "LIBRARIES",
            libraries.asString()
        )

        buildConfigField(
            "java.util.Map<String, java.util.Map<String, String>>",
            "BUNDLES",
            bundles.asContentString()
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
