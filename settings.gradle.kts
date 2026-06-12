pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "NeverForget"

include(":app")
include(":core:model")
include(":core:calendar")
include(":core:data")
include(":core:database")
include(":core:reminders")
include(":core:designsystem")

