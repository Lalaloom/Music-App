package com.cameronbatten.dadmusic.entitlements

/**
 * Snapshot of the current user's feature entitlements.
 *
 * In M3 only [proEnabled] is modelled; richer fields (trial expiry, SKU,
 * acquisition timestamp) land when Play Billing joins the picture in v2.
 */
data class Entitlements(val proEnabled: Boolean)
