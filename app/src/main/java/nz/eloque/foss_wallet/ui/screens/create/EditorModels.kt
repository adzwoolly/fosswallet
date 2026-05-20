package nz.eloque.foss_wallet.ui.screens.create

import com.google.zxing.BarcodeFormat

internal enum class FieldCategory {
    Header,
    Primary,
    Secondary,
    Auxiliary,
    Back,
    ;

    fun displayName(): String = name
}

internal data class BarcodeDraft(
    val message: String,
    val altText: String,
    val format: BarcodeFormat,
)

internal data class LocationDraft(
    val coords: String = "",
    val relevantText: String = "",
)

internal data class FieldDraft(
    val key: String,
    val label: String,
    val value: String,
    val category: FieldCategory,
)

internal sealed class EditorSheet {
    object Logo : EditorSheet()

    object LogoText : EditorSheet()

    data class Fields(
        val category: FieldCategory,
    ) : EditorSheet()

    object Strip : EditorSheet()

    object Thumbnail : EditorSheet()

    object Footer : EditorSheet()

    object Barcodes : EditorSheet()

    object Appearance : EditorSheet()

    object Metadata : EditorSheet()
}
