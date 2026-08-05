package com.jean.vocabs.shared

import android.os.Build

/**
 * Onde o app está rodando — emulador ou aparelho de verdade.
 *
 * Existe por um motivo só: o servidor local tem endereços diferentes nos dois
 * casos, e errar isso dá um timeout que parece problema do servidor.
 */
object Device {

    /**
     * `ro.hardware` é o sinal confiável.
     *
     * A receita que circula por aí — testar se `Build.BRAND` ou `Build.FINGERPRINT`
     * começam com "generic" — **não funciona mais**. Um emulador atual reporta
     * brand `google`, modelo `sdk_gphone64_x86_64` e fingerprint começando em
     * `google/`, e passaria por aparelho físico.
     *
     * `goldfish` é o emulador antigo do Android, `ranchu` é o atual; `vbox86` é o
     * Genymotion e `gce_x86`/`cutf` são os emuladores de nuvem usados em CI.
     */
    val isEmulator: Boolean by lazy {
        Build.HARDWARE.lowercase() in EMULATED_HARDWARE ||
            // Rede de segurança para emuladores de fabricantes que não estão na
            // lista: o nome do produto praticamente sempre entrega.
            Build.PRODUCT.lowercase().let { produto ->
                produto.startsWith("sdk") || produto.contains("emulator")
            }
    }

    private val EMULATED_HARDWARE = setOf("goldfish", "ranchu", "vbox86", "gce_x86", "cutf")
}
