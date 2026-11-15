package moye.wear.wraive.data

import android.graphics.Bitmap
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import moye.wear.wraive.model.AttachmentKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.UUID

class AttachmentReaderTest {
    @Test
    fun localFilesKeepTheirMetadataAndReadAsTextOrImages() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.cacheDir, "attachments-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            val reader = AttachmentReader(context)
            val text = File(directory, "notes.txt").apply { writeText("本地文件内容") }
            val attachment = reader.read(Uri.fromFile(text)).single()
            assertEquals(AttachmentKind.TEXT, attachment.kind)
            assertEquals(text.name, attachment.name)
            assertEquals("text/plain", attachment.mimeType)
            assertEquals(text.length(), attachment.sizeBytes)
            assertEquals("本地文件内容", attachment.extractedText)

            val image = File(directory, "photo.png")
            val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
            image.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            val photo = reader.read(Uri.fromFile(image)).single()
            assertEquals(AttachmentKind.IMAGE, photo.kind)
            assertEquals(image.name, photo.name)
            assertTrue(photo.uri.startsWith("data:image/jpeg;base64,"))
        } finally {
            directory.deleteRecursively()
        }
    }
}
