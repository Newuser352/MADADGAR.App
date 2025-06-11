pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Add jcenter() for legacy libraries if needed
        // jcenter() // Warning: this repository is going to shut down
        
        // Maven repositories for specific libraries if needed
        maven { url = uri("https://jitpack.io") } // For libraries hosted on JitPack
    }
    
    // Enable version catalog
    versionCatalogs {
        create("libs")
    }
}

rootProject.name = "MADADGAR App"
include(":app")
