package com.dao.catalog.version.shared

import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.provider.Provider
import org.gradle.plugin.use.PluginDependency
import java.util.Optional

fun VersionCatalog.plugins(): Map<String, String> {
    return pluginAliases
        .associateWith { alias ->
            findPlugin(alias)
                .map(Provider<PluginDependency>::get)
                .map(PluginDependency::toString)
                .let(Optional<String>::get)
        }
}
