package com.nightwise.timewise.report

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object ReportSharer {

    private const val DIR = "reports"
    private const val FILE = "timewise-report.png"

    /** Writes the report image to the shareable cache folder. Call off the main thread. */
    fun save(context: Context, bitmap: Bitmap): Uri {
        val dir = File(context.cacheDir, DIR).apply { mkdirs() }
        val file = File(dir, FILE)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun share(context: Context, image: Uri?, text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            if (image != null) {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, image)
                clipData = ClipData.newRawUri(null, image)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
            }
        }
        context.startActivity(Intent.createChooser(send, "Share your report"))
    }
}
