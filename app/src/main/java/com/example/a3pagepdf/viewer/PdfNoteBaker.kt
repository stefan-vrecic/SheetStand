package com.example.a3pagepdf.viewer

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

// Matches PdfPageSource's own on-screen render scale, for a consistent
// pixel budget between what you see live and what gets exported.
private const val RENDER_SCALE = 2

// Sized relative to page width rather than a fixed pixel value — source
// PDFs vary a lot in native page size, and a fixed size would look tiny on
// a large page or oversized on a small one.
private const val BASE_NOTE_FONT_FRACTION = 0.016f
private const val NOTE_PADDING_FRACTION = 0.008f
private const val NOTE_CORNER_RADIUS_FRACTION = 0.004f

/**
 * "Bakes" placed [PageNote]s permanently into a fresh copy of a PDF — the
 * motivating case being reading the result on a Kindle, which obviously
 * can't render this app's overlay. Built on [PdfRenderer] + [PdfDocument]
 * (both built into Android) rather than a PDF-editing library: this app has
 * no other need for one, and those two together are exactly enough to
 * flatten notes onto pages and write a new file.
 *
 * The one real tradeoff: the output is image-based, not vector — every page
 * becomes one embedded bitmap, the same as what's already rendered for
 * on-screen display. For sheet music (already just notation, not searchable
 * text) that's a non-issue; it's also exactly what keeps this simple enough
 * to build without a heavyweight PDF-authoring dependency.
 */
object PdfNoteBaker {

    /**
     * Renders [uri] with [notes] burned in and writes the result under this
     * app's files dir, returning the file — or null on any failure
     * (unreadable source, no pages, write error), so callers can just show
     * "couldn't export" instead of crashing.
     */
    fun bake(context: Context, uri: Uri, outputFileName: String, notes: List<PageNote>): File? {
        val notesByPage = notes.groupBy { it.page }
        var fd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var document: PdfDocument? = null
        return try {
            fd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            val originalSizeBytes = fd.statSize
            renderer = PdfRenderer(fd)
            if (renderer.pageCount == 0) return null

            document = PdfDocument()
            for (pageIndex in 0 until renderer.pageCount) {
                val page = renderer.openPage(pageIndex)
                val width = page.width * RENDER_SCALE
                val height = page.height * RENDER_SCALE
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val pdfPage = document.startPage(
                    PdfDocument.PageInfo.Builder(width, height, pageIndex).create()
                )
                val canvas = pdfPage.canvas
                canvas.drawBitmap(bitmap, 0f, 0f, null)
                bitmap.recycle()

                notesByPage[pageIndex + 1]?.forEach { note ->
                    drawNote(canvas, note, width, height)
                }

                document.finishPage(pdfPage)
            }

            val outDir = File(context.filesDir, "exports").apply { mkdirs() }
            val outFile = File(outDir, outputFileName)
            FileOutputStream(outFile).use { document.writeTo(it) }
            Log.d(
                "PdfNoteBaker",
                "baked $outputFileName: original=${originalSizeBytes}B, exported=${outFile.length()}B " +
                    "(${renderer.pageCount} pages, ${notes.size} notes)"
            )
            outFile
        } catch (e: Exception) {
            null
        } finally {
            document?.close()
            renderer?.close()
            fd?.close()
        }
    }

    /**
     * [note]'s x/yFraction anchor its top-left corner (same convention as
     * on-screen — see PageNoteBubble), so the background rect and text
     * baseline are both positioned from that corner rather than centered.
     */
    private fun drawNote(canvas: Canvas, note: PageNote, pageWidthPx: Int, pageHeightPx: Int) {
        val padding = pageWidthPx * NOTE_PADDING_FRACTION
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = pageWidthPx * BASE_NOTE_FONT_FRACTION * note.scale
        }
        val x = note.xFraction * pageWidthPx
        val y = note.yFraction * pageHeightPx

        val textWidth = textPaint.measureText(note.text)
        val metrics = textPaint.fontMetrics
        val textHeight = metrics.descent - metrics.ascent

        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(230, 255, 245, 200)
        }
        val rect = RectF(x, y, x + textWidth + padding * 2, y + textHeight + padding * 2)
        val corner = pageWidthPx * NOTE_CORNER_RADIUS_FRACTION
        canvas.drawRoundRect(rect, corner, corner, backgroundPaint)
        canvas.drawText(note.text, x + padding, y + padding - metrics.ascent, textPaint)
    }

    /**
     * Shares [file] as a PDF attachment, routed specifically to Gmail with a
     * generic chooser fallback — same shape as HomeActivity's
     * shareLastPdfViaGmail, just pointed at a freshly-baked file (via
     * FileProvider, since sharing a raw file:// URI to another app is
     * blocked) instead of the last-opened PDF's own URI.
     */
    fun shareViaGmail(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Sharing a PDF")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.google.android.gm")
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            intent.setPackage(null)
            try {
                context.startActivity(Intent.createChooser(intent, "Share PDF"))
            } catch (e2: ActivityNotFoundException) {
                Toast.makeText(context, "No app available to share the PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
