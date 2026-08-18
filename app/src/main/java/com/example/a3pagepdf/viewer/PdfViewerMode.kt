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
