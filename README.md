# CorpSign Standardizer

Aplicacao Android (Kotlin) que cria um "Container de Navegacao Padronizado"
para a equipe de operacoes, garantindo identidade geografica e de rede
consistente com a sede em Ararangua, SC, Brasil.

## Como o roteamento SOCKS5 garante a consistencia de IP para a auditoria

1. O VPS corporativo (em Ararangua) executa um servidor SOCKS5 (Dante) na
   porta 1080 (ver `setup_vps.sh` / `dante.conf`).
2. No Android, o `WebViewActivity` cria um `OkHttpClient` configurado com
   `Proxy(Proxy.Type.SOCKS, ...)` apontando para o VPS.
3. O `WebViewClient.shouldInterceptRequest` captura toda requisicao HTTP/HTTPS
   emitida pelo WebView (incluindo assets, XHR, Service Workers quando
   possivel) e a reexecuta via OkHttp, que tunela o trafego pelo SOCKS5.
4. Como a conexao TCP de saida e estabelecida pelo VPS, o IP de origem
   observado por qualquer servidor remoto (e por servicos de geolocalizacao
   de IP) e o IP publico do VPS em Ararangua.
5. Combinado com a injecao de `corp_metadata.js` (que mascara geolocalizacao,
   idioma, fuso horario e metadados de dispositivo) e com o `CorpGeoService`
   (que injeta coordenadas da sede no LocationManager), o "fingerprint" do
   documento gerado fica 100% alinhado com o `CorpProfile`.

## Estrutura
- `app/build.gradle`, `settings.gradle`, `build.gradle`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/corpsign/standardizer/CorpProfile.kt`
- `app/src/main/java/com/corpsign/standardizer/CorpGeoService.kt`
- `app/src/main/java/com/corpsign/standardizer/MainActivity.kt`
- `app/src/main/java/com/corpsign/standardizer/WebViewActivity.kt`
- `app/src/main/assets/js/corp_metadata.js`
- `setup_vps.sh`, `dante.conf`

## Observacoes
- Requer Android 8.0+ (API 26), sem root/device owner.
- O usuario deve selecionar o app como "mock location app" nas Opcoes do
  Desenvolvedor; o `CorpGeoService` monitora essa configuracao e guia o
  usuario ate a tela correspondente.