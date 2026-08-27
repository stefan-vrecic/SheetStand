package com.example.a3pagepdf.viewer

/**
 * Stable identifiers for each PDF viewer mode. Doubles as:
 *  - the `mode` key MultiPagePdfController/PdfPageSource persist "last
 *    opened PDF" under (see [PdfPersistence]), and
 *  - the value stored on [FavoriteItem.mode], so HomeActivity knows which
 *    activity to relaunch a favourited PDF in.
 */
object PdfViewerMode {
    const val TWO_PAGE = "two_page"
    const val THREE_PAGE = "three_page"
    const val FOUR_PAGE = "four_page"
    const val AUTO_SCROLL = "auto_scroll"
}

/**
 * Label shown for each mode in a "which viewer?" picker, paired with its
 * [PdfViewerMode] value. Shared by FavoritesGrid (a favourite added without a
 * mode) and HomeActivity (a PDF opened from outside the app via "Open with")
 * — both need the exact same choice, so the list lives in one place.
 */
val MODE_OPTIONS = listOf(
    "2-Page Mode" to PdfViewerMode.TWO_PAGE,
    "3-Page Mode" to PdfViewerMode.THREE_PAGE,
    "4-Page Mode" to PdfViewerMode.FOUR_PAGE,
    "Auto-Scroll Mode" to PdfViewerMode.AUTO_SCROLL
)
