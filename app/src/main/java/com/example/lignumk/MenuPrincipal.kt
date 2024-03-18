package com.example.lignumk

import android.os.Bundle
import androidx.activity.ComponentActivity
//Clases
import ConexionFirebase
import android.animation.LayoutTransition
import android.animation.ObjectAnimator

// Importar la clase Context
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Layout
import android.transition.AutoTransition
import android.transition.Transition
import android.transition.TransitionManager
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.work.WorkManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.concurrent.Executors
import android.app.AlertDialog
import android.net.Uri
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.carousel.CarouselSnapHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.concurrent.thread
import kotlin.properties.Delegates


val cFirebase = ConexionFirebase()
val actividadesMP = Actividades()

class MenuPrincipal : AppCompatActivity() {

    private lateinit var workManager: WorkManager
    lateinit var tvTit: TextView
    lateinit var tvDescripcion: TextView
    lateinit var tvMonedas: TextView
    lateinit var layout: LinearLayout
    lateinit var nombreUsuario: TextView
    lateinit var fotoPerfil: ImageView
    lateinit var progreso: ProgressBar
    lateinit var layoutTarea: RelativeLayout
    lateinit var tvNotificacion: TextView
    lateinit var btnEnviar: ExtendedFloatingActionButton
    lateinit var btSemanal:Button
    lateinit var progressIndicator: LinearProgressIndicator
   private lateinit var carouselRecyclerView: RecyclerView

    //DiasSemana
    lateinit var IvLunes: ImageView
    lateinit var IvMartes: ImageView
    lateinit var IvMiercoles: ImageView
    lateinit var IvJueves: ImageView
    lateinit var IvViernes: ImageView
    lateinit var IvSabado: ImageView
    lateinit var IvDomingo: ImageView

    val actMenu = Actividades()
    private val PREFS_NAME = "MyPrefs"
    private val LAST_OPEN_DATE = "lastOpenDate"

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var pickMedia2: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var auth: FirebaseAuth
    private var firstRun by Delegates.notNull<Boolean>()
    val contsto = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_principal)
        VerificaPrimeraVez()

        val sharedPref = getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
        val FIRST_RUN = "first_run"
        firstRun = sharedPref.getBoolean(FIRST_RUN, true)

        pickMedia2 = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                // Comprueba la versión de Android
                try {
                    // Solicita el permiso persistente
                    applicationContext.contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }

                val sharedPref = this.getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
                val editor = sharedPref.edit()
                editor.putString("fotoTarea", uri.toString())
                editor.apply()

                actividadesMP.popImagen(this,sharedPref.getString("tituloSemanal","")!!, sharedPref.getString("descripcionSemanal","")!!,sharedPref.getString("puntosSemanal","")!!,progressIndicator)//
            } else {

            }
        }

        pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                // Comprueba la versión de Android
                try {
                    // Solicita el permiso persistente
                    applicationContext.contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }

                val sharedPref = this.getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
                val editor = sharedPref.edit()
                editor.putString("fotoTarea", uri.toString())
                editor.apply()

                val puntos = sharedPref.getString("puntos","")
                actividadesMP.popImagen(this,tvTit.text.toString(), tvDescripcion.text.toString(),puntos.toString(),progressIndicator)//
            } else {

            }
        }


        workManager = WorkManager.getInstance(applicationContext)


        tvTit = findViewById(R.id.cTVtitulo)
        tvDescripcion = findViewById(R.id.cTVDescripcion)
        layout = findViewById(R.id.lay)
        nombreUsuario = findViewById(R.id.nombreUsuario)
        tvMonedas = findViewById(R.id.tvMonedas)
        fotoPerfil = findViewById(R.id.fotoPerfil)
        progreso = findViewById(R.id.progressBar)
        layoutTarea = findViewById(R.id.layoutTarea)
        tvNotificacion = findViewById(R.id.cTVnoti)
        btnEnviar = findViewById(R.id.btnEnviarActividad)
        progressIndicator = findViewById(R.id.progress_indicator)
        btSemanal = findViewById(R.id.BotonSemanal)

        IvLunes = findViewById(R.id.IvLunes)
        IvMartes = findViewById(R.id.IvMartes)
        IvMiercoles = findViewById(R.id.IvMiercoles)
        IvJueves = findViewById(R.id.IvJueves)
        IvViernes = findViewById(R.id.IvViernes)
        IvSabado = findViewById(R.id.IvSabado)
        IvDomingo = findViewById(R.id.IvDomingo)



        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this,gso)

        layout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        var firtRun = sharedPref.getBoolean("first_run", true)
        if (!firtRun) {
            cardUsuario(this)
            cardUsuario(this)
        }

    }

    fun cardUsuario(context: Context){
        progressIndicator.visibility = View.VISIBLE

        val sharedPref = context.getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
        var nombre = sharedPref.getString("UserName", "")
        val uid = sharedPref.getString("UID", "")
        val foto = sharedPref.getString("fotoPerfil","")
        val tipo = sharedPref.getString("tipoFoto","")
        val diasSemana = sharedPref.getString("diasSemana","")

        // Aquí recuperamos el Map de las SharedPreferences y asignamos los drawables a los ImageViews
        val jsonString = sharedPref.getString("diasSemana", "")
        val type = object : TypeToken<Map<String, String>>() {}.type
        val recuperadoMap: Map<String, String> = Gson().fromJson(jsonString, type)

        val dias = listOf("IvLunes", "IvMartes", "IvMiercoles", "IvJueves", "IvViernes", "IvSabado", "IvDomingo")

        for (dia in dias) {
            val imageView: ImageView = findViewById(resources.getIdentifier(dia, "id", packageName))
            imageView.setImageResource(resources.getIdentifier(recuperadoMap[dia], "drawable", packageName))
        }

        if (nombre != null) nombre = nombre.split(" ")[0]
        nombreUsuario.text = "\t BIENVENIDO $nombre"
        cFirebase.LeerDatos("Usuarios","Puesto","Empleado",this)

        try{
            val jsonArray = actividadesMP.leeArchivo(this,"Usuarios")
            var objetoBuscado: JSONObject? = null

            for (i in 0 until jsonArray.length()) {
                val objeto = jsonArray.getJSONObject(i)
                if (objeto.getString("UID") == uid) {
                    objetoBuscado = objeto
                    break
                }
            }

            if (objetoBuscado != null) {
                tvMonedas.text = "$ ${objetoBuscado.getInt("monedas")}"
            } else {
                Log.d("MiApp", "No se encontró el usuario")
            }

            if (tipo == "uri"){
                if (foto != null) {
                    val decodedString = Base64.decode(foto, Base64.DEFAULT)
                    val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                    fotoPerfil.setImageBitmap(decodedByte)
                }
            }else{
                val user = Firebase.auth.currentUser
                user?.let {
                    val photoURL = it.photoUrl

                    var image: Bitmap? = null
                    val imageurl = photoURL.toString()
                    val executorService = Executors.newSingleThreadExecutor()
                    executorService.execute {
                        try {
                            val `in` = java.net.URL(imageurl).openStream()
                            image = BitmapFactory.decodeStream(`in`)

                            runOnUiThread {
                                try {
                                    Log.d("xd", "Entra a bitmap")
                                    Thread.sleep(1000)
                                    Log.d("image","imagen $image")
                                    fotoPerfil.setImageBitmap(image)
                                } catch (e: InterruptedException) {
                                    Log.d("tag1","Error UI 1")
                                }
                            }
                        } catch (e: Exception) {
                            runOnUiThread {
                                Log.d("tag1","Error UI 2")
                            }
                        }
                    }
                }

            }
        }catch (e: FileNotFoundException) {
            Log.d("TAG", "Archivo no encontrado, reintentando en 5 segundos", e)
            Handler(Looper.getMainLooper()).postDelayed({
                // Reintentar AsignarTareas después de 5 segundos
                val json = actividadesMP.leeArchivo(this,"Usuarios")
                cardUsuario(this)
            }, 5000)
        }finally {
            Log.d("TAG", "No se pudo w")
        }

        //-------------
        val diaDeLaSemana = LocalDate.now().dayOfWeek.value
        progreso.progress = diaDeLaSemana-1
        progressIndicator.visibility = View.GONE
    }
    fun cardSemanal(view: View){
        val sharedPref = this.getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
        val puntos = sharedPref.getString("puntos","")
        val tituloSemanal = sharedPref.getString("tituloSemanal","")
        val descripcionSemanal = sharedPref.getString("descripcionSemanal","")
        val subtipoSemanal = sharedPref.getString("subtipoSemanal","")

        when(subtipoSemanal){
            "encuesta" -> actividadesMP.popEncuesta(contsto ,tituloSemanal!!,descripcionSemanal!!,puntos!!,progressIndicator)
            "foto" -> {
                val dialog = MaterialAlertDialogBuilder(this)
                    .setTitle(tituloSemanal)
                    .setMessage(descripcionSemanal)
                    .setPositiveButton("Aceptar") { dialog, which ->




                        pickMedia2.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    .show()

                }
        }

        progressIndicator.visibility = View.GONE
    }

    fun getImages():List<String>{
        return listOf(
            "https://files.catbox.moe/q2s4ph.png"
        )
    }

    fun DepCard(view: View){
        val v = if (layoutTarea.visibility == View.GONE) View.VISIBLE else View.GONE
        TransitionManager.beginDelayedTransition(layout, AutoTransition())
        layoutTarea.visibility = v

    }

    fun prueba(view: View){
        val sharedPref = this.getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
        val subtipo = sharedPref.getString("subtipo","")
        val puntos = sharedPref.getString("puntos","")
        val tituloSemanal = sharedPref.getString("tituloSemanal","")
        val descripcionSemanal = sharedPref.getString("descripcionSemanal","")
        val subtipoSemanal = sharedPref.getString("subtipoSemanal","")

// Muestra el indicador de progreso antes de iniciar la operación de larga duración
        var result =""
        Log.d("Subtipo", "Subtipo de actividad: $subtipo")
        when (subtipo) {
            "escritura" -> actividadesMP.popEscritura(contsto,tvTit.text.toString(), tvDescripcion.text.toString(),puntos.toString(),progressIndicator)
            "foto" -> pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            "seleccionMultiple" -> actividadesMP.popSeleccionMultiple(contsto, tvTit.text.toString(), tvDescripcion.text.toString(), puntos.toString(),progressIndicator)

            else -> {
                when(subtipoSemanal){
                    "encuesta" -> actividadesMP.popEncuesta(contsto ,sharedPref.getString("tituloSemanal","")!!,sharedPref.getString("descripcionSemanal","")!!,sharedPref.getString("puntosSemanal","")!!,progressIndicator)
                }
            }
        }
progressIndicator.visibility = View.GONE
    }
    fun auxSemanal(context: MenuPrincipal, uid: String) {
        val tvMonedas = (context as MenuPrincipal).findViewById<TextView>(R.id.tvMonedas)
        val btnSemanal = (context as MenuPrincipal).findViewById<Button>(R.id.BotonSemanal)
        btSemanal.visibility = View.GONE

        Handler(Looper.getMainLooper()).postDelayed({
            val jsonArray = actividadesMP.leeArchivo(context,"Usuarios")
            var objetoBuscado: JSONObject? = null

            for (i in 0 until jsonArray.length()) {
                val objeto = jsonArray.getJSONObject(i)
                if (objeto.getString("UID") == uid) {
                    objetoBuscado = objeto
                    break
                }
            }
            if (objetoBuscado != null) {
                Log.d("Objeto","Entra a objeto buscado")
                tvMonedas.text = "$ ${objetoBuscado.getInt("monedas")}"
            } else {
                Log.d("MiApp", "No se encontró el usuario")
            }
        }, 5000)

        val sharedPref = getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString("tituloSemanal","Realizada!")
        editor.apply()

        val carouselRecyclerView: RecyclerView = findViewById(R.id.carouselRecyclerView)
        carouselRecyclerView.adapter = CarouselAdapter(images = getImages(),"Realizada!")
    }
    fun aux(context: Context,uid: String){
        val tvMonedas = (context as MenuPrincipal).findViewById<TextView>(R.id.tvMonedas)
        val tvNotificacion = context.findViewById<TextView>(R.id.cTVnoti)
        val btnEnviar = context.findViewById<TextView>(R.id.btnEnviarActividad)
        val IvCargaCircular = context.findViewById<CircularProgressIndicator>(R.id.CargaCircular)
        val layout = context.findViewById<RelativeLayout>(R.id.layoutTarea)

        val IvLunes = context.findViewById<ImageView>(R.id.IvLunes)
        val IvMartes = context.findViewById<ImageView>(R.id.IvMartes)
        val IvMiercoles = context.findViewById<ImageView>(R.id.IvMiercoles)
        val IvJueves = context.findViewById<ImageView>(R.id.IvJueves)
        val IvViernes = context.findViewById<ImageView>(R.id.IvViernes)
        val IvSabado = context.findViewById<ImageView>(R.id.IvSabado)
        val IvDomingo = context.findViewById<ImageView>(R.id.IvDomingo)

        Handler(Looper.getMainLooper()).postDelayed({
            val jsonArray = actividadesMP.leeArchivo(context,"Usuarios")
            var objetoBuscado: JSONObject? = null

            for (i in 0 until jsonArray.length()) {
                val objeto = jsonArray.getJSONObject(i)
                if (objeto.getString("UID") == uid) {
                    objetoBuscado = objeto
                    break
                }
            }
            if (objetoBuscado != null) {
                Log.d("Objeto","Entra a objeto buscado")
                tvMonedas.text = "$ ${objetoBuscado.getInt("monedas")}"
            } else {
                Log.d("MiApp", "No se encontró el usuario")
            }
        }, 5000)

        val diaDeLaSemana = LocalDate.now().dayOfWeek.value
        val imageViews = arrayOf(IvLunes, IvMartes, IvMiercoles, IvJueves, IvViernes, IvSabado, IvDomingo)
        val drawables = arrayOf(R.drawable.lunesbien, R.drawable.martesbien, R.drawable.miercolesbien, R.drawable.juevesbien, R.drawable.viernesbien, R.drawable.sabadobien, R.drawable.domingobien)
        imageViews[diaDeLaSemana-1].setImageResource(drawables[diaDeLaSemana-1])

        IvCargaCircular.isVisible = false
        layout.isVisible = false

        val dias = listOf("IvLunes", "IvMartes", "IvMiercoles", "IvJueves", "IvViernes", "IvSabado", "IvDomingo")

// Obtiene el nombre del día correspondiente al número del día de la semana
        val nombreDelDia = dias[diaDeLaSemana - 1]

        val sharedPref = context.getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
        val jsonString = sharedPref.getString("diasSemana", "")
        val racha = sharedPref.getInt("racha",0)

        val type = object : TypeToken<Map<String, String>>() {}.type
        val recuperadoMap: Map<String, String> = Gson().fromJson(jsonString, type)
        val mutableMap = recuperadoMap.toMutableMap()

// Reemplaza el valor por el nuevo valor
        val valorActual = mutableMap[nombreDelDia]
        mutableMap[nombreDelDia] = "$valorActual" + "bien"

// Guarda el nuevo valor en las SharedPreferences
        val nuevoJsonString = Gson().toJson(mutableMap.toMap())
        val editor = sharedPref.edit()
        editor.putString("diasSemana", nuevoJsonString)
        editor.putString("actNotificacion", "Actividad Realizada!")
        editor.putBoolean("actBotonEnviar", false)
        editor.putInt("racha", racha+1)
        editor.apply()

        tvNotificacion.text = sharedPref.getString("actNotificacion","")
        btnEnviar.isEnabled = sharedPref.getBoolean("actBotonEnviar",true)

    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onStart() {
        super.onStart()

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

        // Compara las fechas
        val sharedPref = getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()

        if (currentDay != lastOpenDay && firstRun == false) {
            actMenu.AsignarTareas(this,"diaria","Tareas")
            editor.putString("actNotificacion","Tienes una nueva asignacion!")
            editor.putBoolean("actBotonEnviar",true)

            prefs.edit().putLong(LAST_OPEN_DATE, currentDate).apply()

            //Reinicia dias de la semana si es lunes:
            val diaDeLaSemana = LocalDate.now().dayOfWeek.value
            if (diaDeLaSemana == 1) { // Si es lunes
                btSemanal.alpha= 0f
                btSemanal.isEnabled = true
                val diasSemana: Map<String, String> =
                    mapOf("IvLunes" to "lunes", "IvMartes" to "martes", "IvMiercoles" to "miercoles",
                        "IvJueves" to "jueves","IvViernes" to "viernes","IvSabado" to "sabado",
                        "IvDomingo" to "domingo")

                val jsonString = Gson().toJson(diasSemana)

                editor.putString("diasSemana", jsonString)
                editor.apply()

                //Tarea semanal
                actMenu.AsignarTareas(this,"semanal","Tareassemanal")

            }else{
                val dias = listOf("IvLunes", "IvMartes", "IvMiercoles", "IvJueves", "IvViernes", "IvSabado", "IvDomingo")
// Obtiene el nombre del día correspondiente al número del día de la semana de ayer
                val nombreDelDia = dias[diaDeLaSemana - 2]
                val jsonString = sharedPref.getString("diasSemana", "")

                val type = object : TypeToken<Map<String, String>>() {}.type
                val recuperadoMap: Map<String, String> = Gson().fromJson(jsonString, type)
                val mutableMap = recuperadoMap.toMutableMap()

// Reemplaza el valor por el nuevo valor
                val valorActual = mutableMap[nombreDelDia]
                if (!valorActual!!.contains("bien")){
                    mutableMap[nombreDelDia] = "$valorActual" + "mal"
                    val nuevoJsonString = Gson().toJson(mutableMap.toMap())
                    editor.putString("diasSemana", nuevoJsonString)
                    editor.apply()
                }
            }

            editor.apply()
        }

        //-----------------------
        // Recuperar el texto de la variable elemento usando la misma clave
        tvNotificacion.text = sharedPref.getString("actNotificacion","")
        tvDescripcion.text = sharedPref.getString("descripcion", "")
        tvTit.text =  sharedPref.getString("titulo", "")
        btnEnviar.isEnabled = sharedPref.getBoolean("actBotonEnviar",true)
        val carouselRecyclerView: RecyclerView = findViewById(R.id.carouselRecyclerView)
        carouselRecyclerView.adapter = CarouselAdapter(images = getImages(),sharedPref.getString("tituloSemanal","")!!)

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