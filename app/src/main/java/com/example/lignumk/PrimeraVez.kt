package com.example.lignumk
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.graphics.BitmapFactory
import android.content.ContentValues.TAG
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.lignumk.databinding.ActivityPrimeraVezBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import org.json.JSONObject
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import java.util.Calendar
import android.net.Uri
import android.provider.Settings
import android.util.TypedValue



class PrimeraVez : AppCompatActivity() {

    private val actividades = Actividades()

    private var esAut = false
    var esSpn = false
    private var puid = ""
    lateinit var pspinner: String

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityPrimeraVezBinding
    private lateinit var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrimeraVezBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        actividadesMP.saveSharedPref(this,"first_run",false)
        pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            binding.ImButtonFotoPerfil.setImageURI(uri)
            actividades.uriToBase64(this,uri!!,"fotoPerfil")
            actividades.saveSharedPref(this,"tipoFoto","uri")
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        //DescargaArchivosSingingGoogle
        cFirebase.LeerDatos("Tareas", "tipo", "diaria", this)
        cFirebase.LeerDatos("Tareas", "tipo", "semanal", this)
        cFirebase.LeerDatos("Tienda", "tipo", "potenciador", this)
        cFirebase.LeerDatos("Tienda", "tipo", "estetica", this)
        cFirebase.LeerDatos("Insignias","tipo","insignia",this)

        //AsignaTareas
        actividades.AsignarTareas(this,"diaria","Tareas") //Se lee el archivo y se extrae la tarea en el momento

        rellenaSpin()

    }


    fun seleccionaImagen(view: View) {

        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    }

    fun continuar(view: View) {
        binding.progressIndicatorPV.visibility = View.VISIBLE
        val chipsSeleccionados = binding.chipGroup.checkedChipIds.map { id ->
            val chip = findViewById<Chip>(id)
            chip.text.toString()
        }

        if (chipsSeleccionados.isNotEmpty() && esAut && esSpn) {

            val jsonObject = JSONObject()
            jsonObject.put("coleccion", "Usuarios")
            jsonObject.put("documento", puid)
            jsonObject.put("UID", puid)
            jsonObject.put("Notificaciones", binding.switchNotification.isChecked)
            jsonObject.put("Puesto", pspinner)
            jsonObject.put("Ddescanso", chipsSeleccionados)
            jsonObject.put("monedas", 0)
            jsonObject.put("racha", 0)
            jsonObject.put("respuestas", "")
            jsonObject.put("fotoPerfil", actividades.sharedPref(this@PrimeraVez,"fotoPerfil",String::class.java))
            jsonObject.put("tipoFoto",actividades.sharedPref(this@PrimeraVez,"tipoFoto",String::class.java))
            val jsonDatos = jsonObject.toString()
            cFirebase.PostData(jsonDatos)

            try {
                binding.switchNotification.setOnCheckedChangeListener{_, isChecked ->
                    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val intent = Intent(this, AlarmReceiver::class.java)
                    val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

                    if (isChecked) {
                        Toast.makeText(this, "Notificaciones activadas", Toast.LENGTH_SHORT).show()
                        // Programar la alarma para que se dispare cada 24 horas
                        val interval: Long = 24 * 60 * 60 * 1000 // 24 horas en milisegundos
                        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pendingIntent)
                    } else {
                        // Cancelar la alarma
                        Toast.makeText(this, "Notificaciones desactivadas", Toast.LENGTH_SHORT).show()
                        alarmManager.cancel(pendingIntent)
                    }
                }

                //Habilita drawables dia de la semana
                val diasSemana: Map<String, String> =
                    mapOf(
                        "IvLunes" to "lunes", "IvMartes" to "martes", "IvMiercoles" to "miercoles",
                        "IvJueves" to "jueves", "IvViernes" to "viernes", "IvSabado" to "sabado",
                        "IvDomingo" to "domingo"
                    )
                val jsonString = Gson().toJson(diasSemana)

                actividades.saveSharedPref(this@PrimeraVez, "UID", puid)
                actividades.saveSharedPref(this@PrimeraVez, "diasSemana", jsonString)
                actividades.saveSharedPref(this@PrimeraVez, "racha", 0)
                actividades.saveSharedPref(this@PrimeraVez,"AvatarComprados","https://firebasestorage.googleapis.com/v0/b/lignumbd-b32e7.appspot.com/o/maderita.png?alt=media&token=dd072e43-ef88-4b95-baa8-6e89ed0709d9")
                actividades.saveSharedPref(this@PrimeraVez,"AvatarActivo","https://firebasestorage.googleapis.com/v0/b/lignumbd-b32e7.appspot.com/o/maderita.png?alt=media&token=dd072e43-ef88-4b95-baa8-6e89ed0709d9")
                actividades.saveSharedPref(this@PrimeraVez,"Skin",R.color.seed)
                actividades.saveSharedPref(this@PrimeraVez,"SkinCompradas",R.color.seed.toString())
                actividades.saveSharedPref(this@PrimeraVez, "tipoPotenciadorActivo", "no")
                actividades.saveSharedPref(this@PrimeraVez, "potenciadorActivo", "no")
                actividades.saveSharedPref(this@PrimeraVez,"InsigniaActiva","https://firebasestorage.googleapis.com/v0/b/lignumbd-b32e7.appspot.com/o/hojita.png?alt=media&token=60027732-7d2f-4c8d-873d-5b909d9de4ba")
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar los datos en Firebase", e)
            }

            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(this@PrimeraVez, MenuPrincipal::class.java)
                startActivity(intent)
            }, 25000)

        } else {
            Toast.makeText(this, "Selecciona todos los campos.", Toast.LENGTH_SHORT).show()
        }
    }

    fun rellenaSpin() {
        val adapter = ArrayAdapter.createFromResource(this, R.array.combo_puestos, android.R.layout.simple_spinner_item)
        binding.SpnPuestos.adapter = adapter

        binding.SpnPuestos.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val item = adapter.getItem(position)

                pspinner = item.toString()
                esSpn = position != 0
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
    }


    fun SingingGoogle(view: View) {
        val signIntent = googleSignInClient.signInIntent
        launcher.launch(signIntent)

    }

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            manageResule(task)
            Toast.makeText(this, "Autenticado Exitosamente", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                this,
                "No Autenticado, codigo de error: ${result.resultCode}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun manageResule(task: Task<GoogleSignInAccount>) {
        if (task.isSuccessful) {
            val account: GoogleSignInAccount? = task.result
            if (account != null) {
                updateUi(account)
            }
        }
    }

    private fun updateUi(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener {

            if (it.isSuccessful) {
                verifyUser()
            } else {
                Toast.makeText(this, "?????????", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun verifyUser() {
        val user = Firebase.auth.currentUser
        user?.let {
            val name = it.displayName
            val photoURL = it.photoUrl
            val emailVerified = it.isEmailVerified
            puid = it.uid

            if (emailVerified) {
                Toast.makeText(this, "Correo Autenticado", Toast.LENGTH_SHORT).show()
                esAut = true
            }

            val imageurl = photoURL.toString()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val `in` = java.net.URL(imageurl).openStream()
                    val image = BitmapFactory.decodeStream(`in`)
                    withContext(Dispatchers.Main) {
                        binding.ImButtonFotoPerfil.scaleType = ImageView.ScaleType.CENTER_CROP
                        binding.ImButtonFotoPerfil.setImageBitmap(image)

                        actividades.saveSharedPref(this@PrimeraVez,"fotoPerfil",imageurl)
                        actividades.saveSharedPref(this@PrimeraVez,"UserName",name)
                        actividades.saveSharedPref(this@PrimeraVez,"tipoFoto","bitmap")

                    }
                } catch (e: Exception) {
                    Toast.makeText(this@PrimeraVez, "Error al cargar la imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) verifyUser()
    }

}
