package com.intellica.panicshield.block

/**
 * Curated default set of Turkish banking / fintech package names. On first
 * launch, the intersection of this set with installed packages becomes the
 * user's initial "protected apps" selection. The user can add/remove any
 * app afterward, so this list only needs to be a sensible starting point —
 * it does not need to be exhaustive or kept perfectly current.
 */
object KnownBanks {
    val DEFAULT_TR: Set<String> = setOf(
        "com.ziraat.ziraatmobil",
        "com.akbank.android.apps.akbank_direkt",
        "com.garanti.cepsubesi",
        "com.isbank.mobile.bireysel",
        "com.pozitron.iscep",
        "com.ykb.android",
        "com.halkbank.mobil",
        "com.qnb.mobile",
        "finansbank.enpara",
        "com.ingbankasi.ingmobil",
        "com.denizbank.mobildeniz",
        "com.tmob.denizbank",
        "com.teb",
        "com.vakifbank.mobile",
        "com.kuveytturk.mobil",
        "tr.com.sekerbank.mobil",
        "com.papara.app",
        "com.ininal.wallet",
        "com.fastpay.fastpay",
        "com.tosla.android",
    )
}
