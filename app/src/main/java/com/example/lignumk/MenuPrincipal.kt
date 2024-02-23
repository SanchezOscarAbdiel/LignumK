package com.example.lignumk

import android.os.Bundle
import androidx.activity.ComponentActivity
//Clases
import ConexionFirebase

// Importar la clase Context
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.work.WorkManager


val cFirebase = ConexionFirebase()

enum class ProviderType{
    GOOGLE
}

class MenuPrincipal : ComponentActivity() {

    private lateinit var workManager: WorkManager
    lateinit var tvTit: TextView
    lateinit var tvDescripcion: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workManager = WorkManager.getInstance(applicationContext)
        setContentView(R.layout.activity_menu_principal)
        VerificaPrimeraVez()
        tvTit = findViewById(R.id.tvTitulo)
        tvDescripcion = findViewById(R.id.tvDescripcion)


        val email =""
        val provider = ""

    //Guardado de datos
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
        prefs.putString("email", email)
        prefs.putString("provider", provider)
}

    override fun onResume() {
        super.onResume()
        val sharedPref = getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
        // Recuperar el texto de la variable elemento usando la misma clave
        val descripcion = sharedPref.getString("descripcion", "")
        val titulo = sharedPref.getString("titulo", "")
        // Asignar el texto al editText
        tvDescripcion.text = descripcion
        tvTit.text = titulo
    }

    fun VerificaPrimeraVez(){
        val sharedPref = getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        val FIRST_RUN = "first_run"
        val firstRun = sharedPref.getBoolean(FIRST_RUN, true)
        if (firstRun) {
            val intent = Intent(this, PrimeraVez::class.java)
            startActivity(intent)
            editor.putBoolean(FIRST_RUN, false)
            editor.apply()
        }
    }
}



