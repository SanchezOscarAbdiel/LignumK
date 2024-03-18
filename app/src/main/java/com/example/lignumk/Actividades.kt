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
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.content
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

import java.io.FileNotFoundException
import java.util.Calendar
import java.util.concurrent.TimeUnit

val cFirebaseA = ConexionFirebase()
val cPrimeraVez = PrimeraVez()
val cSelectorFotos = SelectorFotos()
val cMenuPrincipal = MenuPrincipal()

class Actividades{

    fun leeArchivo(contexto: Context, nombre: String): JSONArray {
        val archivo = File(contexto.getExternalFilesDir(null), "$nombre.json")
        val archivoLector = FileReader(archivo)
        val contenido = archivoLector.readText()
        return JSONArray(contenido)
    }

    suspend fun samAItexto(contexto: Context,titulo: String, descripcion: String, respuesta: String,puntos:String,progressIndicator:LinearProgressIndicator) {
        val generativeModel = GenerativeModel(
            modelName = "gemini-pro",
            apiKey = "AIzaSyDo5BH4jyyrGS28OIpMTdpTL-Zx3oVGKbI"
        )
        progressIndicator.visibility = View.VISIBLE

        val Prompt = "A un trabajador de una empresa madedera se le asignó una actividad que lleva por titulo: " +
                "'${titulo}' teniendo que hacer lo siguiente:'${descripcion}'. esta fue su respuesta: " +
                "'${respuesta}'. puntua su respuesta con un rango de 0 a '${puntos}' (escribe asi: -x/${puntos}-) " +
                "y escribe un dato curioso corto corto acerca del tema y su respuesta"
        val response = generativeModel.generateContent(Prompt)

        response.text?.let { popRetroalimentacion(contexto, it, titulo,progressIndicator,"diaria") }
    }

    suspend fun samAIEncuesta(contexto: Context,titulo: String, descripcion: String, respuesta: String,puntos:String,progressIndicator:LinearProgressIndicator,tipo: String) {
        val generativeModel = GenerativeModel(
            modelName = "gemini-pro",
            apiKey = "AIzaSyDo5BH4jyyrGS28OIpMTdpTL-Zx3oVGKbI"
        )
        progressIndicator.visibility = View.VISIBLE

        val Prompt = "A un trabajador de una empresa madedera se le asignó una encuesta, teniendo que " +
                "responder las siguientes preguntas:'${descripcion}'. estas fueron sus respuestas: " +
                "'${respuesta}'.escribe: -${puntos}/${puntos}-) " +
                "y escribe un pequeño comentario constructivo en base a sus respuestas"
        val response = generativeModel.generateContent(Prompt)

        response.text?.let { popRetroalimentacion(contexto, it, titulo,progressIndicator,tipo) }
    }

    suspend fun samAISeleccion(contexto: Context,titulo: String, descripcion: String, respuesta: String, opcionCorrecta:String ,puntos:String,progressIndicator: LinearProgressIndicator) {
        progressIndicator.visibility = View.VISIBLE
        val generativeModel = GenerativeModel(
            modelName = "gemini-pro",
            apiKey = "AIzaSyDo5BH4jyyrGS28OIpMTdpTL-Zx3oVGKbI"
        )

        val Prompt = "A un trabajador de una empresa madedera se le asignó una actividad que lleva " +
                "por titulo: '${titulo}' teniendo que responder la siguiente pregunta de opcion multiple:" +
                "'${descripcion}'. esta fue la respuesta que selecciono: '${respuesta}', la respuesta correcta " +
                "a la pregunta es: ${opcionCorrecta}. puntua su respuesta con un rango de 0 a '${puntos}' " +
                "(escribe asi: -x/${puntos}-) y escribe un dato curioso corto acerca del tema o su respuesta " +
                "que incite el uso de equipo de seguridad a pesar de no querer"

        val response = generativeModel.generateContent(Prompt)

        response.text?.let { popRetroalimentacion(contexto, it, titulo,progressIndicator,"diaria") }
    }

    suspend fun samAIimagen(contexto: Context, titulo: String, descripcion: String,respuesta: String, puntos: String,progressIndicator: LinearProgressIndicator){
        progressIndicator.visibility = View.VISIBLE
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

            val image1: Bitmap = bitmap

            val inputContent = content {
                image(image1)
                text("A un trabajador de una empresa madedera se le asignó una actividad que lleva por titulo: " +
                        "'${titulo}' teniendo que hacer lo siguiente:'${descripcion}'. esta fue su respuesta: " +
                        "'${respuesta}' ademas de adjuntar esta imagen. Analiza su imagen y puntuala dependiendo si coincide con lo requerido " +
                        "con un rango de 0 a '${puntos}' (escribe asi: -x/${puntos}-) y escribe algo corto e interesante acerca del tema o su respuesta")
            }

            val response = generativeModel.generateContent(inputContent)
            response.text?.let {
                popRetroalimentacion(contexto, it, titulo,progressIndicator,"diaria")
            }
        }

    }



    fun AsignarTareas(contexto: Context, tipo:String, documento:String) {
        try {

            val json = leeArchivo(contexto, documento)

            // Generar un número aleatorio entre 0 y el tamaño del arreglo menos uno
            val indice = Random.nextInt(0, json.length())
            // Obtener el elemento del arreglo json usando el índice
            val elemento = json.getJSONObject(indice)
            // Hacer algo con el elemento, por ejemplo, imprimirlo
            cFirebaseA.LeerDatos("Tareas", "tipo", tipo, contexto)

            val sharedPref = contexto.getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
            // Obtener un editor de las SharedPreferences
            val editor = sharedPref.edit()
            // Guardar el texto de la variable elemento como un valor asociado a una clave
            if(tipo == "diaria"){
                editor.putString("descripcion", elemento.get("descripcion").toString())
                editor.putString("titulo", elemento.get("titulo").toString())
                editor.putString("subtipo", elemento.get("subtipo").toString())
                editor.putString("puntos", elemento.get("puntos").toString())
            }else{
                editor.putString("descripcionSemanal", elemento.get("descripcion").toString())
                editor.putString("tituloSemanal", elemento.get("titulo").toString())
                editor.putString("subtipoSemanal", elemento.get("subtipo").toString())
                editor.putString("puntosSemanal", elemento.get("puntos").toString())
            }

            // Guardar los cambios en el archivo
            Log.d("AsignarTareas", "Tareas asignadas")
            editor.apply()
        } catch (e: FileNotFoundException) {
            Log.d("TAG", "Archivo no encontrado, reintentando en 5 segundos", e)
            Handler(Looper.getMainLooper()).postDelayed({
                // Reintentar AsignarTareas después de 5 segundos
                AsignarTareas(contexto, tipo,documento)
            }, 5000)
        }
    }
    fun popEscritura(contexto: Context, titulo: String, descripcion: String, puntos: String,progressIndicator: LinearProgressIndicator){
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
                    samAItexto(contexto,titulo, descripcion, inputText, puntos,progressIndicator)
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

    fun popRetroalimentacion(contexto: Context,result: String, titulo: String,progressIndicator: LinearProgressIndicator, tipo: String) {
        progressIndicator.visibility = View.GONE
        progressIndicator.visibility = View.GONE
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
                if (number != null) {
                    if (tipo == "diaria")
                        actualizaActividad(contexto, number)
                    else
                        actualizaSemanal(contexto,number, titulo)
                }
            }
            .show()
    }

    private fun actualizaSemanal(contexto: Context, puntos: String, titulo: String) {
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
        jsonObject.put("respuestas","Actividad: $titulo "+sharedPref.getString("respuestasSemanal",""))

        val jsonDatos = jsonObject.toString()

        cFirebaseA.UpdateData(jsonDatos)

        cFirebaseA.LeerDatos("Usuarios","Puesto","Empleado",contexto)

        cMenuPrincipal.auxSemanal(contexto,uid!!)

    }

    fun actualizaActividad(contexto: Context,puntos: String){
        val IvCargaCircular = (contexto as MenuPrincipal).findViewById<CircularProgressIndicator>(R.id.CargaCircular)
        IvCargaCircular.isVisible = true
        val sharedPref = contexto.getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
        var uid = sharedPref.getString("UID", "")
        val racha = sharedPref.getInt("racha", 0)

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
        jsonObject.put("racha",racha+1)

        val jsonDatos = jsonObject.toString()

        cFirebaseA.UpdateData(jsonDatos)

        cFirebaseA.LeerDatos("Usuarios","Puesto","Empleado",contexto)

        if (uid != null)
            cMenuPrincipal.aux(contexto,uid)

    }

    fun popImagen(contexto: Context, titulo: String, descripcion: String,puntos: String,progressIndicator: LinearProgressIndicator){
        val inflater = LayoutInflater.from(contexto)
        val view = inflater.inflate(R.layout.activity_selector_fotos, null)
        var texto = view.findViewById<EditText>(R.id.myEditText)
        var imagen = view.findViewById<ImageView>(R.id.myImageButton)
        val im = ImageView(contexto)

        val sharedPref = contexto.getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
        val uri = Uri.parse(sharedPref.getString("fotoTarea",""))

        imagen.setImageURI(uri)
// Crear el dialogo
        val dialog = MaterialAlertDialogBuilder(contexto)
            .setTitle(titulo)
            .setMessage(descripcion)
            .setView(view)
            .setPositiveButton("Aceptar") { dialog, which ->

                CoroutineScope(Dispatchers.Main).launch {
                    samAIimagen(contexto,titulo,descripcion,texto.text.toString(), puntos,progressIndicator)
                }
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
    fun popEncuesta(contexto: Context, titulo: String, descripcion: String, puntos: String,progressIndicator: LinearProgressIndicator){
        val dialogView = LayoutInflater.from(contexto).inflate(R.layout.dialog_survey, null)
        val container = dialogView.findViewById<LinearLayout>(R.id.container)

        val json = objBuscado(contexto, "Tareassemanal","titulo",titulo)
        val opciones = ArrayList<String>()

        json!!.keys().forEach { key ->
            if (key.startsWith("pregunta")) {
                opciones.add(json.getString(key))
            }
        }

        val editTexts = ArrayList<EditText>()

        for (opcion in opciones) {
            val questionTextView = TextView(contexto)
            questionTextView.text = opcion
            container.addView(questionTextView)

            val answerEditText = EditText(contexto)
            editTexts.add(answerEditText)
            container.addView(answerEditText)
        }

        val dialog = MaterialAlertDialogBuilder(contexto)
            .setTitle(titulo)
            .setMessage(descripcion)
            .setView(dialogView)
            .setNeutralButton("Cancelar") { dialog, which ->

            }
            .setPositiveButton("Aceptar") { dialog, which ->
                val answers = editTexts.map { it.text.toString() }
                val sharedPref = contexto.getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
                val editor = sharedPref.edit()
                editor.putString("respuestasSemanal",answers.toString())
                editor.apply()
                CoroutineScope(Dispatchers.Main).launch {
                        samAIEncuesta(contexto, titulo, opciones.toString(), answers.toString(), puntos, progressIndicator,"semanal")
                }
            }
            .show()

    }

    fun popSeleccionMultiple(contexto: Context,titulo: String, descripcion: String,puntos: String,progressIndicator: LinearProgressIndicator){
        val json = objBuscado(contexto, "Tareas","titulo",titulo)
        Log.d("json", "Json seleccion: $json")
        val opciones = ArrayList<String>()

        json!!.keys().forEach { key ->
            if (key.startsWith("opcion")) {
                opciones.add(json.getString(key))
            }
        }
        Log.d("json", "array seleccion: $opciones")

        val adapter = object : ArrayAdapter<String>(contexto, R.layout.list_item, opciones) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(contexto).inflate(R.layout.list_item, parent, false)
                val textView = view.findViewById<TextView>(R.id.option_text)
                textView.text = opciones[position]
                return view
            }
        }

        val dialog = MaterialAlertDialogBuilder(contexto)
            .setTitle(titulo)
            .setAdapter(adapter) { dialog, which ->
                val opcionSeleccionada = opciones[which]
                CoroutineScope(Dispatchers.Main).launch {
                    samAISeleccion(contexto,titulo,descripcion,opcionSeleccionada,json.getString("correcta"),puntos,progressIndicator)
                    dialog.cancel()
                    }
            }
        dialog.show()
    }

    fun objBuscado(contexto: Context, coleccion: String, campo:String, buscado:String): JSONObject? {
        val jsonArray = leeArchivo(contexto,coleccion)
        var objetoBuscado: JSONObject? = null

        for (i in 0 until jsonArray.length()) {
            val objeto = jsonArray.getJSONObject(i)
            if (objeto.getString(campo) == buscado) {
                objetoBuscado = objeto
                break
            }
        }
        if (objetoBuscado != null) {
            Log.d("Objeto","Entra a objeto buscado")
            return objetoBuscado
        } else {
            return null
        }
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