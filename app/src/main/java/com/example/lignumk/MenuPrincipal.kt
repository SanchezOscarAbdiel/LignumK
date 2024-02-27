package com.example.lignumk

import android.os.Bundle
import androidx.activity.ComponentActivity
//Clases
import ConexionFirebase

// Importar la clase Context
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.work.WorkManager
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar


val cFirebase = ConexionFirebase()

enum class ProviderType{
    GOOGLE
}

class MenuPrincipal : ComponentActivity() {

    private lateinit var workManager: WorkManager
    lateinit var tvTit: TextView
    lateinit var tvDescripcion: TextView
    lateinit var tvRand: TextView
    val actMenu = Actividades()
    private val PREFS_NAME = "MyPrefs"
    private val LAST_OPEN_DATE = "lastOpenDate"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workManager = WorkManager.getInstance(applicationContext)
        setContentView(R.layout.activity_menu_principal)

        tvTit = findViewById(R.id.tvTitulo)
        tvDescripcion = findViewById(R.id.tvDescripcion)
        tvRand = findViewById(R.id.TVrand)



}

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onStart() {
        super.onStart()
        VerificaPrimeraVez()
// Obtén las preferencias compartidas
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Obtiene la fecha almacenada previamente (si existe)
        val lastOpenDate = prefs.getLong(LAST_OPEN_DATE, 0)
        // Obtiene la fecha actual
        val currentDate = Calendar.getInstance().timeInMillis
        // Obtiene la fecha actual sin la hora (para comparar solo el día)
        val currentDay = Calendar.getInstance()
        currentDay.timeInMillis = currentDate
        currentDay.set(Calendar.HOUR_OF_DAY, 0)
        currentDay.set(Calendar.MINUTE, 0)
        currentDay.set(Calendar.SECOND, 0)
        currentDay.set(Calendar.MILLISECOND, 0)

        // Obtiene la fecha almacenada sin la hora
        val lastOpenDay = Calendar.getInstance()
        lastOpenDay.timeInMillis = lastOpenDate
        lastOpenDay.set(Calendar.HOUR_OF_DAY, 0)
        lastOpenDay.set(Calendar.MINUTE, 0)
        lastOpenDay.set(Calendar.SECOND, 0)
        lastOpenDay.set(Calendar.MILLISECOND, 0)
        Log.d("MiWorker", "Last: ${lastOpenDay}, current: $currentDay")
        // Compara las fechas
        val sharedPref = getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
        val FIRST_RUN = "first_run"
        val firstRun = sharedPref.getBoolean(FIRST_RUN, true)
        Log.d("MiWorker", "First run $firstRun")
        if (currentDay != lastOpenDay && firstRun == false) {
            // La aplicación se ha abierto por primera vez en el día
            // Realiza las acciones necesarias aquí
            // ...
            actMenu.AsignarTareas(this)
            // Guarda la nueva fecha 647
            prefs.edit().putLong(LAST_OPEN_DATE, currentDate).apply()
        }
        //-----------------------
        // Recuperar el texto de la variable elemento usando la misma clave
        val descripcion = sharedPref.getString("descripcion", "")
        val titulo = sharedPref.getString("titulo", "")
        val nRandom = sharedPref.getString("Nrandom", "")
        // Asignar el texto al editText Nrandom

        Log.d("MiWorker", "Shared preferences OnStart: \n${descripcion}, ${titulo}, ${nRandom}")
        tvDescripcion.text = descripcion
        tvTit.text = titulo
        tvRand.text = nRandom
    }

    fun VerificaPrimeraVez(){
        val sharedPref = getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        val FIRST_RUN = "first_run"
        val firstRun = sharedPref.getBoolean(FIRST_RUN, true)
        if (firstRun) {
            val intent = Intent(this, PrimeraVez::class.java)
            startActivity(intent)
            finish()
            editor.putBoolean(FIRST_RUN, false)
            editor.apply()
        }
    }
}



