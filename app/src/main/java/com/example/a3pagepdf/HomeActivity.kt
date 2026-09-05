package com.example.a3pagepdf

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a3pagepdf.viewer.EXTRA_PDF_URI
import com.example.a3pagepdf.viewer.FavoriteItem
import com.example.a3pagepdf.viewer.FavoritesGrid
import com.example.a3pagepdf.viewer.FavoritesStore
import com.example.a3pagepdf.viewer.HomeModeMenu
import com.example.a3pagepdf.viewer.ModePickerDialog
import com.example.a3pagepdf.viewer.PdfDisplayNames
import com.example.a3pagepdf.viewer.PdfViewerMode

class HomeActivity : ComponentActivity() {

    // Must match PdfPageSource's keys — that's what actually writes this value
    // (currently only Auto-Scroll mode persists "last opened PDF").
    private val prefsName = "pdf_prefs"
    private val lastUriKey = "last_pdf_uri"

    // Held here (not just inside the Composable) so addToFavorites() can push
    // updates straight into the grid without recreating the whole activity.
    private val favoritesState = mutableStateOf<List<FavoriteItem>>(emptyList())

    // Non-null while a PDF opened from *outside* the app (the system's "Open
    // with" chooser — Files, Drive, an email attachment, etc.) is waiting on
    // the user to pick which viewer mode to open it in. See the VIEW
    // intent-filter on this activity in the manifest, which is what makes
    // "PDF Studio" show up in that chooser alongside Acrobat/Samsung Notes/etc.
    private val pendingExternalUriState = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        favoritesState.value = FavoritesStore.load(this)

        if (intent.action == Intent.ACTION_VIEW) {
            pendingExternalUriState.value = intent.data
        }

        val addFavoriteLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            uri?.let { addToFavorites(it) }
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "SheetStand",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Separate from (and doesn't need to match the width of) the
                            // favourites section below.
                            HomeModeMenu(
                                onOpenTwoPage = { startActivity(Intent(this@HomeActivity, TwoPageActivity::class.java)) },
                                onOpenThreePage = { startActivity(Intent(this@HomeActivity, ThreePageActivity::class.java)) },
                                onOpenFourPage = { startActivity(Intent(this@HomeActivity, FourPageActivity::class.java)) },
                                onOpenAutoScroll = { startActivity(Intent(this@HomeActivity, AutoScrollActivity::class.java)) },
                                onShareViaGmail = { shareLastPdfViaGmail() },
                                modifier = Modifier.fillMaxWidth(0.55f)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            var favorites by favoritesState

                            // Its own coloured, centred card — visually separated from
                            // the mode-picker stack and the share button above it.
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shadowElevation = 4.dp,
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                Box(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp)) {
                                    FavoritesGrid(
                                        favorites = favorites,
                                        onOpen = { item -> openFavorite(item) },
                                        onOpenWithMode = { item, mode -> openFavoriteWithMode(item, mode) },
                                        onOpenOnceWithMode = { item, mode -> launchFavoriteActivity(item.uri, mode) },
                                        onAddClick = { addFavoriteLauncher.launch(arrayOf("application/pdf")) },
                                        onRemove = { item ->
                                            FavoritesStore.remove(this@HomeActivity, item.uri)
                                            favorites = FavoritesStore.load(this@HomeActivity)
                                        },
                                        onRename = { item, newName ->
                                            PdfDisplayNames.set(this@HomeActivity, item.uri, newName)
                                            favorites = FavoritesStore.load(this@HomeActivity)
                                        },
                                        onClearAll = {
                                            FavoritesStore.clear(this@HomeActivity)
                                            favorites = FavoritesStore.load(this@HomeActivity)
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        var pendingExternalUri by pendingExternalUriState
                        pendingExternalUri?.let { uri ->
                            ModePickerDialog(
                                onDismiss = { pendingExternalUri = null },
                                onSelect = { mode ->
                                    openExternalPdf(uri, mode)
                                    pendingExternalUri = null
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Picks up favourites toggled from a viewer's top-bar star while this
        // activity was in the background — onCreate alone won't see those.
        favoritesState.value = FavoritesStore.load(this)
    }

    /**
     * Opens a favourited PDF in whichever mode it was favourited from.
     * [item.mode] should never be null here — FavoritesGrid routes those
     * through the mode-picker dialog (-> [openFavoriteWithMode]) instead —
     * but two_page is a safe fallback if it somehow slips through.
     */
    private fun openFavorite(item: FavoriteItem) {
        launchFavoriteActivity(item.uri, item.mode ?: PdfViewerMode.TWO_PAGE)
    }

    /** Called once the user picks a mode for a favourite that didn't have one yet — remembers the choice. */
    private fun openFavoriteWithMode(item: FavoriteItem, mode: String) {
        FavoritesStore.setMode(this, item.uri, mode)
        favoritesState.value = FavoritesStore.load(this)
        launchFavoriteActivity(item.uri, mode)
    }

    /** A PDF opened via the system's "Open with" chooser, once the user's picked a mode. */
    private fun openExternalPdf(uri: Uri, mode: String) {
        launchFavoriteActivity(uri, mode)
    }

    private fun launchFavoriteActivity(uri: Uri, mode: String) {
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            // Persistable grants aren't guaranteed — a plain "Open with" VIEW
            // intent from another app often only grants a short-lived read,
            // not a persistable one (that's an SAF/OpenDocument-specific
            // concept). FLAG_GRANT_READ_URI_PERMISSION below on the *new*
            // intent covers that case: it re-extends whatever read access
            // this activity currently holds to the viewer activity being
            // launched, which otherwise gets none of it by default.
        }
        val target = when (mode) {
            PdfViewerMode.TWO_PAGE -> TwoPageActivity::class.java
            PdfViewerMode.THREE_PAGE -> ThreePageActivity::class.java
            PdfViewerMode.FOUR_PAGE -> FourPageActivity::class.java
            PdfViewerMode.AUTO_SCROLL -> AutoScrollActivity::class.java
            else -> TwoPageActivity::class.java
        }
        val intent = Intent(this, target).apply {
            putExtra(EXTRA_PDF_URI, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(intent)
    }

    private fun addToFavorites(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            // ignore — not all providers grant persistable permission
        }
        val name = FavoritesStore.queryDisplayName(this, uri)
        val added = FavoritesStore.add(this, uri, name)
        if (added) {
            favoritesState.value = FavoritesStore.load(this)
        } else {
            Toast.makeText(
                this,
                if (FavoritesStore.isFavorite(this, uri)) "Already in Favourites" else "Favourites is full",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Emails the most recently opened PDF as an attachment, routed
     * specifically to Gmail. Falls back to a generic share chooser if
     * Gmail isn't installed.
     */
    private fun shareLastPdfViaGmail() {
        val uri = getLastPdfUri()
        if (uri == null) {
            Toast.makeText(this, "No PDF has been opened yet", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Sharing a PDF")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.google.android.gm") // force Gmail specifically
        }

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Gmail isn't installed — fall back to whatever the user has for sharing/email.
            intent.setPackage(null)
            try {
                startActivity(Intent.createChooser(intent, "Share PDF"))
            } catch (e2: ActivityNotFoundException) {
                Toast.makeText(this, "No app available to share the PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getLastPdfUri(): Uri? {
        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        val uriString = prefs.getString(lastUriKey, null) ?: return null
        return Uri.parse(uriString)
    }
}
