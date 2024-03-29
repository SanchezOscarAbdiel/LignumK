
package com.example.lignumk
import android.os.Bundle
import ConexionFirebase
import android.Manifest
import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.annotation.RequiresApi
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.io.FileNotFoundException
import java.time.LocalDate
import java.util.Calendar
import android.util.Base64
import android.util.TypedValue
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.lignumk.databinding.ActivityMenuPrincipalBinding
import com.example.lignumk.ui.theme.LignumKTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import java.lang.NullPointerException
import kotlin.properties.Delegates

val cFirebase = ConexionFirebase()
val actividadesMP = Actividades()

class MenuPrincipal : AppCompatActivity() {


    private lateinit var taskViewModel: TaskViewModel
    val actMenu = Actividades()
    private val LAST_OPEN_DATE = "lastOpenDate"

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>
    private var firstRun by Delegates.notNull<Boolean>()
    val contsto = this
    lateinit var binding: ActivityMenuPrincipalBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuPrincipalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        taskViewModel = ViewModelProvider(this).get(TaskViewModel::class.java)
        VerificaPrimeraVez()
        setCardColor()
        setDiasSemana(this)
        binding.CargaCircular.isVisible = false
        binding.progressIndicator.visibility = View.GONE
        binding.BotonSemanal.isEnabled = actividadesMP.sharedPref(this, "BotonSemanal",Boolean::class.java)!!

        firstRun = actividadesMP.sharedPref(this,"first_run",Boolean::class.java) == true

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this,gso)

        binding.lay.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        if (!actividadesMP.sharedPref(this,"first_run",Boolean::class.java)!!) {
            cardUsuario(this)
        }


            Log.d(getString(R.string.menuPrincipal), "pickMedia en OnCreate, con ${titulo}, ${puntos},")
            pickMedia = registerPickMedia()

        val iconoInsignia = actividades.sharedPref(this, "InsigniaActiva", String::class.java)
        Log.d("Icono insignia","IconoInsignia: $iconoInsignia")
        val fiftyDp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 58f, this.resources.displayMetrics).toInt()
        binding.IbInsignia.layoutParams = FrameLayout.LayoutParams(fiftyDp, fiftyDp)
        Glide.with(this)
            .load(iconoInsignia)
            .centerCrop()
            .into(binding.IbInsignia)
        binding.IbInsignia.setOnClickListener{
            actividadesMP.anuncio("Insignias","Desbloquea insignias juntando racha!\n" +
                    "Realiza actividades todos los dias, cuando hagas varias actividades consecutivas" +
                    "podrás ganar una insignia de merito.",this)
        }

        leaderBoard()

    }

    fun notificacion(view: View){
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        // Programar la alarma para que se dispare cada 24 horas
        val interval: Long = 24 * 60 * 60 * 1000 // 24 horas en milisegundos
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pendingIntent)
    }

    var titulo = ""
    var descripcion = ""
    var puntos = ""
    var tipo = ""
    override fun onResume() {
        super.onResume()
        Log.d("OnResume","Entrando a onResume")
        cardUsuario(this)
    }
    fun setCardColor(){
        val col = actividadesMP.sharedPref(contsto,"Skin",Int::class.java)!!
        val colorSeed = ContextCompat.getColor(contsto, col)
Log.d("color", "ingresa a color: $col $colorSeed")
// Aplicar el color recuperado a una vista
        binding.cardUsuario.setCardBackgroundColor(colorSeed)
        binding.cardAnuncio.setCardBackgroundColor(colorSeed)
        binding.cardInsignia.setCardBackgroundColor(colorSeed)
        binding.cardOpciones.setCardBackgroundColor(colorSeed)
        binding.cardLeaderboard.setCardBackgroundColor(colorSeed)
        binding.cardTareas.setCardBackgroundColor(colorSeed)
        binding.cardTareasSemanales.setCardBackgroundColor(colorSeed)
        binding.cardTienda.setCardBackgroundColor(colorSeed)
    }

    fun bottomSheetTienda(view: View){
        Tienda().show(supportFragmentManager, "Tienda")
        if(Tienda().isHidden){
            this.recreate()
        }
    }

    fun bottomSheetConfiguracion(view: View){
        Configuracion().show(supportFragmentManager, "Configuracion")
        if(Configuracion().isHidden){
            this.recreate()
        }
    }

    private fun registerPickMedia(): ActivityResultLauncher<PickVisualMediaRequest> {

        Log.d(getString(R.string.menuPrincipal),"Llega a register pickmedia con estos puntos: $puntos")

        return registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                try {
                    applicationContext.contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }

                actividadesMP.saveSharedPref(contsto,"fotoTarea", uri.toString())


                Log.d(getString(R.string.menuPrincipal),"PickMedia -> popImagen con puntos ${puntos} y binding $binding")
                    actividadesMP.popImagen(this, titulo, descripcion, puntos, binding.progressIndicator, binding,tipo)

            }
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun setDiasSemana(context: Context){

        actividadesMP.log(getString(R.string.menuPrincipal),Exception().stackTrace[0].fileName
                +" " + Exception().stackTrace[0].methodName)

        val jsonString = actividadesMP.sharedPref(context,"diasSemana",String::class.java)
        val type = object : TypeToken<Map<String, String>>() {}.type
        val recuperadoMap: Map<String, String> = Gson().fromJson(jsonString, type)

        val dias = mapOf(
            "IvLunes" to R.id.IvLunes,
            "IvMartes" to R.id.IvMartes,
            "IvMiercoles" to R.id.IvMiercoles,
            "IvJueves" to R.id.IvJueves,
            "IvViernes" to R.id.IvViernes,
            "IvSabado" to R.id.IvSabado,
            "IvDomingo" to R.id.IvDomingo
        )

        for ((dia, id) in dias) {
            val imageView: ImageView = findViewById(id)
            val drawableId = resources.getIdentifier(recuperadoMap[dia], "drawable", packageName)
            imageView.setImageResource(drawableId)
        }

        val diaDeLaSemana = LocalDate.now().dayOfWeek.value
        binding.progressBar.progress = diaDeLaSemana-1
        binding.progressIndicator.visibility = View.GONE
    }

    fun cardUsuario(context: Context){
        actividadesMP.log(getString(R.string.menuPrincipal),Exception().stackTrace[0].fileName
                +" " + Exception().stackTrace[0].methodName)

        var nombre = actividadesMP.sharedPref(context,"UserName",String::class.java)
        val uid = actividadesMP.sharedPref(context,"UID",String::class.java)
        val foto = actividadesMP.sharedPref(context,"fotoPerfil",String::class.java)
        val tipo = actividadesMP.sharedPref(context,"tipoFoto",String::class.java)

        if (nombre != null) nombre = nombre.split(" ")[0]
        binding.nombreUsuario.text = getString(R.string.bienvenido, nombre)

        cFirebase.LeerDatos("Usuarios","Puesto","Empleado",this)

        try{

            binding.tvMonedas.text = "$ "+actividadesMP.objetoBuscado(
                actividadesMP.leeArchivo(this,"Usuarios"),
                uid!!,"monedas",Int::class) .toString()

            if (tipo == "uri"){
                    val decodedString = Base64.decode(foto, Base64.DEFAULT)
                    val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                    binding.fotoPerfil.setImageBitmap(decodedByte)

            }else{
                val user = Firebase.auth.currentUser
                user?.let {
                    val photoURL = it.photoUrl
                    Glide.with(this)
                        .load(photoURL)
                        .into(binding.fotoPerfil)
                }
            }
        }catch (e: FileNotFoundException) {

            actividadesMP.log(getString(R.string.menuPrincipal),Exception().stackTrace[0].fileName
                    +" " + Exception().stackTrace[0].methodName +"\n" +
                    "Archivo no encontrado, reintentando en 5 segundos $e")

            Handler(Looper.getMainLooper()).postDelayed({
                cardUsuario(this)
            }, 5000)

        }
    }

    fun cardSemanal(view: View) {
        actividadesMP.log(getString(R.string.menuPrincipal),Exception().stackTrace[0].methodName)
        binding.CargaCircular.isVisible = true
        val sharedPref = this.getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
        puntos = actividadesMP.sharedPref(contsto, "puntosSemanal", String::class.java)!!
        titulo = actividadesMP.sharedPref(contsto, "tituloSemanal", String::class.java)!!
        descripcion =
            actividadesMP.sharedPref(contsto, "descripcionSemanal", String::class.java)!!
        tipo = "semanal"

        Log.d(getString(R.string.menuPrincipal),"Card semanal con ${puntos}, ${titulo}, ${descripcion}, $tipo")

        when (sharedPref.getString("subtipoSemanal", "")) {
            "encuesta" -> {
                actividadesMP.log(getString(R.string.menuPrincipal)+"➡ "+getString(R.string.actividades)
                    ,Exception().stackTrace[0].methodName+
                "➡ popEncuesta con ${titulo}, ${descripcion} , ${puntos}")

                actividadesMP.popEncuesta(
                    contsto,
                    titulo,
                    descripcion,
                    puntos,
                    binding.progressIndicator,
                    binding
                )
            }

            "foto" -> {
                Log.d("a", "Binding -> $binding")
                MaterialAlertDialogBuilder(contsto)
                    .setTitle(titulo)
                    .setMessage(descripcion)
                    .setPositiveButton("Aceptar") { dialog, which ->
                        pickMedia.launch(PickVisualMediaRequest())
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()

            }
        }

        binding.progressIndicator.visibility = View.GONE
    }

    fun getImages(context: Context):List<String>{
        return listOf(actividadesMP.sharedPref(context,"AvatarActivo",String::class.java)!!)
    }

    fun DepCard(view: View){
        val v = if (binding.layoutTarea.visibility == View.GONE) View.VISIBLE else View.GONE
        TransitionManager.beginDelayedTransition(binding.lay, AutoTransition())
        binding.layoutTarea.visibility = v

    }

    fun cardDiaria(view: View){
        actividadesMP.log(getString(R.string.menuPrincipal),Exception().stackTrace[0].methodName)

        val subtipo = actividadesMP.sharedPref(contsto,"subtipo",String::class.java)
        puntos = actividadesMP.sharedPref(contsto,"puntos",String::class.java)!!
        titulo = binding.cTVtitulo.text.toString()
        descripcion = binding.cTVDescripcion.text.toString()
        tipo = "diaria"

        binding.CargaCircular.isVisible = true
        Log.d("Subtipo", "Subtipo de actividad: $subtipo")
        when (subtipo) {
            "escritura" -> {
                actividadesMP.log(getString(R.string.menuPrincipal)+"➡"+getString(R.string.actividades)
                    ,Exception().stackTrace[0].methodName+
                        "➡ popEscritura con ${binding.cTVtitulo.text}, ${ binding.cTVDescripcion.text} , ${puntos}")

                actividadesMP.popEscritura(
                    contsto,
                    binding.cTVtitulo.text.toString(),
                    binding.cTVDescripcion.text.toString(),
                    puntos,
                    binding.progressIndicator,
                    binding
                )
            }
            "foto" -> pickMedia.launch(PickVisualMediaRequest())
            "seleccionMultiple" -> {
                actividadesMP.log(getString(R.string.menuPrincipal)+"➡ "+getString(R.string.actividades)
                    ,Exception().stackTrace[0].methodName+
                            "➡ popSeleccionMultipe con ${binding.cTVtitulo.text}, ${ binding.cTVDescripcion.text} , ${puntos}")

                actividadesMP.popSeleccionMultiple(
                    contsto,
                    binding.cTVtitulo.text.toString(),
                    binding.cTVDescripcion.text.toString(),
                    puntos,
                    binding.progressIndicator,
                    binding
                )
            }
        }
        binding.progressIndicator.visibility = View.GONE
    }

    fun auxSemanal(context: Context, uid: String, binding: ActivityMenuPrincipalBinding) {
        Log.d(context.getString(R.string.menuPrincipal), "llega a auxSemanal " + "con ${uid} y binding $binding")

        Handler(Looper.getMainLooper()).postDelayed({
            actividadesMP.saveSharedPref(context,"BotonSemanal",false)
            binding.BotonSemanal.isEnabled = false
            actividadesMP.log(context.getString(R.string.menuPrincipal)+"➡ "+context.getString(R.string.actividades)
                ,"➡ Objeto buscado")

                binding.tvMonedas.text = actividadesMP.objetoBuscado(
                    actividadesMP.leeArchivo(context,"Usuarios"),
                    uid,"monedas",Int::class) .toString()
            actividadesMP.saveSharedPref(context,"tituloSemanal","Realizada!")

            binding.carouselRecyclerView.adapter = CarouselAdapter(images = getImages(context),"Realizada!")
            binding.CargaCircular.isVisible = false
        }, 10000)


    }
    fun aux(context: Context, uid: String, binding: ActivityMenuPrincipalBinding) {
        // Recuperar y mostrar las monedas del usuario
        fun actualizarMonedas(context: Context, uid: String) {
            try {
                val monedas = actividadesMP.objetoBuscado(
                    actividadesMP.leeArchivo(context, "Usuarios"),
                    uid, "monedas", Int::class
                ).toString()
                binding.tvMonedas.text = monedas
            } catch (e: NullPointerException) {
                // Si se produce una excepción, reintenta después de un corto intervalo de tiempo
                Handler(Looper.getMainLooper()).postDelayed({
                    actualizarMonedas(context, uid)
                }, 5000)
            }
        }
        Handler(Looper.getMainLooper()).postDelayed({
            actualizarMonedas(context, uid)
        }, 5000)



        // Configurar la imagen del día de la semana
        val diaDeLaSemana = LocalDate.now().dayOfWeek.value
        val imageViews = arrayOf(
            binding.IvLunes, binding.IvMartes, binding.IvMiercoles,
            binding.IvJueves, binding.IvViernes, binding.IvSabado, binding.IvDomingo
        )
        val drawables = arrayOf(
            R.drawable.lunesbien, R.drawable.martesbien, R.drawable.miercolesbien,
            R.drawable.juevesbien, R.drawable.viernesbien, R.drawable.sabadobien, R.drawable.domingobien
        )
        imageViews[diaDeLaSemana - 1].setImageResource(drawables[diaDeLaSemana - 1])

        // Ocultar elementos de la interfaz de usuario
        binding.CargaCircular.isVisible = false
        binding.layoutTarea.isVisible = false

        // Actualizar las SharedPreferences con la actividad del día
        val dias = listOf("IvLunes", "IvMartes", "IvMiercoles", "IvJueves", "IvViernes", "IvSabado", "IvDomingo")
        val nombreDelDia = dias[diaDeLaSemana - 1]
        val jsonString = actividadesMP.sharedPref(context, "diasSemana", String::class.java)
        val type = object : TypeToken<Map<String, String>>() {}.type
        val recuperadoMap: Map<String, String> = Gson().fromJson(jsonString, type)
        val mutableMap = recuperadoMap.toMutableMap()
        val valorActual = mutableMap[nombreDelDia]
        mutableMap[nombreDelDia] = "$valorActual" + "bien"
        val nuevoJsonString = Gson().toJson(mutableMap.toMap())
        actividadesMP.saveSharedPref(context, "diasSemana", nuevoJsonString)

        // Actualizar las SharedPreferences con la notificación y la racha
        val racha = actividadesMP.sharedPref(context, "racha", Int::class.java)
        actividadesMP.saveSharedPref(context, "actNotificacion", "Actividad Realizada!")
        actividadesMP.saveSharedPref(context, "actBotonEnviar", false)
        actividadesMP.saveSharedPref(context, "racha", racha!! + 1)

        //Revisar si tiene una racha:
        actividadesMP.Insignias(context)
        val fiftyDp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 75f, context.resources.displayMetrics).toInt()
        binding.IbInsignia.layoutParams = FrameLayout.LayoutParams(fiftyDp, fiftyDp)
        val iconoInsignia = actividades.sharedPref(context, "InsigniaActiva", String::class.java)

// Cargar la imagen desde la URL en el "iconoInsignia" utilizando Glide
        Glide.with(context)
            .load(iconoInsignia)
            .into(binding.IbInsignia)

        // Actualizar la interfaz de usuario con la notificación y el estado del botón
        binding.cTVnoti.text = actividadesMP.sharedPref(context, "actNotificacion", String::class.java)
        binding.btnEnviarActividad.isEnabled = actividadesMP.sharedPref(context, "actBotonEnviar", Boolean::class.java) == true
        binding.CargaCircular.isVisible = false
    }

    data class Usuario(
        val fotoPerfil: String,
        val racha: Int
    )

    fun leaderBoard() {
        val jsonUsuario = actividades.leeArchivo(this,"Usuarios")
        val jsonUsuarioString = jsonUsuario.toString()
        val listType = object : TypeToken<List<Usuario>>() {}.type
        val items: List<Usuario> = Gson().fromJson(jsonUsuarioString , listType)

        // Ordenar la lista de usuarios por la racha en orden descendente
        val usuariosOrdenados = items.sortedByDescending { it.racha }

        // Tomar solo los primeros tres usuarios de la lista ordenada
        val primerosTresUsuarios = usuariosOrdenados.take(3)

        // Crear una lista de pares (fotoPerfil, racha) de los tres usuarios con la racha más alta
        val photoUrlsWithRacha = primerosTresUsuarios.map { Pair(it.fotoPerfil, it.racha) }

        // Crear y asignar el adaptador del carrusel con la lista de pares (fotoPerfil, racha)
        binding.carouselLeaderboard.adapter = CarouselAdapterLeader(imagesWithRacha = photoUrlsWithRacha)
    }




    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onStart() {
        super.onStart()
        // Obtiene la fecha almacenada previamente (si existe)
        val lastOpenDate = actividadesMP.sharedPref(contsto,LAST_OPEN_DATE,Long::class.java)
        // Obtiene la fecha actual
        val currentDate = Calendar.getInstance().timeInMillis
        // Obtiene la fecha actual sin la hora (para comparar solo el día)
        // Obtiene la fecha actual y la fecha almacenada sin la hora (para comparar solo el día)
        val currentDay = Calendar.getInstance().apply {
            timeInMillis = currentDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val lastOpenDay = Calendar.getInstance().apply {
            timeInMillis = lastOpenDate!!
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (currentDay != lastOpenDay && !firstRun) {
            actMenu.AsignarTareas(this,"diaria","Tareas")
            actividadesMP.saveSharedPref(contsto,"actNotificacion","Tienes una nueva asignacion!")
            actividadesMP.saveSharedPref(contsto,"actBotonEnviar",true)
            actividadesMP.saveSharedPref(contsto,LAST_OPEN_DATE,currentDate)

            //Reinicia dias de la semana si es lunes:
            val diaDeLaSemana = LocalDate.now().dayOfWeek.value
            if (diaDeLaSemana == 1) { // Si es lunes
                binding.BotonSemanal.alpha= 0f
                binding.BotonSemanal.isEnabled = true
                actividadesMP.saveSharedPref(this, "BotonSemanal", true)
                val diasSemana: Map<String, String> =
                    mapOf("IvLunes" to "lunes", "IvMartes" to "martes", "IvMiercoles" to "miercoles",
                        "IvJueves" to "jueves","IvViernes" to "viernes","IvSabado" to "sabado",
                        "IvDomingo" to "domingo")

                val jsonString = Gson().toJson(diasSemana)

                actividadesMP.saveSharedPref(contsto,"diasSemana",jsonString)

                //Tarea semanal
                actMenu.AsignarTareas(this,"semanal","Tareassemanal")

            }else{
                val dias = listOf("IvLunes", "IvMartes", "IvMiercoles",
                    "IvJueves", "IvViernes", "IvSabado", "IvDomingo")
// Obtiene el nombre del día correspondiente al número del día de la semana de ayer
                val nombreDelDia = dias[diaDeLaSemana - 2]
                val jsonString = actividadesMP.sharedPref(contsto,"diasSemana",String::class.java)

                val type = object : TypeToken<Map<String, String>>() {}.type
                val recuperadoMap: Map<String, String> = Gson().fromJson(jsonString, type)
                val mutableMap = recuperadoMap.toMutableMap()

                // Reemplaza el valor por el nuevo valor
                val valorActual = mutableMap[nombreDelDia]
                if (!valorActual!!.contains("bien")){
                    mutableMap[nombreDelDia] = "$valorActual" + "mal"
                    val nuevoJsonString = Gson().toJson(mutableMap.toMap())
                    actividadesMP.saveSharedPref(contsto,"diasSemana",nuevoJsonString)
                }
            }
        }

        //-----------------------
        // Recuperar el texto de la variable elemento usando la misma clave
        binding.cTVnoti.text = actividadesMP.sharedPref(contsto,"actNotificacion",String::class.java)
        binding.cTVDescripcion.text = actividadesMP.sharedPref(contsto,"descripcion",String::class.java)
        binding.cTVtitulo.text =  actividadesMP.sharedPref(contsto,"titulo",String::class.java)
        binding.btnEnviarActividad.isEnabled = actividadesMP.sharedPref(contsto,"actBotonEnviar",Boolean::class.java) == true
        val carouselRecyclerView: RecyclerView = findViewById(R.id.carouselRecyclerView)
        carouselRecyclerView.adapter = CarouselAdapter(images = getImages(this@MenuPrincipal),
            actividadesMP.sharedPref(contsto,"tituloSemanal",String::class.java)!!)

    }

    private fun VerificaPrimeraVez(){
        if (actividadesMP.sharedPref(contsto,"first_run",Boolean::class.java) == true) {

            finish()
            val intent = Intent(this, PrimeraVez::class.java)
            startActivity(intent)


        }
    }

}