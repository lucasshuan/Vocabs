package io.github.lucasshuan.vocabu.shared

import android.os.Build

/** Exists only to pick the local server's address; see `AppContainer.baseUrl`. */
object Device {

    /**
     * On `ro.hardware`. The `Build.BRAND`/`FINGERPRINT` starts-with-"generic"
     * recipe no longer works: a current emulator reports brand `google` and a
     * fingerprint starting `google/`, and passes as physical.
     *
     * `goldfish` is the old emulator, `ranchu` the current one, `vbox86`
     * Genymotion, `gce_x86`/`cutf` the cloud emulators CI runs on.
     */
    val isEmulator: Boolean by lazy {
        Build.HARDWARE.lowercase() in EMULATED_HARDWARE ||
            // Net for vendor emulators off the list; the product name gives them away.
            Build.PRODUCT.lowercase().let { product ->
                product.startsWith("sdk") || product.contains("emulator")
            }
    }

    private val EMULATED_HARDWARE = setOf("goldfish", "ranchu", "vbox86", "gce_x86", "cutf")
}
