package com.example.a3pagepdf.viewer

import android.content.Intent
import android.net.Uri
import android.os.Build

/** Intent extra used by HomeActivity's favourites grid to hand a PDF straight to a viewer mode. */
const val EXTRA_PDF_URI = "extra_pdf_uri"

/** Version-safe read of [EXTRA_PDF_URI] — null when this activity wasn't launched from a favourite. */
fun Intent.pdfUriExtra(): Uri? =
    if (Build.VERSION.SDK_INT >= 33) {
        getParcelableExtra(EXTRA_PDF_URI, Uri::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(EXTRA_PDF_URI)
    }
