package com.dao.catalog.version.shared

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import java.util.SortedMap

fun Project.generateCatalogVersions(buildConfigField: ConfigField) {
    val plugins: SortedMap<String, String> = sortedMapOf()
    val libraries: SortedMap<String, String> = sortedMapOf()
    val bundles: SortedMap<String, Map<String, String>> = sortedMapOf()

    extensions.getByType<VersionCatalogsExtension>().forEach { catalog ->
        val dependencies = configurations.detachedConfiguration()
            .apply { resolveDependencies(catalog) }
            .run { catalog.resolvedDependencies() }

        plugins.putAll(catalog.plugins())
        libraries.putAll(dependencies.values.toMap())
        bundles.putAll(catalog.bundles(dependencies))
    }

    buildConfigField.put("PLUGINS", plugins)
    buildConfigField.put("LIBRARIES", libraries)
    buildConfigField.put("BUNDLES", bundles)
}
