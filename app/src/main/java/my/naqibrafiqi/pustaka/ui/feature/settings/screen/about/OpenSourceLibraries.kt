package my.naqibrafiqi.pustaka.ui.feature.settings.screen.about

import androidx.annotation.RawRes
import my.naqibrafiqi.pustaka.R

/** A licence the app ships the full text of. */
enum class Licence(val title: String, @param:RawRes val textRes: Int) {
    APACHE_2_0("Apache License 2.0", R.raw.licence_apache_2_0),
    BSD_3_CLAUSE_READIUM("BSD 3-Clause License", R.raw.licence_bsd_3_readium),
    MIT_JSOUP("MIT License", R.raw.licence_mit_jsoup),
}

/** A library, or a family of libraries, that Pustaka includes. */
data class OpenSourceLibrary(
    val name: String,
    val author: String,
    val url: String,
    val licence: Licence,
    /** What it's for, or which parts are included. */
    val description: String,
)

/**
 * Everything in the app's release build, grouped by project. Kept by hand (AboutLibraries, which
 * would generate it, needs a newer Kotlin than the project uses). When a dependency is added,
 * check `./gradlew :app:dependencies --configuration releaseRuntimeClasspath` and add its project
 * here if it isn't covered yet.
 */
val OpenSourceLibraries: List<OpenSourceLibrary> = listOf(
    OpenSourceLibrary(
        name = "Readium Kotlin Toolkit",
        author = "Readium Foundation",
        url = "https://github.com/readium/kotlin-toolkit",
        licence = Licence.BSD_3_CLAUSE_READIUM,
        description = "Opens EPUB files, reads their details and displays them in the reader.",
    ),
    OpenSourceLibrary(
        name = "Android Jetpack (AndroidX)",
        author = "The Android Open Source Project",
        url = "https://developer.android.com/jetpack/androidx",
        licence = Licence.APACHE_2_0,
        description = "Compose, Material 3, Navigation 3, Room, DataStore, WorkManager, Lifecycle, " +
            "AppCompat, Fragment, Media3, WebKit and the other androidx libraries.",
    ),
    OpenSourceLibrary(
        name = "Kotlin and kotlinx",
        author = "JetBrains",
        url = "https://kotlinlang.org",
        licence = Licence.APACHE_2_0,
        description = "The Kotlin standard library, coroutines, serialization and JetBrains annotations.",
    ),
    OpenSourceLibrary(
        name = "Compose Multiplatform runtime",
        author = "JetBrains",
        url = "https://github.com/JetBrains/compose-multiplatform",
        licence = Licence.APACHE_2_0,
        description = "Shared Compose and Lifecycle pieces used by Navigation 3.",
    ),
    OpenSourceLibrary(
        name = "Dagger and Hilt",
        author = "Google",
        url = "https://dagger.dev",
        licence = Licence.APACHE_2_0,
        description = "Dependency injection.",
    ),
    OpenSourceLibrary(
        name = "Guava",
        author = "Google",
        url = "https://github.com/google/guava",
        licence = Licence.APACHE_2_0,
        description = "Used by AndroidX Media3, which Readium uses.",
    ),
    OpenSourceLibrary(
        name = "Accompanist",
        author = "Google",
        url = "https://github.com/google/accompanist",
        licence = Licence.APACHE_2_0,
        description = "Drawing images, used by Coil.",
    ),
    OpenSourceLibrary(
        name = "Material Symbols",
        author = "Google",
        url = "https://github.com/google/material-design-icons",
        licence = Licence.APACHE_2_0,
        description = "The icons.",
    ),
    OpenSourceLibrary(
        name = "Coil",
        author = "Coil Contributors",
        url = "https://github.com/coil-kt/coil",
        licence = Licence.APACHE_2_0,
        description = "Loads book covers.",
    ),
    OpenSourceLibrary(
        name = "OkHttp and Okio",
        author = "Square, Inc.",
        url = "https://square.github.io/okhttp/",
        licence = Licence.APACHE_2_0,
        description = "Used by Coil.",
    ),
    OpenSourceLibrary(
        name = "Timber",
        author = "Jake Wharton",
        url = "https://github.com/JakeWharton/timber",
        licence = Licence.APACHE_2_0,
        description = "Logging, used by Readium.",
    ),
    OpenSourceLibrary(
        name = "jsoup",
        author = "Jonathan Hedley",
        url = "https://jsoup.org",
        licence = Licence.MIT_JSOUP,
        description = "Reads HTML, used by Readium.",
    ),
    OpenSourceLibrary(
        name = "Koi",
        author = "Xiaoke Zhang",
        url = "https://github.com/mcxiaoke/kotlin-koi",
        licence = Licence.APACHE_2_0,
        description = "Kotlin helpers, used by Readium.",
    ),
    OpenSourceLibrary(
        name = "JSpecify, JSR 305 and javax.inject",
        author = "The JSpecify, FindBugs and JSR 330 authors",
        url = "https://jspecify.dev",
        licence = Licence.APACHE_2_0,
        description = "Annotations used by other libraries.",
    ),
)
