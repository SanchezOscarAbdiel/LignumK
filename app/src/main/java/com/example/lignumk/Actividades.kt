package com.example.lignumk

import ConexionFirebase
import WorkManagerFile
import android.util.Log
import org.json.JSONArray
import java.io.File
import java.io.FileReader
import kotlin.random.Random
import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

val cFirebaseA = ConexionFirebase()

class Actividades{

    fun leeArchivo(contexto: Context, nombre: String): JSONArray {
        val archivo = File(contexto.getExternalFilesDir(null), "$nombre.json")
        val archivoLector = FileReader(archivo)
        val contenido = archivoLector.readText()

        return JSONArray(contenido)
    }

    fun AsignarTareas(contexto: Context) {
        val json = leeArchivo(contexto, "Tareas")

        // Generar un número aleatorio entre 0 y el tamaño del arreglo menos uno
        val indice = Random.nextInt(0, json.length())
        // Obtener el elemento del arreglo json usando el índice
        val elemento = json.getJSONObject(indice)
        // Hacer algo con el elemento, por ejemplo, imprimirlo
        //Log.d("AsignarTareas", "Elemento al azar en LeerTareas${elemento.get("descripcion")}")
        cFirebaseA.LeerDatos("Tareas", "tipo", "diaria", contexto)

        val sharedPref = contexto.getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
        // Obtener un editor de las SharedPreferences
        val editor = sharedPref.edit()
        // Guardar el texto de la variable elemento como un valor asociado a una clave
        editor.putString("descripcion", elemento.get("descripcion").toString())
        editor.putString("titulo", elemento.get("titulo").toString())
        val randomInt = (0..1000).random() // Generates a random integer between 0 and 10 (inclusive)
        editor.putString("Nrandom", randomInt.toString())
        // Guardar los cambios en el archivo
        Log.d("AsignarTareas", "Tareas asignadas")
        editor.apply()

    }

    fun oneTimeR(contexto: Context, delay: Long,para: String){
        lateinit var workManager: WorkManager
        workManager = WorkManager.getInstance(contexto)
        Log.d("Parametro", "Parametro oneTimeR: ${para}")
        val miOneTimeWorkRequest = OneTimeWorkRequest.Builder(WorkManagerFile::class.java)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(false).build())
            .setInputData(Data.Builder().putString("parametro", para).build())
            .setInitialDelay(delay, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(contexto).enqueue(miOneTimeWorkRequest)

    }

    fun SincronizaTareas(): Long {
        val currentTime = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) // Hora actual
        val desiredHour = 16 // Hora deseada (7 AM)
        val desiredMinute = 8 // Minuto deseado (35 minutos)
        // Calcula la diferencia en milisegundos hasta la próxima ejecución
        val delay = TimeUnit.HOURS.toMillis(((0 + desiredHour - currentTime) % 24).toLong()) +
                TimeUnit.MINUTES.toMillis((desiredMinute - Calendar.getInstance().get(Calendar.MINUTE)).toLong())

        Log.d("SincronizaTareas", "El delay es: $delay")
        return delay
    }


    fun periodicRTareas(contexto: Context){
        val miPeriodicWorkRequest = PeriodicWorkRequest.Builder(WorkManagerFile::class.java, 15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(Data.Builder().putString("parametro", "AsignarTareas").build())
            .build()
        WorkManager.getInstance(contexto).enqueue(miPeriodicWorkRequest)
    }

    fun periodicTimeR(contexto: Context, delay: Long,para: String){
        val miPeriodicWorkRequest = PeriodicWorkRequest.Builder(WorkManagerFile::class.java, delay, TimeUnit.SECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(Data.Builder().putString("parametro", para).build())
            .build()
        WorkManager.getInstance(contexto).enqueue(miPeriodicWorkRequest)
    }

    fun EscribirTareas() {
        // Aquí puedes escribir el código para escribir las tareas
    }

    fun BorrarTareas() {
        // Aquí puedes escribir el código para borrar las tareas
    }

}