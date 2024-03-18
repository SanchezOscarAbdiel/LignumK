package com.example.lignumk

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.Manifest
import android.content.ContentValues.TAG
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter

import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import java.util.concurrent.CompletableFuture
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.lignumk.databinding.ActivityPrimeraVezBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import kotlin.random.Random
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.gson.Gson
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch


class PrimeraVez : AppCompatActivity() {
    val actividades = Actividades()

    lateinit var BtnGoogle: Button
    lateinit var SpnPuesto: Spinner
    lateinit var chGrop: ChipGroup
    lateinit var imgBtn: ImageButton
    lateinit var cbCorreo: CheckBox
    lateinit var progressIndicator: LinearProgressIndicator

    var esAut = false
    var esSpn = false
    var Puid = ""
    lateinit var Pimg: ByteArray
    lateinit var Pspinner: String
    companion object { private const val CAMERA_REQUEST_CODE = 100 }

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityPrimeraVezBinding
    private lateinit var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrimeraVezBinding.inflate(layoutInflater)
        setContentView(binding.root)
        BtnGoogle = findViewById(R.id.SingingGoogle)
        auth = Firebase.auth
        SpnPuesto = findViewById(R.id.SpnPuestos)
        chGrop = findViewById(R.id.chipGroup)
        imgBtn = findViewById(R.id.ImButtonFotoPerfil)
        cbCorreo = findViewById(R.id.CbNotificacion)
        progressIndicator = findViewById(R.id.progress_indicatorPV)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
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

                imgBtn.setImageURI(uri)
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()
                val encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT)
                val sharedPref = this.getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
                val editor = sharedPref.edit()
                editor.putString("fotoPerfil", encodedImage)
                editor.putString("tipoFoto", "uri")
                editor.apply()

            } else {

            }
        }


        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        //DescargaArchivos
        cFirebase.LeerDatos("Tareas", "tipo", "diaria", this)
        cFirebase.LeerDatos("Tareas", "tipo", "semanal", this)

        //AsignaTareas
        actividades.AsignarTareas(this,"diaria","Tarea") //Se lee el archivo y se extrae la tarea en el momento

        rellenaSpin()


    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // El permiso fue concedido, puedes usar la cámara
                    //useCamera()
                } else {
                    // El permiso fue denegado
                }
                return
            }
            else -> {
                // Ignora todos los otros códigos de solicitud
            }
        }
    }

    fun seleccionaImagen(view: View) {

        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    }

    fun continuar(view: View) {
        val sharedPref = this.getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
        progressIndicator.visibility = View.VISIBLE
        val chipsSeleccionados = chGrop.checkedChipIds.map { id ->
            val chip = findViewById<Chip>(id)
            chip.text.toString()
        }

        if (chipsSeleccionados.isNotEmpty() && esAut && esSpn) {

            val jsonObject = JSONObject()

// Agregar las propiedades del mapa al objeto JSON
            jsonObject.put("coleccion", "Usuarios")
            jsonObject.put("documento", Puid)
            jsonObject.put("UID", Puid)
            jsonObject.put("Notificaciones", cbCorreo.isChecked)
            jsonObject.put("Puesto", Pspinner)
            jsonObject.put("Ddescanso", chipsSeleccionados)
            jsonObject.put("monedas", 0)
            jsonObject.put("racha",0)
            jsonObject.put("respuestas","")

// Convertir el objeto JSON a una cadena JSON
            val jsonDatos = jsonObject.toString()

            lifecycleScope.launch {
                try {
                    cFirebase.PostData(jsonDatos).await()

                    //Habilita drawables dia de la semana
                    val diasSemana: Map<String, String> =
                        mapOf("IvLunes" to "lunes", "IvMartes" to "martes", "IvMiercoles" to "miercoles",
                            "IvJueves" to "jueves","IvViernes" to "viernes","IvSabado" to "sabado",
                            "IvDomingo" to "domingo")
                    val jsonString = Gson().toJson(diasSemana)

                    val editor = sharedPref.edit()
                    editor.putString("UID", Puid)
                    editor.putString("diasSemana", jsonString)
                    editor.putInt("racha",0)
                    editor.apply()
                    val intent = Intent(this@PrimeraVez, MenuPrincipal::class.java)
                    startActivity(intent)
                    this@PrimeraVez.finish()
                } catch (e: Exception) {
                    // Maneja cualquier excepción que pueda haber ocurrido durante la operación de Firebase
                    Log.e(TAG, "Error al actualizar los datos en Firebase", e)
                }
            }

        } else {
            Toast.makeText(this, "Selecciona todos los campos.", Toast.LENGTH_SHORT).show()
        }
    }

    fun SelectedChip(view: View) {
        val chipsSeleccionados = chGrop.checkedChipIds // Obtiene los IDs de los Chips seleccionados

        for (id in chipsSeleccionados) {
            val chip = findViewById<Chip>(id)
        }
    }

    fun rellenaSpin() {

// Crear un adaptador para el spinner con el array de recursos
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.combo_puestos,
            android.R.layout.simple_spinner_item
        )
        SpnPuesto.adapter = adapter

        SpnPuesto.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val item = adapter.getItem(position)

                Pspinner = item.toString()
                Toast.makeText(this@PrimeraVez, "Item seleccionado: $Pspinner", Toast.LENGTH_SHORT)
                    .show()
                if (position != 0) esSpn = true
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
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
                Toast.makeText(this, "Autenticado", Toast.LENGTH_SHORT).show()
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
            val email = it.email
            val photoURL = it.photoUrl
            val emailVerified = it.isEmailVerified
            Puid = it.uid

            //Pase de parametros

            if (emailVerified) {
                Toast.makeText(this, "Correo Autenticado", Toast.LENGTH_SHORT).show()
                esAut = true
            }

            var image: Bitmap? = null
            val imageurl = photoURL.toString()
            val executorService = Executors.newSingleThreadExecutor()
            executorService.execute {
                try {
                    val `in` = java.net.URL(imageurl).openStream()
                    image = BitmapFactory.decodeStream(`in`)
                } catch (e: Exception) {
                    Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
                }
                runOnUiThread {
                    try {
                        Thread.sleep(1000)
                        //Imagen placed
                        binding.ImButtonFotoPerfil.setImageBitmap(image)

                        val sharedPref = this.getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
                        val editor = sharedPref.edit()
                        editor.putString("fotoPerfil", image.toString())
                        editor.putString("UserName", name)
                        editor.putString("tipoFoto", "bitmap")
                        editor.apply()

                        // Supongamos que tienes un Bitmap llamado "image"
                        val stream = ByteArrayOutputStream()
                        image?.compress(Bitmap.CompressFormat.PNG, 100, stream)
                        Pimg = stream.toByteArray()


                    } catch (e: InterruptedException) {
                        Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
                    }
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
