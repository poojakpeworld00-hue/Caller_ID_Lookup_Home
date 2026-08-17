package com.callerid.numberlookup.home.ui.contacts

import com.callerid.numberlookup.home.data.PersonItem

/** Top filter tabs for the contacts list. */
enum class PersonFilter { ALL, FAVORITES, RECENTS, GROUPS }

/** A row in the contacts list: an alphabetical section letter or a contact. */
sealed interface PersonRow {
    data class Header(val letter: String) : PersonRow
    data class Item(val contact: PersonItem) : PersonRow
}
