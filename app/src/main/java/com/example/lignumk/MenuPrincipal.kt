package com.example.lignumk

import android.os.Bundle
import androidx.activity.ComponentActivity
//Clases
import ConexionFirebase
import android.animation.LayoutTransition

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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject


val cFirebase = ConexionFirebase()
val actividadesMP = Actividades()

class MenuPrincipal : ComponentActivity() {

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
    private lateinit var auth: FirebaseAuth
    val contsto = this

    override fun onResume() {
        super.onResume()
        Log.d("Resume ", "Entra a OnResume")
        cardUsuario(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        workManager = WorkManager.getInstance(applicationContext)
        setContentView(R.layout.activity_menu_principal)

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

        IvLunes = findViewById(R.id.Lunes)
        IvMartes = findViewById(R.id.Martes)
        IvMiercoles = findViewById(R.id.Miercoles)
        IvJueves = findViewById(R.id.Jueves)
        IvViernes = findViewById(R.id.Viernes)
        IvSabado = findViewById(R.id.Sabado)
        IvDomingo = findViewById(R.id.Domingo)

        VerificaPrimeraVez()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this,gso)

        layout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        val sharedPref = getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
        var firtRun = sharedPref.getBoolean("first_run", true)
        if (!firtRun)
            cardUsuario(this)

    }

    fun cardUsuario(context: Context){
        Log.d("hola","hola papus")
        val sharedPref = context.getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
        var nombre = sharedPref.getString("UserName", "")
        var uid = sharedPref.getString("UID", "")
        var foto = sharedPref.getString("fotoPerfil","")
        var tipo = sharedPref.getString("tipoFoto","")
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
                tvMonedas.text = "$ ${objetoBuscado.getInt("monedas").toString()}"
            } else {
                Log.d("MiApp", "No se encontró el usuario")
            }

            if (tipo == "uri"){
                if (foto != null) {
                    val uri = Uri.parse(foto)
                    fotoPerfil.setImageURI(uri)
                }
            }else{
                val user = Firebase.auth.currentUser
                user?.let {
                    val photoURL = it.photoUrl

                    var image:Bitmap? = null
                    val imageurl = photoURL.toString()
                    val executorService = Executors.newSingleThreadExecutor()
                    executorService.execute{
                        try {
                            val `in` = java.net.URL(imageurl).openStream()
                            image = BitmapFactory.decodeStream(`in`)
                        }catch (e:Exception){
                            Toast.makeText(this,"Error", Toast.LENGTH_SHORT).show()
                        }
                        runOnUiThread{
                            try {
                                Thread.sleep(1000)
                                //Imagen placed
                                fotoPerfil.setImageBitmap(image)

                            }catch (e:InterruptedException){
                                Toast.makeText(this,"Error", Toast.LENGTH_SHORT).show()
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
                val jsonO = json.getJSONObject(0)
                tvMonedas.text = "${tvMonedas.text} ${jsonO.getString("monedas")}"
            }, 5000)
        }finally {
            Log.d("TAG", "No se pudo w")
        }

        //-------------
        val diaDeLaSemana = LocalDate.now().dayOfWeek.value

        progreso.progress = diaDeLaSemana-1

        // Define un arreglo con todas tus ImageViews
        val imageViews = arrayOf(IvLunes, IvMartes, IvMiercoles, IvJueves, IvViernes, IvSabado, IvDomingo)

// Define un arreglo con todos tus drawables
        val drawables = arrayOf(R.drawable.lunes, R.drawable.martes, R.drawable.miercoles, R.drawable.jueves, R.drawable.viernes, R.drawable.sabado, R.drawable.domingo)

// Verifica si hoy es lunes
        if (diaDeLaSemana == 1) for (i in imageViews.indices)  imageViews[i].setImageResource(drawables[i])

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

        var result =""
        Log.d("Subtipo", "Subtipo de actividad: $subtipo")
        when (subtipo) {
            "escritura" -> actividadesMP.popEscritura(contsto,tvTit.text.toString(), tvDescripcion.text.toString(),puntos.toString())
            "foto" -> actividadesMP.popImagen(this,tvTit.text.toString(), tvDescripcion.text.toString())
            else -> Log.d("MiWorker", "Parámetro inválido")
        }

    }

    fun aux(context: Context,uid: String){
        val tvMonedas = (context as MenuPrincipal).findViewById<TextView>(R.id.tvMonedas)
        val tvNotificacion = (context as MenuPrincipal).findViewById<TextView>(R.id.cTVnoti)
        val btnEnviar = (context as MenuPrincipal).findViewById<TextView>(R.id.btnEnviarActividad)
        val IvCargaCircular = (context as MenuPrincipal).findViewById<CircularProgressIndicator>(R.id.CargaCircular)
        val layout = (context as MenuPrincipal).findViewById<RelativeLayout>(R.id.layoutTarea)

        val IvLunes = (context as MenuPrincipal).findViewById<ImageView>(R.id.Lunes)
        val IvMartes = (context as MenuPrincipal).findViewById<ImageView>(R.id.Martes)
        val IvMiercoles = (context as MenuPrincipal).findViewById<ImageView>(R.id.Miercoles)
        val IvJueves = (context as MenuPrincipal).findViewById<ImageView>(R.id.Jueves)
        val IvViernes = (context as MenuPrincipal).findViewById<ImageView>(R.id.Viernes)
        val IvSabado = (context as MenuPrincipal).findViewById<ImageView>(R.id.Sabado)
        val IvDomingo = (context as MenuPrincipal).findViewById<ImageView>(R.id.Domingo)

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
                tvMonedas.text = "$ ${objetoBuscado.getInt("monedas").toString()}"
            } else {
                Log.d("MiApp", "No se encontró el usuario")
            }
        }, 5000)

    tvNotificacion.text = "Actividad realizada!"
        btnEnviar.isEnabled = false

        val diaDeLaSemana = LocalDate.now().dayOfWeek.value
        val imageViews = arrayOf(IvLunes, IvMartes, IvMiercoles, IvJueves, IvViernes, IvSabado, IvDomingo)
        val drawables = arrayOf(R.drawable.lunesbien, R.drawable.martesbien, R.drawable.miercolesbien, R.drawable.juevesbien, R.drawable.viernesbien, R.drawable.sabadobien, R.drawable.domingobien)
        imageViews[diaDeLaSemana-1].setImageResource(drawables[diaDeLaSemana-1])

        IvCargaCircular.isVisible = false
        layout.isVisible = false

        val sharedPref = context.getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString("actNotificacion", "Actividad Realizada!")
        editor.putBoolean("actBotonEnviar", false)
        editor.apply()
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
        val FIRST_RUN = "first_run"
        val firstRun = sharedPref.getBoolean(FIRST_RUN, true)

        if (currentDay != lastOpenDay && firstRun == false) {
            actMenu.AsignarTareas(this)
            tvNotificacion.text = "Tienes una nueva asignacion!"
            btnEnviar.isEnabled = true
            prefs.edit().putLong(LAST_OPEN_DATE, currentDate).apply()
        }

        //-----------------------
        // Recuperar el texto de la variable elemento usando la misma clave
        val descripcion = sharedPref.getString("descripcion", "")
        val titulo = sharedPref.getString("titulo", "")

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
            finish()
            editor.putBoolean(FIRST_RUN, false)
            editor.apply()
        }
    }
}



