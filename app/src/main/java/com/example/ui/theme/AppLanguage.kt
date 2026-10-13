package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val flag: String
) {
    FRENCH("fr", "Français", "🇫🇷"),
    ENGLISH("en", "English", "🇬🇧");

    fun tr(fr: String, en: String): String {
        return if (this == FRENCH) fr else en
    }
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.FRENCH }

@Composable
@ReadOnlyComposable
fun tr(fr: String, en: String): String {
    return LocalAppLanguage.current.tr(fr, en)
}
