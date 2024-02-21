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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workManager = WorkManager.getInstance(applicationContext)
        setContentView(R.layout.activity_menu_principal)


        val miOneTimeWorkRequest = OneTimeWorkRequest.Builder(WorkManagerFile::class.java)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .setInputData(Data.Builder().putString("parametro", "LeerTareas").build())
            .setInitialDelay(10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(applicationContext).enqueue(miOneTimeWorkRequest)

        val sharedPref = getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
// Obtener un editor de SharedPreferences
        val editor = sharedPref.edit()
// Crear una clave para guardar el estado de la primera ejecución
        val FIRST_RUN = "first_run"
// Comprobar si existe el valor de la clave en las Shared Preferences
        val firstRun = sharedPref.getBoolean(FIRST_RUN, true)
// Si el valor es true, significa que es la primera vez que se ejecuta la app
        if (firstRun) {
            val intent = Intent(this, PrimeraVez::class.java)
            startActivity(intent)
            editor.putBoolean(FIRST_RUN, false)
            editor.apply()
    }

}
}



