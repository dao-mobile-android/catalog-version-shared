package com.dao.catalog.version.shared

import org.gradle.api.artifacts.ExternalModuleDependencyBundle
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.provider.Provider
import java.util.Optional

fun VersionCatalog.bundles(dependencies: Map<String, Pair<String, String>>): Map<String, Map<String, String>> {
    return bundleAliases
        .associateWith(::findBundle)
        .mapValues { (_, libraries) ->
            libraries
                .map(Provider<ExternalModuleDependencyBundle>::get)
                .let(Optional<ExternalModuleDependencyBundle>::get)
                .map(MinimalExternalModuleDependency::getModule)
                .map(ModuleIdentifier::toString)
                .map(dependencies::getValue)
                .toMap()
        }
}
