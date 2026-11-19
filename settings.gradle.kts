rootProject.name = "truequapp-beta"

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

include(":app")
include(":core:domain")
include(":core:common")
include(":core:designsystem")
include(":core:network")
include(":core:datastore")
include(":core:database")
include(":feature:auth")
include(":feature:catalog")
include(":feature:trade")
include(":feature:chat")
include(":feature:safezones")

