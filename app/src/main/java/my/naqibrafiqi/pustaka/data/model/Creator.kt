package my.naqibrafiqi.pustaka.data.model

import kotlinx.serialization.Serializable

/**
 * A person credited in a book's `content.opf` (`dc:creator` or `dc:contributor`).
 *
 * @property role a MARC relator code such as `aut` or `ill`, or null when the file gives none.
 */
@Serializable
data class Creator(
    val name: String,
    val role: String? = null,
) {
    /** A creator with no role is treated as an author, as the EPUB specification suggests. */
    val isAuthor: Boolean get() = role == null || role == ROLE_AUTHOR

    companion object {
        const val ROLE_AUTHOR = "aut"
        const val ROLE_ILLUSTRATOR = "ill"
        const val ROLE_TRANSLATOR = "trl"
        const val ROLE_EDITOR = "edt"
    }
}
