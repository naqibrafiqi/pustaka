package my.naqibrafiqi.pustaka.ui.feature.settings

import my.naqibrafiqi.pustaka.ui.feature.settings.screen.about.reflowLicence
import org.junit.Assert.assertEquals
import org.junit.Test

class ReflowLicenceTest {

    @Test
    fun joinsWrappedLinesButKeepsParagraphsAndListItems() {
        val text = """
            BSD 3-Clause License

            Redistribution and use in source and binary forms, with or without
            modification, are permitted provided that:

            * Redistributions of source code must retain the above
              copyright notice.
            * Neither the name of the copyright holder
              nor its contributors may be used.
        """.trimIndent().replace("\n", "\r\n")

        assertEquals(
            "BSD 3-Clause License\n\n" +
                "Redistribution and use in source and binary forms, with or without modification, are permitted provided that:\n\n" +
                "* Redistributions of source code must retain the above copyright notice.\n" +
                "* Neither the name of the copyright holder nor its contributors may be used.",
            reflowLicence(text),
        )
    }
}
