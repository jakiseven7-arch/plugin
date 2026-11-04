package com.corpsign.standardizer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.statusText).text =
            "Padrão Corporativo Ativo: Araranguá"

        findViewById<Button>(R.id.btnContract).setOnClickListener {
            launchStandardizedFlow(CorpProfile.CONTRACT_URL)
        }
        findViewById<Button>(R.id.btnInspection).setOnClickListener {
            launchStandardizedFlow(CorpProfile.INSPECTION_URL)
        }
    }

    private fun launchStandardizedFlow(url: String) {
        // 1. Inicia o CorpGeoService (mock location da sede)
        startService(Intent(this, CorpGeoService::class.java))

        // 2. Verificacao do mock location e guia o usuario se necessario
        // (o proprio servico monitora e abre as configuracoes)
        // 3. Abre o WebView com UA corporativo + roteamento SOCKS5 + injecao JS
        val intent = Intent(this, WebViewActivity::class.java).apply {
            putExtra(WebViewActivity.EXTRA_URL, url)
        }
        startActivity(intent)
    }
}