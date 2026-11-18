package my.naqibrafiqi.pustaka.data.library.epub

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageStylesTest {

    @Test
    fun theScriptGoesAtTheEndOfTheHead() {
        val html = """<html><head><title>Chapter 1</title></HEAD><body><p>Text</p></body></html>"""

        val injected = String(PageStyles.inject(html.toByteArray()))

        val script = injected.indexOf("<script")
        assertTrue(script > injected.indexOf("</title>"))
        assertTrue(script < injected.indexOf("</HEAD>"))
        assertEquals(html, injected.replaceRange(script, injected.indexOf("</HEAD>"), ""))
    }

    @Test
    fun theScriptStaysValidXhtmlAndAddsNoStylesReadiumWouldSee() {
        val injected = String(PageStyles.inject("<head></head>".toByteArray()))
        val body = injected.substringAfter("<script type=\"text/javascript\">").substringBefore("</script>")

        assertFalse(body.contains('<'))
        assertFalse(body.contains('&'))
        // Readium skips its default styles when a chapter has any of these.
        assertFalse(injected.contains("<style", ignoreCase = true))
        assertFalse(injected.contains("<link", ignoreCase = true))
        assertFalse(injected.contains(" style="))
    }

    @Test
    fun theScriptCentresIllustrationsAndReportsLongPressesOnThem() {
        val injected = String(PageStyles.inject("<head></head>".toByteArray()))

        assertTrue(injected.contains("[data-pustaka-illustration] { display: block; margin-left: auto !important; margin-right: auto !important; }"))
        assertTrue(injected.contains("addEventListener('contextmenu'"))
        assertTrue(injected.contains("window.${PageStyles.IMAGES_INTERFACE}.open(picture.src, picture.alt || '')"))
    }

    @Test
    fun textOutsideAsciiIsKeptAsItIs() {
        val html = "<head><title>Café — 第一章</title></head><body>…</body>"

        val injected = String(PageStyles.inject(html.toByteArray(Charsets.UTF_8)), Charsets.UTF_8)

        assertTrue(injected.startsWith("<head><title>Café — 第一章</title>"))
        assertTrue(injected.endsWith("</head><body>…</body>"))
    }

    @Test
    fun aPageWithoutAHeadIsLeftAlone() {
        val html = "<p>No head</p>".toByteArray()
        assertArrayEquals(html, PageStyles.inject(html))
    }
}
