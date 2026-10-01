package com.safeshield.app.core

object Constants {
    const val DATABASE_NAME = "safeshield.db"
    const val PREFS_NAME = "safeshield_settings"

    const val CHANNEL_SCAN = "scan_progress"
    const val CHANNEL_THREAT = "threat_alerts"

    const val NOTIF_ID_SCAN = 1001
    const val NOTIF_ID_THREAT_BASE = 2000

    const val WORK_SCHEDULED_SCAN = "safeshield_scheduled_scan"
    const val WORK_SIGNATURE_UPDATE = "safeshield_signature_update"
    const val WORK_APK_WATCH = "safeshield_apk_watch"

    const val BUNDLED_SIGNATURES_ASSET = "malware_signatures.json"
    const val SIGNATURE_FEED_URL =
        "https://raw.githubusercontent.com/safeshield-app/signatures/main/signatures.json"

    const val VIRUSTOTAL_BASE_URL = "https://www.virustotal.com/api/v3/"
    const val SAFE_BROWSING_BASE_URL = "https://safebrowsing.googleapis.com/"

    /** Minimum VirusTotal engine detections before we call a file malicious. */
    const val VT_MALICIOUS_THRESHOLD = 3

    const val SKU_REMOVE_ADS = "safeshield_remove_ads"
}
