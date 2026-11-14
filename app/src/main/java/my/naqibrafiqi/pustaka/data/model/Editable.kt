package my.naqibrafiqi.pustaka.data.model

/**
 * A detail that has a default value (read from the file or folder) and an optional edit made by
 * the user in the app. Edits are only stored in the database and are never written to the files.
 */
data class Editable<T>(val default: T, val edited: T? = null) {
    /** The value to show: the user's edit if there is one, otherwise the default. */
    val value: T get() = edited ?: default

    val isEdited: Boolean get() = edited != null
}

/**
 * Turns what the user typed into the edit that should be stored.
 *
 * An edit is only kept when it differs from the default. A blank value, or one that matches the
 * default, is stored as "no edit", so the field resets to its default and keeps following the file
 * if the file changes later.
 */
internal object EditRules {

    fun text(input: String?, default: String?): String? {
        val trimmed = input?.trim()
        return if (trimmed.isNullOrEmpty() || trimmed == default?.trim()) null else trimmed
    }

    fun list(input: List<String>?, default: List<String>?): List<String>? {
        val cleaned = input?.map { it.trim() }?.filter { it.isNotEmpty() }?.distinct()
        return if (cleaned.isNullOrEmpty() || cleaned == default) null else cleaned
    }

    fun creators(input: List<Creator>?, default: List<Creator>?): List<Creator>? {
        val cleaned = input
            ?.map { Creator(name = it.name.trim(), role = it.role?.trim()?.ifEmpty { null }) }
            ?.filter { it.name.isNotEmpty() }
            ?.distinct()
        return if (cleaned.isNullOrEmpty() || cleaned == default) null else cleaned
    }

    fun <T : Any> value(input: T?, default: T?): T? = if (input == default) null else input
}
