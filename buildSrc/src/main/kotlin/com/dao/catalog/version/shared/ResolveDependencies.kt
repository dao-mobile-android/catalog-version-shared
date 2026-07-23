package com.dao.catalog.version.shared

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.provider.Provider
import java.util.Optional

private fun ResolvedDependency.asString(): String {
    return module.id.module.toString()
}

context(project: Project)
fun Configuration.resolveDependencies(catalog: VersionCatalog) {
    catalog.libraryAliases.forEach { alias ->
        catalog.findLibrary(alias).ifPresent { provider ->
            dependencies.add(
                if (alias.endsWith(".bom")) {
                    project.dependencies.platform(provider.get())
                } else {
                    project.dependencies.create(provider.get())
                }
            )
        }
    }
}

context(configuration: Configuration)
fun VersionCatalog.resolvedDependencies(): Map<String, Pair<String, String>> {
    return configuration.resolvedConfiguration.lenientConfiguration
        .allModuleDependencies.associateBy(ResolvedDependency::asString)
        .let { dependencies ->
            libraryAliases
                .associate { alias ->
                    val dependency = findLibrary(alias)
                        .map(Provider<MinimalExternalModuleDependency>::get)
                        .map(MinimalExternalModuleDependency::getModule)
                        .map(ModuleIdentifier::toString)
                        .map(dependencies::getValue)
                        .let(Optional<ResolvedDependency>::get)

                    dependency.module.id.module.toString() to (alias to dependency.name)
                }
        }
}
