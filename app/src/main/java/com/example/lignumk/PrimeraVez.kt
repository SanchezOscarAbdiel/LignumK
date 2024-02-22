package com.example.lignumk

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.lignumk.databinding.ActivityPrimeraVezBinding
import androidx.work.Constraints
import WorkManagerFile
import android.content.Intent
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

class PrimeraVez : AppCompatActivity() {
val actividades = Actividades()

lateinit var tvA: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_primera_vez)
        tvA = findViewById(R.id.tvAviso)

        //DescargaArchivos
        tvA.text ="Descargando archivos..."
        cFirebase.LeerDatos("Tareas", "tipo", "diaria", this)
        Thread.sleep(2_000)

        //Inicia sesion

        //Dias de descanso

        //AsignaTareas
        val delay = actividades.SincronizaTareas()
        actividades.oneTimeR(applicationContext,15,"AsignarTareas") //Se lee el archivo y se extrae la tarea en el momento
        tvA.text ="Asignando primera tareas"
        Thread.sleep(2_000)
        actividades.oneTimeR(applicationContext,delay,"AsignarTareas") //Se lee el archivo y se extrae la a las 7 am
        tvA.text ="Asignando Ciclos"
        actividades.oneTimeR(applicationContext,delay,"EstablecerCiclo") //Se establece el ciclo una vez que haya pasado el primer delay a las 7 de la mañana
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }


}

/*
* Hacer consultas de tareas
* (Dos veces por si tiene que descargar el archivo)
* Calcular workmanager para 7 am
* Rellenar datos
*   Guardar dia actual
*   Dia de descanso
*   Datos personales
*
* Iniciar sesion con google
* */