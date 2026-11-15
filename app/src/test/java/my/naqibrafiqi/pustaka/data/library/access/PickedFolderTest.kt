package my.naqibrafiqi.pustaka.data.library.access

import org.junit.Assert.assertEquals
import org.junit.Test

class PickedFolderTest {

    private val root = "/storage/emulated/0"
    private val authority = PickedFolder.EXTERNAL_STORAGE_AUTHORITY

    @Test
    fun turnsAFolderOnInternalStorageIntoAPath() {
        assertEquals(
            PickedFolder.Path("/storage/emulated/0/Download/Pustaka"),
            PickedFolder.from(authority, "primary:Download/Pustaka", root),
        )
    }

    @Test
    fun handlesTheTopOfInternalStorageAndStraySlashes() {
        assertEquals(PickedFolder.Path(root), PickedFolder.from(authority, "primary:", "$root/"))
        assertEquals(PickedFolder.Path("$root/Books"), PickedFolder.from(authority, "primary:/Books/", root))
    }

    @Test
    fun rejectsSdCardsAndOtherApps() {
        assertEquals(PickedFolder.NotInternalStorage, PickedFolder.from(authority, "1A2B-3C4D:Books", root))
        assertEquals(PickedFolder.NotInternalStorage, PickedFolder.from("com.google.android.apps.docs.storage", "abc", root))
        assertEquals(PickedFolder.NotInternalStorage, PickedFolder.from(authority, null, root))
        assertEquals(PickedFolder.NotInternalStorage, PickedFolder.from(authority, "primary", root))
    }
}
