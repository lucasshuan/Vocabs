package io.github.lucasshuan.vocabu.shared

import android.os.Build

/**
 * Whether the app is running on an emulator or a real device. Exists for one
 * reason: the local server has different addresses in the two cases, and getting
 * it wrong gives a timeout that looks like a server problem.
 */
object Device {

    /**
     * `ro.hardware` is the reliable signal.
     *
     * The recipe that circulates — testing whether `Build.BRAND` or
     * `Build.FINGERPRINT` start with "generic" — **no longer works**. A current
     * emulator reports brand `google`, model `sdk_gphone64_x86_64` and a
     * fingerprint starting with `google/`, and would pass as physical.
     *
     * `goldfish` is the old Android emulator, `ranchu` the current one; `vbox86`
     * is Genymotion, and `gce_x86`/`cutf` are the cloud emulators used in CI.
     */
    val isEmulator: Boolean by lazy {
        Build.HARDWARE.lowercase() in EMULATED_HARDWARE ||
            // Safety net for vendor emulators not on the list: the product name
            // almost always gives them away.
            Build.PRODUCT.lowercase().let { product ->
                product.startsWith("sdk") || product.contains("emulator")
            }
    }

    private val EMULATED_HARDWARE = setOf("goldfish", "ranchu", "vbox86", "gce_x86", "cutf")
}
