package dev.lexip.hecate.ui.setup

import android.content.Intent

// Helper to share a URL via Android Sharesheet, reused by setup components.
internal fun android.content.Context.shareSetupUrl(url: String) {
	if (url.isBlank()) return

	val sendIntent = Intent().apply {
		action = Intent.ACTION_SEND
		putExtra(Intent.EXTRA_TEXT, url)
		putExtra(Intent.EXTRA_TITLE, "Setup - Adaptive Theme")
		type = "text/plain"
	}

	val shareIntent = Intent.createChooser(sendIntent, null)
	startActivity(shareIntent)
}

