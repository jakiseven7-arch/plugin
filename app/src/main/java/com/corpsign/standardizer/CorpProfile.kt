package com.corpsign.standardizer

/**
 * Perfil corporativo central (a "Sede" em Ararangua, SC, Brasil).
 * Todos os metadados de rede, geolocalizacao e sistema derivam daqui,
 * garantindo identidade consistente para auditoria interna.
 */
object CorpProfile {
    // --- Localizacao fisica (sede) ---
    const val CITY = "Araranguá"
    const val STATE = "Santa Catarina"
    const val COUNTRY = "Brasil"
    const val LATITUDE = -28.9309
    const val LONGITUDE = -49.0891
    const val ALTITUDE = 10.0
    const val TIMEZONE = "America/Sao_Paulo"
    const val LANGUAGE = "pt-BR"

    // --- Rede corporativa (VPS em Ararangua) ---
    // IP publico do VPS que sera a origem de todo o trafego de saida.
    const val CORPORATE_IP = "200.150.1.10"
    const val VPS_HOST = "200.150.1.10"
    const val VPS_SOCKS_PORT = 1080

    // --- User-Agent corporativo (Chrome Mobile / Samsung Galaxy S23) ---
    const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; SM-S911B) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.6099.230 Mobile Safari/537.36"

    // --- Metadados de dispositivo padrao ---
    const val DEVICE_MODEL = "SM-S911B"
    const val DEVICE_BRAND = "samsung"
    const val SCREEN_WIDTH = 390
    const val SCREEN_HEIGHT = 844
    const val DEVICE_PIXEL_RATIO = 3.0

    // --- Alvos de padronizacao ---
    const val CONTRACT_URL = "https://p.bancopan.com.br/ei4qUf4"
    const val INSPECTION_URL = "https://p.bancopan.com.br/W5lbHfb"

    // Nome do provedor de mock location registrado no LocationManager
    const val MOCK_PROVIDER_NAME = "CorpMockProvider"
}