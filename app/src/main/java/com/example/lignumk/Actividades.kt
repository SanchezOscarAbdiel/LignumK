package com.example.lignumk

import ConexionFirebase
import WorkManagerFile
import android.app.AlertDialog
import android.util.Log
import org.json.JSONArray
import java.io.File
import java.io.FileReader
import kotlin.random.Random
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.core.view.isVisible
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.content
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

import java.io.FileNotFoundException
import java.util.Calendar
import java.util.concurrent.TimeUnit

val cFirebaseA = ConexionFirebase()
val cPrimeraVez = PrimeraVez()
val cMenuPrincipal = MenuPrincipal()

class Actividades{

    fun leeArchivo(contexto: Context, nombre: String): JSONArray {
        val archivo = File(contexto.getExternalFilesDir(null), "$nombre.json")
        val archivoLector = FileReader(archivo)
        val contenido = archivoLector.readText()
        return JSONArray(contenido)
    }

    suspend fun samAItexto(contexto: Context,titulo: String, descripcion: String, respuesta: String,puntos:String) {
        val generativeModel = GenerativeModel(
            modelName = "gemini-pro",
            apiKey = "AIzaSyDo5BH4jyyrGS28OIpMTdpTL-Zx3oVGKbI"
        )

        val Prompt = "A un trabajador de una empresa madedera se le asignó una actividad que lleva por titulo: '${titulo}' teniendo que hacer lo siguiente:'${descripcion}'. esta fue su respuesta: '${respuesta}'. puntua su respuesta con un rango de 0 a '${puntos}' (escribe asi: -x/${puntos}-) y escribe retroalimentacion corta acerca del tema"
        val response = generativeModel.generateContent(Prompt)

        response.text?.let { popRetroalimentacion(contexto, it, titulo) }
    }

    suspend fun samAIimagen(contexto: Context, titulo: String){

        val generativeModel = GenerativeModel(
            // Use a model that's applicable for your use case (see "Implement basic use cases" below)
            modelName = "gemini-pro-vision",
            // Access your API key as a Build Configuration variable (see "Set up your API key" above)
            apiKey = "AIzaSyDo5BH4jyyrGS28OIpMTdpTL-Zx3oVGKbI"
        )
        val sharedPref = contexto.getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
        val uriString = sharedPref.getString("fotoTarea", null)
        if (uriString != null) {
            val uri = Uri.parse(uriString)
            val inputStream = contexto.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            // Ahora, puedes usar el Bitmap. Por ejemplo:
            // myImageView.setImageBitmap(bitmap)

            val image1: Bitmap = bitmap

            val inputContent = content {
                image(image1)
                text("What's different between these pictures?")
            }

            val response = generativeModel.generateContent(inputContent)
            response.text?.let {
                popRetroalimentacion(contexto, it, titulo)
            }
        }

    }



    fun AsignarTareas(contexto: Context) {
        try {
            val json = leeArchivo(contexto, "Tareas")

            // Generar un número aleatorio entre 0 y el tamaño del arreglo menos uno
            val indice = Random.nextInt(0, json.length())
            // Obtener el elemento del arreglo json usando el índice
            val elemento = json.getJSONObject(indice)
            // Hacer algo con el elemento, por ejemplo, imprimirlo
            cFirebaseA.LeerDatos("Tareas", "tipo", "diaria", contexto)

            val sharedPref = contexto.getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
            // Obtener un editor de las SharedPreferences
            val editor = sharedPref.edit()
            // Guardar el texto de la variable elemento como un valor asociado a una clave
            editor.putString("descripcion", elemento.get("descripcion").toString())
            editor.putString("titulo", elemento.get("titulo").toString())
            editor.putString("subtipo", elemento.get("subtipo").toString())
            editor.putString("puntos", elemento.get("puntos").toString())
            // Guardar los cambios en el archivo
            Log.d("AsignarTareas", "Tareas asignadas")
            editor.apply()
        } catch (e: FileNotFoundException) {
            Log.d("TAG", "Archivo no encontrado, reintentando en 5 segundos", e)
            Handler(Looper.getMainLooper()).postDelayed({
                // Reintentar AsignarTareas después de 5 segundos
                AsignarTareas(contexto)
            }, 5000)
        }
    }
    fun popEscritura(contexto: Context, titulo: String, descripcion: String, puntos: String){
        val editText = EditText(contexto)
        val dialog =MaterialAlertDialogBuilder(contexto)
            .setTitle(titulo)
            .setMessage(descripcion)
            .setView(editText)
            .setNeutralButton("Cancelar") { dialog, which ->

            }
            .setPositiveButton("Aceptar") { dialog, which ->
                val inputText = editText.text.toString()

                CoroutineScope(Dispatchers.Main).launch {
                    samAItexto(contexto,titulo, descripcion, inputText, puntos)
                }
            }
            .show()

        // Establecer un TextWatcher en el EditText
        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Habilitar el botón de acción positiva solo si el EditText no está vacío
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = !s.isNullOrEmpty()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

// Mostrar el dialogo
        dialog.show()

// Deshabilitar inicialmente el botón de acción positiva
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
    }

    fun popRetroalimentacion(contexto: Context,result: String, titulo: String) {
        val dialog =MaterialAlertDialogBuilder(contexto)
            .setTitle(titulo)
            .setMessage(result)
            .setPositiveButton("Reclamar puntos") { dialog, which ->
                //Marcar como completada la actividad
                val regex = Regex("-\\d+/\\d+-")
                val matchResult = regex.find(result)
                val score = matchResult?.value

// Ahora, score es "-10/15-", puedes procesarlo más para obtener solo el número
                val number = score?.substring(1, score.indexOf("/"))  // Esto debería dar "10"
                if (number != null)
                    actualizaActividad(contexto,number)
            }
            .show()
    }

    fun actualizaActividad(contexto: Context,puntos: String){
        val IvCargaCircular = (contexto as MenuPrincipal).findViewById<CircularProgressIndicator>(R.id.CargaCircular)
        IvCargaCircular.isVisible = true
        val sharedPref = contexto.getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
        var uid = sharedPref.getString("UID", "")

        val json = actividadesMP.leeArchivo(contexto,"Usuarios")
        var objetoBuscado: JSONObject? = null
        var monedas = 0

        for (i in 0 until json.length()) {
            val objeto = json.getJSONObject(i)
            if (objeto.getString("UID") == uid) {
                objetoBuscado = objeto
                break
            }
        }
        if (objetoBuscado != null) {
            Log.d("Objeto","Entra a objeto buscado")
            monedas = objetoBuscado.getInt("monedas") + puntos.toInt()
        } else {
            Log.d("MiApp", "No se encontró el usuario")
        }


        val jsonObject = JSONObject()
        jsonObject.put("coleccion", "Usuarios")
        jsonObject.put("documento", uid)
        jsonObject.put("monedas",monedas)

        val jsonDatos = jsonObject.toString()

        cFirebaseA.UpdateData(jsonDatos)

        cFirebaseA.LeerDatos("Usuarios","Puesto","Empleado",contexto)

        if (uid != null)
            cMenuPrincipal.aux(contexto,uid)

    }

    fun popImagen(contexto: Context, titulo: String, descripcion: String){
        val inflater = LayoutInflater.from(contexto)
        val view = inflater.inflate(R.layout.activity_dialog_foto, null)
        var texto = view.findViewById<EditText>(R.id.myEditText)

// Crear el dialogo
        val dialog = MaterialAlertDialogBuilder(contexto)
            .setTitle(titulo)
            .setMessage(descripcion)
            .setView(view)  // Agregar el layout al dialogo
            .setPositiveButton("Aceptar") { dialog, which ->

            }
            .setNegativeButton("Cancelar", null)
            .show()

        texto.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Habilitar el botón de acción positiva solo si el EditText no está vacío
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = !s.isNullOrEmpty()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

// Mostrar el dialogo
        dialog.show()

// Deshabilitar inicialmente el botón de acción positiva
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
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