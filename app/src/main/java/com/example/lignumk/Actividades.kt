package com.example.lignumk

import ConexionFirebase
import WorkManagerFile
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.util.Log
import org.json.JSONArray
import java.io.File
import java.io.FileReader
import kotlin.random.Random
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.widget.EditText
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.example.lignumk.databinding.ActivityMenuPrincipalBinding
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.reflect.KClass
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.widget.Button

val cFirebaseA = ConexionFirebase()
@SuppressLint("StaticFieldLeak")
val cMenuPrincipal = MenuPrincipal()
class Actividades{
    //######################################## OBJETOS

    fun log(message: String, fileName: String) {
        Log.d(fileName, message)
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            // for other device how are able to connect with Ethernet
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            // for check internet over Bluetooth
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
            else -> false
        }
    }

    fun leeArchivo(contexto: Context, nombre: String): JSONArray {
        val archivo = File(contexto.getExternalFilesDir(null), "$nombre.json")
        val archivoLector = FileReader(archivo)
        val contenido = archivoLector.readText()
        return JSONArray(contenido)
    }

    fun modeloIA(model: String): GenerativeModel {
        return GenerativeModel(
            modelName = model,
            apiKey = "AIzaSyDo5BH4jyyrGS28OIpMTdpTL-Zx3oVGKbI"
        )
    }

    suspend fun samAIRetro(
        contexto: Context,
        racha: Int,
        lastSignInDate: Date?,
        fechaComoCadena: String,
        descansos: String,
        promedio: Float,
        progressIndicator: LinearProgressIndicator
    ) {
        val generativeModel = modeloIA("gemini-pro")

        val prompt =
            "${contexto.getString(R.string.retroalimentacion)} racha (numero de actividades realizadas): $racha," +
                    "primera vez que accedió a la aplicacion: $lastSignInDate, hoy: $fechaComoCadena," +
                    "dias de descanso (no se hacen actividades): $descansos," +
                    "calificacion promediada sobre 100 de todas las actividades realizadas: $promedio/100"


        Log.d("prompt",prompt)
        generativeModel.generateContent(prompt).text?.let {
            progressIndicator.visibility = View.GONE
            actividadesMP.anuncio(
                "SAM dice:",
                it,
                contexto
            )
        }
    }

    private suspend fun samAItexto(contexto: Context, titulo: String, descripcion: String, respuesta: String, puntos:String, progressIndicator:LinearProgressIndicator, binding: ActivityMenuPrincipalBinding) {
        val generativeModel = modeloIA("gemini-pro")
        progressIndicator.visibility = View.VISIBLE

        val prompt = "A un trabajador de una empresa madedera se le asignó una actividad que lleva por titulo: " +
                "'${titulo}' teniendo que hacer lo siguiente:'${descripcion}'. esta fue su respuesta: " +
                "'${respuesta}'. puntua su respuesta con un rango de 0 a '${puntos}' (escribe asi: -x/${puntos}-) " +
                "y escribe un dato curioso corto corto acerca del tema y su respuesta"

        generativeModel.generateContent(prompt) .text?.let { popRetroalimentacion(contexto, it, titulo,progressIndicator,"diaria", binding, puntos.toInt()) }
    }

    private suspend fun samAIEncuesta(contexto: Context, titulo: String, descripcion: String, respuesta: String, puntos:String, progressIndicator:LinearProgressIndicator, tipo: String, binding: ActivityMenuPrincipalBinding) {
        val generativeModel = modeloIA("gemini-pro")
        progressIndicator.visibility = View.VISIBLE

        val prompt = "A un trabajador de una empresa madedera se le asignó una encuesta, teniendo que " +
                "responder las siguientes preguntas:'${descripcion}'. estas fueron sus respuestas: " +
                "'${respuesta}'.escribe: -${puntos}/${puntos}- (ejemplo: -10/10-), recuerda poner ambos guiones" +
                "y escribe un pequeño comentario constructivo en base a sus respuestas"

        generativeModel.generateContent(prompt) .text?.let { popRetroalimentacion(contexto, it, titulo,progressIndicator,tipo, binding, puntos.toInt()) }
    }

    private suspend fun samAISeleccion(contexto: Context, titulo: String, descripcion: String, respuesta: String, opcionCorrecta:String, puntos:String, progressIndicator: LinearProgressIndicator, binding: ActivityMenuPrincipalBinding) {
        progressIndicator.visibility = View.VISIBLE
        val generativeModel = modeloIA("gemini-pro")

        val prompt = "A un trabajador de una empresa madedera se le asignó una actividad que lleva " +
                "por titulo: '${titulo}' teniendo que responder la siguiente pregunta de opcion multiple:" +
                "'${descripcion}'. esta fue la respuesta que selecciono: '${respuesta}', la respuesta correcta " +
                "a la pregunta es: ${opcionCorrecta}. puntua su respuesta con un rango de 0 a '${puntos}' " +
                "(escribe asi: -x/${puntos}- por ejemplo -15/15-) y escribe un dato curioso corto acerca del tema o su respuesta " +
                "que incite el uso de equipo de seguridad a pesar de no querer"

        generativeModel.generateContent(prompt) .text?.let { popRetroalimentacion(contexto, it, titulo,progressIndicator,"diaria", binding, puntos.toInt()) }
    }

    private suspend fun samAIimagen(contexto: Context, titulo: String, descripcion: String, respuesta: String, puntos: String, progressIndicator: LinearProgressIndicator, binding: ActivityMenuPrincipalBinding, tipo: String){
        Log.d(contexto.getString(R.string.actividades), "samImage llega con con ${titulo}, ${descripcion}" +
                ", ${puntos}, $tipo")

        progressIndicator.visibility = View.VISIBLE
        val generativeModel = modeloIA("gemini-pro-vision")

        val uriString = sharedPref(contexto, "fotoTarea", String::class.java)

            val uri = Uri.parse(uriString)
            val inputStream = contexto.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            val image1: Bitmap = bitmap

            val inputContent = content {
                image(image1)
                text("A un trabajador de una empresa madedera se le asignó una actividad que lleva por titulo: " +
                        "'${titulo}' teniendo que hacer lo siguiente:'${descripcion}'. esta fue su respuesta: " +
                        "'${respuesta}' ademas de adjuntar esta imagen. Analiza su imagen y puntuala dependiendo si coincide con lo requerido " +
                        "con un rango de 0 a '${puntos}' escribe asi: -x/${puntos}- y escribe algo corto e interesante acerca del tema o su respuesta")
            }

        generativeModel.generateContent(inputContent) .text?.let {
            Log.d(contexto.getString(R.string.actividades), "samImage -> popRetroalimentacion  \n" +
                    "con ${titulo}, ${puntos}, $tipo, \n $it")

            popRetroalimentacion(contexto, it, titulo,progressIndicator,tipo,binding, puntos.toInt()) }

    }

    @Suppress("UNCHECKED_CAST")
    fun <T> sharedPref(contexto: Context, variable: String, tipo: Class<T>): T? {
        val sharedPref = contexto.getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
        return when (tipo) {
            String::class.java -> sharedPref.getString(variable, "") as T?
            Int::class.java -> sharedPref.getInt(variable, 0) as T?
            Boolean::class.java -> sharedPref.getBoolean(variable, true) as T?
            Long::class.java -> sharedPref.getLong(variable, 0) as T?
            Float::class.java -> sharedPref.getFloat(variable, 0f) as T?
            else -> null
        }
    }

    fun <T> saveSharedPref(contexto: Context, variable: String, valor: T) {
        val sharedPref = contexto.getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()

        when (valor) {
            is String -> editor.putString(variable, valor)
            is Int -> editor.putInt(variable, valor)
            is Float -> editor.putFloat(variable, valor)
            is Boolean -> editor.putBoolean(variable, valor)
            is Long -> editor.putLong(variable, valor)
            else -> throw IllegalArgumentException("Tipo no soportado")
        }

        editor.apply()
    }

    fun uriToBase64(contexto: Context, uri: Uri, shared: String): String? {
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            contexto.contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        val inputStream = contexto.contentResolver.openInputStream(uri)
        var bitmap = BitmapFactory.decodeStream(inputStream)
        bitmap = resizeToSquare(bitmap)

        // Reducir la calidad de compresión al comprimir la imagen como JPEG con calidad del 20%
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 20, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()

        val encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT)
        return encodedImage
    }

    fun resizeToSquare(bitmap: Bitmap): Bitmap {
        val size = min(bitmap.width, bitmap.height)
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2
        return Bitmap.createBitmap(bitmap, x, y, size, size)
    }


    fun AsignarTareas(contexto: Context, tipo:String, documento:String) {
        try {

            val json = leeArchivo(contexto, documento)
            // Generar un número aleatorio entre 0 y el tamaño del arreglo menos uno
            val indice = Random.nextInt(0, json.length())
            val elemento = json.getJSONObject(indice)

            cFirebaseA.LeerDatos("Tareas", "tipo", tipo, contexto)

            // Guardar el texto de la variable elemento como un valor asociado a una clave
            if(tipo == "diaria"){
                saveSharedPref(contexto,"descripcion",elemento.get("descripcion").toString())
                saveSharedPref(contexto,"titulo",elemento.get("titulo").toString())
                saveSharedPref(contexto,"subtipo",elemento.get("subtipo").toString())
                saveSharedPref(contexto,"puntos",elemento.get("puntos").toString())
            }else{
                saveSharedPref(contexto,"descripcionSemanal",elemento.get("descripcion").toString())
                saveSharedPref(contexto,"tituloSemanal",elemento.get("titulo").toString())
                saveSharedPref(contexto,"subtipoSemanal",elemento.get("subtipo").toString())
                saveSharedPref(contexto,"puntosSemanal",elemento.get("puntos").toString())
            }

            Log.d("AsignarTareas", "Tareas asignadas")
        } catch (e: FileNotFoundException) {
            Log.d("TAG", "Archivo no encontrado, reintentando en 5 segundos", e)
            Handler(Looper.getMainLooper()).postDelayed({
                AsignarTareas(contexto, tipo,documento)
            }, 5000)
        }
    }
    fun popEscritura(contexto: Context, titulo: String, descripcion: String, puntos: String,progressIndicator: LinearProgressIndicator, binding: ActivityMenuPrincipalBinding){
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
                    samAItexto(contexto,titulo, descripcion, inputText, puntos,progressIndicator, binding)
                }
            }
            .setCancelable(false)
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

    private fun popRetroalimentacion(contexto: Context, result: String, titulo: String, progressIndicator: LinearProgressIndicator, tipo: String, binding: ActivityMenuPrincipalBinding, puntosTotales: Int) {
        Log.d(contexto.getString(R.string.actividades), "popRetroalimentacion llega con " +
                " ${result},\n ${titulo}, $tipo")

        progressIndicator.visibility = View.GONE
        MaterialAlertDialogBuilder(contexto)
            .setTitle(titulo)
            .setMessage(result)
            .setPositiveButton("Reclamar puntos") { dialog, which ->
                //Marcar como completada la actividad
                val regex = Regex("-\\d+/\\d+-")
                val matchResult = regex.find(result)
                val score = matchResult?.value

                val number = score?.substring(1, score.indexOf("/"))  // Esto debería dar "10"
                if (number != null) {
                    if (tipo == "diaria") {
                        Log.d(
                            contexto.getString(R.string.actividades),
                            "popRetroalimentacion -> actualizaActividad" +
                                    "con diaria $number"
                        )
                        actualizaActividad(contexto, number, binding, puntosTotales)
                    }else {
                        Log.d(contexto.getString(R.string.actividades), "popRetroalimentacion -> actualizaActividad" +
                                "con semanal ${number}, $titulo")
                        actualizaSemanal(contexto, number, titulo, binding)
                    }
                }
            }
            .setCancelable(false)
            .show()
    }

    @Suppress("UNCHECKED_CAST")
    fun<T : Any> objetoBuscado(json: JSONArray, identificador: String, claveBuscada: String, tipo: KClass<T>): T? {
        var objetoBuscado: JSONObject? = null

        //Busca subJson
        for (i in 0 until json.length()) {
            val objeto = json.getJSONObject(i)
            if (objeto.getString("UID") == identificador) {
                objetoBuscado = objeto
                break
            }
        }

        //Busca clave
        return when {
            objetoBuscado != null -> {
                Log.d("Objeto","Entra a objeto buscado")
                when (tipo) {
                    Int::class -> objetoBuscado.getInt(claveBuscada) as T?
                    String::class -> objetoBuscado.getString(claveBuscada) as T?
                    Boolean::class -> objetoBuscado.getBoolean(claveBuscada) as T?
                    else -> null
                }
            }

            else -> {
                Log.d("MiApp", "No se encontró el usuario")
                if (tipo == Int::class) {
                    return 0 as T
                }
                if (tipo == String::class){
                    return "0" as T
                }else
                    return null
            }
        }
    }

    private fun actualizaSemanal(contexto: Context, puntos: String, titulo: String, binding: ActivityMenuPrincipalBinding) {
        Log.d(contexto.getString(R.string.actividades), "Llega a actualizaActividad" +
                "con semanal ${puntos}, $titulo")

        val uid = sharedPref(contexto,"UID",String::class.java)

        val json = actividadesMP.leeArchivo(contexto,"Usuarios")

        val monedas : Int =  objetoBuscado(json,uid!!,"monedas",Int::class)?.plus(puntos.toInt()) ?: 0

        val jsonObject = JSONObject()
        jsonObject.put("coleccion", "Usuarios")
        jsonObject.put("documento", uid)
        jsonObject.put("monedas",monedas)
        jsonObject.put("respuestas","Actividad: $titulo "+sharedPref(contexto,"respuestasSemanal",String::class.java))
        val jsonDatos = jsonObject.toString()

        cFirebaseA.UpdateData(jsonDatos)

        cFirebaseA.LeerDatos("Usuarios","Puesto","Empleado",contexto)

        Log.d(contexto.getString(R.string.actividades)+"->"+contexto.getString(R.string.menuPrincipal),
            "actualizaActividad -> auxSemanal " + "con ${uid}")
        cMenuPrincipal.auxSemanal(contexto,uid, binding)
    }

    fun actualizaActividad(contexto: Context, puntos: String,binding: ActivityMenuPrincipalBinding, puntosTotales: Int){
        var Puntos = 0
        val potenciador = sharedPref(contexto,"potenciadorActivo", String::class.java)!!
        Puntos = if (potenciador == "Astillas")
            puntos.toInt() * 2
        else
            puntos.toInt()

        val uid = sharedPref(contexto,"UID",String::class.java)
        var racha = sharedPref(contexto,"racha",Int::class.java) ?: 1
        var promedio = sharedPref(contexto, "promedio", Float::class.java) ?: 0f

        racha += 1
        var promActividad = (puntos.toInt()/puntosTotales) * 100
        promedio = ( (promedio*(racha-1))+promActividad )/racha
        saveSharedPref(contexto,"promedio", promedio)

        val json = actividadesMP.leeArchivo(contexto,"Usuarios")
        var monedas : Int =  objetoBuscado(json,uid!!,"monedas",Int::class)?.plus(Puntos) ?: 0


        val jsonObject = JSONObject()
        jsonObject.put("coleccion", "Usuarios")
        jsonObject.put("documento", uid)
        jsonObject.put("monedas",monedas)
        jsonObject.put("racha",racha)
        jsonObject.put("promedio",promedio)
        val jsonDatos = jsonObject.toString()

        cFirebaseA.UpdateData(jsonDatos)

        cFirebaseA.LeerDatos("Usuarios","Puesto","Empleado",contexto)

        actividades.saveSharedPref(contexto, "tipoPotenciadorActivo", "no")
        actividades.saveSharedPref(contexto, "potenciadorActivo", "no")

        cMenuPrincipal.aux(contexto,uid, binding)
    }

    fun popImagen(contexto: Context, titulo: String, descripcion: String,puntos: String,progressIndicator: LinearProgressIndicator,binding: ActivityMenuPrincipalBinding,tipo: String){
        val inflater = LayoutInflater.from(contexto)
        val view = inflater.inflate(R.layout.activity_selector_fotos, null)
        val texto = view.findViewById<EditText>(R.id.myEditText)
        val imagen = view.findViewById<ImageView>(R.id.myImageButton)

        val uri = Uri.parse(sharedPref(contexto,"fotoTarea",String::class.java))
        imagen.setImageURI(uri)

        val dialog = MaterialAlertDialogBuilder(contexto)
            .setTitle(titulo)
            .setMessage(descripcion)
            .setView(view)
            .setPositiveButton("Aceptar") { dialog, which ->

                CoroutineScope(Dispatchers.Main).launch {
                    Log.d(contexto.getString(R.string.actividades), "popImage con ${titulo}, ${descripcion}" +
                            ", ${puntos}, ${tipo}, ${texto.text}")
                    saveSharedPref(contexto,"respuestasSemanal",texto.text.toString())

                    samAIimagen(contexto,titulo,descripcion,texto.text.toString(), puntos,progressIndicator, binding, tipo)
                }

            }
            .setNegativeButton("Cancelar", null)
            .setCancelable(false)
            .show()

        texto.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = !s.isNullOrEmpty()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
    }

    fun popEncuesta(contexto: Context, titulo: String, descripcion: String, puntos: String,progressIndicator: LinearProgressIndicator,binding: ActivityMenuPrincipalBinding){
        val dialogView = LayoutInflater.from(contexto).inflate(R.layout.dialog_survey, null)
        val container = dialogView.findViewById<LinearLayout>(R.id.container)
        val btnAceptar = dialogView.findViewById<Button>(R.id.btnAceptar)

        val (_, opciones) = jsonBuscado(contexto, "Tareassemanal","titulo",titulo,"pregunta") ?: Pair(null, null)

        val editTexts = ArrayList<EditText>()

        for (opcion in opciones!!) {
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
            .setCancelable(false)
            .show()

        btnAceptar.setOnClickListener {
            val answers = editTexts.map { it.text.toString() }
            saveSharedPref(contexto,"respuestasSemanal",answers.toString())
            CoroutineScope(Dispatchers.Main).launch {
                samAIEncuesta(contexto, titulo, opciones.toString(), answers.toString(), puntos, progressIndicator,"semanal", binding)
            }
            dialog.dismiss() // Cierra el diálogo
        }
    }


    fun popSeleccionMultiple(contexto: Context,titulo: String, descripcion: String,puntos: String,progressIndicator: LinearProgressIndicator, binding: ActivityMenuPrincipalBinding){
        val (json, opciones) = jsonBuscado(contexto, "Tareas","titulo",titulo,"opcion") ?: Pair(null, null)

        val adapter = object : ArrayAdapter<String>(contexto, R.layout.list_item, opciones!!) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(contexto).inflate(R.layout.list_item, parent, false)
                val textView = view.findViewById<TextView>(R.id.option_text)
                textView.text = opciones!![position]
                return view
            }
        }
        val correcta = json!!.getString("correcta")
        var tit = ""
        val potenciadorActivo = sharedPref(contexto, "potenciadorActivo",String::class.java)
        tit = if(potenciadorActivo.equals("Martillo del alba"))
            "$titulo\n⭐Opcion correcta: $correcta"
        else
            titulo

        val dialog = MaterialAlertDialogBuilder(contexto)
            .setTitle(tit)
            .setAdapter(adapter) { dialog, which ->
                val opcionSeleccionada = opciones[which]
                CoroutineScope(Dispatchers.Main).launch {
                    samAISeleccion(contexto,titulo,descripcion,opcionSeleccionada,correcta,puntos,progressIndicator, binding)
                    dialog.cancel()
                    }
            }
            .setCancelable(false)
        dialog.show()
    }

    private fun jsonBuscado(contexto: Context, coleccion: String, campo:String, buscado:String, llave:String): Pair<JSONObject?, ArrayList<String>?>? {
        val jsonArray = leeArchivo(contexto,coleccion)
        var objetoBuscado: JSONObject? = null
        var opciones: ArrayList<String>? = null

        //Busca subJson
        for (i in 0 until jsonArray.length()) {
            val objeto = jsonArray.getJSONObject(i)
            if (objeto.getString(campo) == buscado) {
                objetoBuscado = objeto
                break
            }
        }
        //Busca clave
        if (objetoBuscado != null) {
            Log.d("Objeto","Entra a objeto buscado")
            opciones = ArrayList<String>()
            objetoBuscado.keys().forEach { key ->
                if (key.startsWith(llave)) {
                    opciones.add(objetoBuscado.getString(key))
                }
            }
        } else {
            Log.d("MiApp", "No se encontró el usuario")
        }
        return if (objetoBuscado != null || opciones != null) Pair(objetoBuscado, opciones) else null
    }
    data class ItemInsignias(
        val icono: String,
        val limite: Int,
        val nombre: String
    )

    fun anuncio(titulo: String, descripcion: String, contexto: Context){
        MaterialAlertDialogBuilder(contexto)
            .setTitle(titulo)
            .setMessage(descripcion)
            .setPositiveButton("Aceptar") { dialog, _ ->
            dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }
    fun Insignias(contexto: Context) {
        val racha = sharedPref(contexto, "racha", Int::class.java)
        val json = leeArchivo(contexto, "Insignias")
        val jsonString = json.toString()
        val listType = object : TypeToken<List<ItemInsignias>>() {}.type
        val items: List<ItemInsignias> = Gson().fromJson(jsonString, listType)

        for (item in items) {
            if (racha == item.limite) {
                anuncio("Nueva insignia!","Desbloqueaste la insignia de ${item.nombre} " +
                        "por haber tenido una racha de $racha dias\n\nSigue así!",contexto)
                // Guardar el valor de la clave "icono" en la preferencia compartida
                actividades.saveSharedPref(contexto, "InsigniaActiva", item.icono)
                break // Salir del bucle si se encuentra una coincidencia
            }
        }
    }


    fun sincronizaTareas(): Long {
        val currentTime = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) // Hora actual
        val desiredHour = 7 // Hora deseada (7 AM)
        val desiredMinute = 0 // Minuto deseado (35 minutos)
        // Calcula la diferencia en milisegundos hasta la próxima ejecución
        val delay = TimeUnit.HOURS.toMillis(((0 + desiredHour - currentTime) % 24).toLong()) +
                TimeUnit.MINUTES.toMillis((desiredMinute - Calendar.getInstance().get(Calendar.MINUTE)).toLong())

        return delay
    }
    fun periodicTimeR(contexto: Context, delay: Long,para: String){
        val miPeriodicWorkRequest = PeriodicWorkRequest.Builder(WorkManagerFile::class.java, delay, TimeUnit.SECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(Data.Builder().putString("parametro", para).build())
            .build()
        WorkManager.getInstance(contexto).enqueue(miPeriodicWorkRequest)
    }

    fun oneTimeR(contexto: Context, delay: Long,para: String){
        lateinit var workManager: WorkManager
        WorkManager.getInstance(contexto)
        Log.d("Parametro", "Parametro oneTimeR: ${para}")
        val miOneTimeWorkRequest = OneTimeWorkRequest.Builder(WorkManagerFile::class.java)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(false).build())
            .setInputData(Data.Builder().putString("parametro", para).build())
            .setInitialDelay(delay, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(contexto).enqueue(miOneTimeWorkRequest)

    }

    /*f



    fun periodicRTareas(contexto: Context){
        val miPeriodicWorkRequest = PeriodicWorkRequest.Builder(WorkManagerFile::class.java, 15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(Data.Builder().putString("parametro", "AsignarTareas").build())
            .build()
        WorkManager.getInstance(contexto).enqueue(miPeriodicWorkRequest)
    }
*/
}