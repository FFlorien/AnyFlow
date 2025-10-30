package extension

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension

fun Project.getLibsFromVersionCatalog(): VersionCatalog {
    return extensions
        .findByType(VersionCatalogsExtension::class.java)?.named("libs")
        ?: error("Version catalog 'libs' not found. Please ensure it is defined in your settings.gradle.kts")
}