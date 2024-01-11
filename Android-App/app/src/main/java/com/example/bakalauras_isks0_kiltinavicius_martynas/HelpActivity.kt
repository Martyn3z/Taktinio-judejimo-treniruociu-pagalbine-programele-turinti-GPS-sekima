// HelpActivity.kt
package com.example.bakalauras_isks0_kiltinavicius_martynas

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat

class HelpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        val helpText = """
    <h1>Sveiki atvykę į programėlės pagalbos ir DUK puslapį</h1>
    <p>Čia rasite naudingą informaciją, kuri padės jums suprasti ir sėkmingai naudotis mūsų programėle.</p>

    <h2>DUK (Dažnai Užduodami Klausimai)</h2>
    <h3>1. Kaip prisijungti prie sesijos?</h3>
    <p>Atidarykite programėlę ir įveskite savo slapyvardį, pasirinkite klasę ir įrašykite sesijos kodą. Jeigu neturite sesijos kodo, paklauskite jo organizatoriaus.</p>

    <h3>2. Kaip kurti naują sesiją?</h3>
    <p>Pasirinkite "Sukurti arba prisijungti" ir nurodykite naują sesijos kodą. Po to pasirinkite komandą ir patvirtinkite pasirinkimą.</p>

    <h3>3. Kaip rasti vietą žemėlapyje?</h3>
    <p>Žemėlapio viršuje galite naudoti paieškos laukelį ir jame įvesti koordinates, taip pat jeigu norite sužinoti vietovės koordinates, ant jos tiesiog paspauskite ir koordinatės iššoks paieškos lauke.</p>

    <h3>4. Kaip peržiūrėti sesijos dalyvio informaciją?</h3>
    <p>Žemėlapyje paspauskite ant naudotojo žymos, ir atsiras informacija apie jo duomenis.</p>

    <h2>Kontaktai</h2>
    <p>Turite klausimų arba pasiūlymų? Susisiekite el. paštu: martynas.kiltinavicius@knf.stud.vu.lt.</p>

    <h2>Dėkojame, kad naudojatės mūsų programėle!</h2>
    """.trimIndent()

        val textView = findViewById<TextView>(R.id.help_text)
        textView.text = HtmlCompat.fromHtml(helpText, HtmlCompat.FROM_HTML_MODE_COMPACT)
    }
}
