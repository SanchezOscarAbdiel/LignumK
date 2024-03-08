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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


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
            cardUsuario()

    }

    fun cardUsuario(){
        val sharedPref = getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
        var nombre = sharedPref.getString("UserName", "")
        var uid = sharedPref.getString("UID", "")
        var foto = sharedPref.getString("fotoPerfil","")
        var tipo = sharedPref.getString("tipoFoto","")
        if (nombre != null) nombre = nombre.split(" ")[0]

        nombreUsuario.text = "${nombreUsuario.text} $nombre"

        Log.d("cardUsuario","Entrando a card \nnombre: $nombre \nfoto: $foto, \ntipo: $tipo")

        cFirebase.LeerDatos("Usuarios","Puesto","Empleado",this)
        try{
            val json = actividadesMP.leeArchivo(this,"Usuarios")
            val jsonO = json.getJSONObject(0)
            tvMonedas.text = "${tvMonedas.text} ${jsonO.getInt("monedas").toString()}"

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
        Log.d("SEMANA", "Dia de la semana $diaDeLaSemana")
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

        var result =""
        when (subtipo) {
            "escritura" -> actividadesMP.popEscritura(this,tvTit.text.toString(),tvDescripcion.text.toString()){ inputText ->
                CoroutineScope(Dispatchers.Main).launch {
                    result = actividadesMP.samAItexto(tvTit.toString(),tvDescripcion.text.toString(),inputText,"15").toString()
                }
            }
            "foto" -> actividadesMP.popImagen(this,tvTit.text.toString(), tvDescripcion.text.toString())
            else -> Log.d("MiWorker", "Parámetro inválido")
        }


        actividadesMP.popRetroalimentacion(contsto,tvTit.text.toString(),result) { number ->
        }
    }


//actividadesMP.popImagen(this,tvTit.text.toString(),tvDescripcion.text.toString())


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
        Log.d("MiWorker", "Last: ${lastOpenDay}, current: $currentDay")
        // Compara las fechas
        val sharedPref = getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
        val FIRST_RUN = "first_run"
        val firstRun = sharedPref.getBoolean(FIRST_RUN, true)
        Log.d("MiWorker", "First run $firstRun")
        if (currentDay != lastOpenDay && firstRun == false) {
            actMenu.AsignarTareas(this)
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
        //tvRand.text = nRandom
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



