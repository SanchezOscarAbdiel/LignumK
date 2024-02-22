package com.example.lignumk

import android.os.Bundle
import androidx.activity.ComponentActivity
//Clases
import ConexionFirebase
import WorkManagerFile

import android.content.SharedPreferences
// Importar la clase Context
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.TextView
import androidx.work.Constraints
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import java.util.concurrent.TimeUnit


val cFirebase = ConexionFirebase()

class MainActivity : ComponentActivity() {

    private lateinit var workManager: WorkManager
    lateinit var tvTit: TextView
    lateinit var tvDescripcion: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workManager = WorkManager.getInstance(applicationContext)
        setContentView(R.layout.activity_menu_principal)
        tvTit = findViewById(R.id.tvTitulo)
        tvDescripcion = findViewById(R.id.tvDescripcion)
        VerificaPrimeraVez()

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



