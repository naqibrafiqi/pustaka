package my.naqibrafiqi.pustaka.ui.model

import my.naqibrafiqi.pustaka.util.text.UiText

/** One-off events from a details screen's ViewModel. */
sealed interface DetailsEvent {

    /** The series or book was removed or deleted, so its screen should close. */
    data object Closed : DetailsEvent

    /** Something to tell the user, such as a file that couldn't be deleted. */
    data class Message(val text: UiText) : DetailsEvent
}
