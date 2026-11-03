package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object PdfExporter {
    private const val TAG = "PdfExporter"
    private const val PAGE_WIDTH = 595 // A4 standard width in points (72 dpi)
    private const val PAGE_HEIGHT = 842 // A4 standard height in points (72 dpi)
    private const val MARGIN = 40f

    fun exportAndSharePdf(
        context: Context,
        title: String,
        documentType: String, // "Resume" or "Cover_Letter"
        applicantName: String,
        applicantContact: String,
        content: String
    ): Uri? {
        return try {
            val pdfDir = File(context.cacheDir, "pdfs")
            if (!pdfDir.exists()) {
                pdfDir.mkdirs()
            }

            val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30)
            val fileName = "${sanitizedTitle}_${documentType}.pdf"
            val file = File(pdfDir, fileName)

            val pdfDocument = PdfDocument()

            val textPaint = TextPaint().apply {
                color = Color.rgb(30, 41, 59)
                textSize = 10.5f
                isAntiAlias = true
            }

            val headerPaint = TextPaint().apply {
                color = Color.rgb(30, 58, 138)
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val subheaderPaint = TextPaint().apply {
                color = Color.rgb(71, 85, 105)
                textSize = 9.5f
                isAntiAlias = true
            }

            val dividerPaint = Paint().apply {
                color = Color.rgb(203, 213, 225)
                strokeWidth = 1.2f
            }

            val contentWidth = (PAGE_WIDTH - (MARGIN * 2)).toInt()

            // Break content into lines / paragraphs
            val paragraphs = content.split("\n")

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            var currentY = MARGIN

            // Draw Header on first page
            canvas.drawText(applicantName.ifBlank { "Candidate Profile" }, MARGIN, currentY + 16f, headerPaint)
            currentY += 24f

            if (applicantContact.isNotBlank()) {
                canvas.drawText(applicantContact, MARGIN, currentY + 10f, subheaderPaint)
                currentY += 16f
            }

            val docBadge = "$documentType • $title"
            canvas.drawText(docBadge, MARGIN, currentY + 10f, subheaderPaint)
            currentY += 16f

            canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, dividerPaint)
            currentY += 16f

            for (rawParagraph in paragraphs) {
                val paragraph = rawParagraph.trimEnd()
                if (paragraph.isBlank()) {
                    currentY += 8f
                    continue
                }

                // Check for Section Header style (e.g., # HEADER or ALL CAPS or Experience:)
                val isSectionHeader = paragraph.startsWith("#") ||
                        paragraph.startsWith("##") ||
                        (paragraph.length < 40 && paragraph.all { it.isUpperCase() || it.isWhitespace() || it == ':' || it == '&' })

                val paintToUse = if (isSectionHeader) {
                    TextPaint().apply {
                        color = Color.rgb(30, 58, 138)
                        textSize = 12f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        isAntiAlias = true
                    }
                } else {
                    textPaint
                }

                val cleanText = paragraph.removePrefix("## ").removePrefix("# ")

                val layout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    StaticLayout.Builder.obtain(cleanText, 0, cleanText.length, paintToUse, contentWidth)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0f, 1.15f)
                        .setIncludePad(false)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    StaticLayout(cleanText, paintToUse, contentWidth, Layout.Alignment.ALIGN_NORMAL, 1.15f, 0f, false)
                }

                val layoutHeight = layout.height

                // Check if we need a new page
                if (currentY + layoutHeight > PAGE_HEIGHT - MARGIN) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    currentY = MARGIN

                    // Page continuation header
                    val pageStr = "Page $pageNumber - $applicantName"
                    canvas.drawText(pageStr, MARGIN, currentY + 10f, subheaderPaint)
                    currentY += 18f
                    canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, dividerPaint)
                    currentY += 14f
                }

                canvas.save()
                canvas.translate(MARGIN, currentY)
                layout.draw(canvas)
                canvas.restore()

                currentY += layoutHeight + (if (isSectionHeader) 6f else 4f)
            }

            pdfDocument.finishPage(page)

            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            // Launch share intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "$documentType - $title ($applicantName)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Download or Share $documentType PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

            Toast.makeText(context, "$documentType PDF generated successfully!", Toast.LENGTH_SHORT).show()
            uri
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export PDF", e)
            Toast.makeText(context, "Failed to generate PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            null
        }
    }
}
