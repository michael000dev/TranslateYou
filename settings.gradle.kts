pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // used for OCR
        maven { setUrl("https://jitpack.io") }
        // AiModelHub SDK
        maven {
            url = uri("https://maven.pkg.github.com/alex-80/AiModelHub")
            credentials {
                username =
                    providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
                password =
                    providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
rootProject.name = "Translate You"
include(":app")
include(":translation-engines")
