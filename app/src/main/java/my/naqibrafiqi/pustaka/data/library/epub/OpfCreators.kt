package my.naqibrafiqi.pustaka.data.library.epub

import my.naqibrafiqi.pustaka.data.model.Creator
import org.w3c.dom.Element
import java.io.File
import java.io.InputStream
import java.net.URLDecoder
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Reads the people credited in an EPUB (`dc:creator` and `dc:contributor`) with their roles.
 *
 * Readium files every `dc:creator` as an author whatever its role, so an illustrator listed as
 * `<dc:creator opf:role="ill">` would be shown as an author. This reads the roles from
 * `content.opf` directly instead:
 * - EPUB 2: the `opf:role` attribute;
 * - EPUB 3: a `<meta property="role" refines="#id">` element.
 *
 * A `dc:creator` without a role is an author ([Creator.role] is null). A `dc:contributor` without a
 * role gets the MARC code `ctb` ("contributor"), so it isn't mistaken for an author.
 */
internal object OpfCreators {

    private const val CONTAINER_PATH = "META-INF/container.xml"
    private const val DC = "http://purl.org/dc/elements/1.1/"
    private const val OPF = "http://www.idpf.org/2007/opf"
    const val ROLE_CONTRIBUTOR = "ctb"

    /** Returns null if the EPUB's `content.opf` can't be found or read. */
    fun read(epub: File): List<Creator>? = runCatching {
        ZipFile(epub).use { zip ->
            val container = zip.getEntry(CONTAINER_PATH) ?: return null
            val opfPath = zip.getInputStream(container).use(::packagePath) ?: return null
            val opf = zip.getEntry(opfPath) ?: return null
            zip.getInputStream(opf).use(::parse)
        }
    }.getOrNull()

    /** The path of the package document (`content.opf`) inside the EPUB. */
    fun packagePath(container: InputStream): String? {
        val rootFiles = parseXml(container).getElementsByTagNameNS("*", "rootfile")
        for (i in 0 until rootFiles.length) {
            val rootFile = rootFiles.item(i) as Element
            val mediaType = rootFile.getAttribute("media-type")
            val path = rootFile.getAttribute("full-path")
            if (path.isNotEmpty() && (mediaType.isEmpty() || mediaType == "application/oebps-package+xml")) {
                return URLDecoder.decode(path.replace("+", "%2B"), "UTF-8")
            }
        }
        return null
    }

    fun parse(opf: InputStream): List<Creator> {
        val document = parseXml(opf)
        val metadata = document.getElementsByTagNameNS(OPF, "metadata").item(0) as? Element
            ?: return emptyList()

        val rolesById = mutableMapOf<String, String>()
        val metas = metadata.getElementsByTagNameNS(OPF, "meta")
        for (i in 0 until metas.length) {
            val meta = metas.item(i) as Element
            if (meta.getAttribute("property") != "role") continue
            val id = meta.getAttribute("refines").removePrefix("#")
            val role = meta.textContent.trim()
            if (id.isNotEmpty() && role.isNotEmpty()) rolesById.putIfAbsent(id, role)
        }

        val creators = mutableListOf<Creator>()
        val children = metadata.childNodes
        for (i in 0 until children.length) {
            val element = children.item(i) as? Element ?: continue
            if (element.namespaceURI != DC) continue
            val isCreator = when (element.localName) {
                "creator" -> true
                "contributor" -> false
                else -> continue
            }
            val name = element.textContent.trim().replace(Regex("\\s+"), " ")
            if (name.isEmpty()) continue
            val role = element.getAttributeNS(OPF, "role").trim().ifEmpty { null }
                ?: rolesById[element.getAttribute("id")]
            creators += Creator(
                name = name,
                role = role?.lowercase() ?: if (isCreator) null else ROLE_CONTRIBUTOR,
            )
        }
        return creators.distinct()
    }

    private fun parseXml(input: InputStream) = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        isExpandEntityReferences = false
        // Not every parser supports this. Android's never fetches external entities anyway.
        runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
    }.newDocumentBuilder().parse(input)
}
