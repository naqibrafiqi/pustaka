package my.naqibrafiqi.pustaka.testing

import java.io.File
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Writes small but valid EPUB files for tests.
 *
 * @param metadata the inside of `<metadata>` in `content.opf`, without the `dc:identifier` (which is
 *   always added) unless [identifier] is null.
 * @param cover PNG bytes to add as the cover image, or null for no cover.
 */
fun writeEpub(
    file: File,
    metadata: String,
    identifier: String? = "urn:uuid:test-book",
    epub3: Boolean = true,
    cover: ByteArray? = null,
    chapterText: String = "It was a dark and stormy night. ".repeat(200),
): File {
    file.parentFile?.mkdirs()
    val version = if (epub3) "3.0" else "2.0"
    val coverManifest = when {
        cover == null -> ""
        epub3 -> """<item id="cover" href="cover.png" media-type="image/png" properties="cover-image"/>"""
        else -> """<item id="cover" href="cover.png" media-type="image/png"/>"""
    }
    val coverMeta = if (cover != null && !epub3) """<meta name="cover" content="cover"/>""" else ""
    val navManifest =
        if (epub3) """<item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>""" else ""
    val identifierElement = identifier?.let { """<dc:identifier id="book-id">$it</dc:identifier>""" }.orEmpty()

    val opf = """
        <?xml version="1.0" encoding="UTF-8"?>
        <package xmlns="http://www.idpf.org/2007/opf" xmlns:opf="http://www.idpf.org/2007/opf"
                 version="$version" unique-identifier="book-id">
          <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
            $identifierElement
            $metadata
            $coverMeta
          </metadata>
          <manifest>
            $navManifest
            <item id="chapter1" href="chapter1.xhtml" media-type="application/xhtml+xml"/>
            $coverManifest
          </manifest>
          <spine>
            <itemref idref="chapter1"/>
          </spine>
        </package>
    """.trimIndent().trimStart() // Multi-line metadata defeats trimIndent; the declaration must come first.

    val container = """
        <?xml version="1.0" encoding="UTF-8"?>
        <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
          <rootfiles>
            <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
          </rootfiles>
        </container>
    """.trimIndent()

    val nav = """
        <?xml version="1.0" encoding="UTF-8"?>
        <html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
          <body><nav epub:type="toc"><ol><li><a href="chapter1.xhtml">Chapter 1</a></li></ol></nav></body>
        </html>
    """.trimIndent()

    val chapter = """
        <?xml version="1.0" encoding="UTF-8"?>
        <html xmlns="http://www.w3.org/1999/xhtml"><body><p>$chapterText</p></body></html>
    """.trimIndent()

    ZipOutputStream(file.outputStream()).use { zip ->
        zip.putStored("mimetype", "application/epub+zip".toByteArray())
        zip.putDeflated("META-INF/container.xml", container.toByteArray())
        zip.putDeflated("OEBPS/content.opf", opf.toByteArray())
        if (epub3) zip.putDeflated("OEBPS/nav.xhtml", nav.toByteArray())
        zip.putDeflated("OEBPS/chapter1.xhtml", chapter.toByteArray())
        if (cover != null) zip.putDeflated("OEBPS/cover.png", cover)
    }
    return file
}

/** The EPUB specification wants `mimetype` first and uncompressed. */
private fun ZipOutputStream.putStored(name: String, bytes: ByteArray) {
    val entry = ZipEntry(name).apply {
        method = ZipEntry.STORED
        size = bytes.size.toLong()
        compressedSize = bytes.size.toLong()
        crc = CRC32().apply { update(bytes) }.value
    }
    putNextEntry(entry)
    write(bytes)
    closeEntry()
}

private fun ZipOutputStream.putDeflated(name: String, bytes: ByteArray) {
    putNextEntry(ZipEntry(name))
    write(bytes)
    closeEntry()
}
