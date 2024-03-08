package com.example.lignumk

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter

import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
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
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import coil.compose.ImagePainter
import coil.compose.rememberImagePainter
import com.bumptech.glide.Glide


class PrimeraVez : AppCompatActivity() {
val actividades = Actividades()

    lateinit var BtnGoogle: Button
    lateinit var SpnPuesto: Spinner
    lateinit var chGrop: ChipGroup
    lateinit var imgBtn: ImageButton
    lateinit var cbCorreo: CheckBox
    lateinit var gifCarga: ImageView

    var esAut = false
    var esSpn = false
    var Puid = ""
    lateinit var Pimg:ByteArray
    lateinit var Pspinner: String



    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityPrimeraVezBinding
    private lateinit var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrimeraVezBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //setContentView(R.layout.activity_primera_vez)
        BtnGoogle = findViewById(R.id.SingingGoogle)
        auth = Firebase.auth
        SpnPuesto = findViewById(R.id.SpnPuestos)
        chGrop = findViewById(R.id.chipGroup)
        imgBtn = findViewById(R.id.ImButtonFotoPerfil)
        cbCorreo = findViewById(R.id.CbNotificacion)
        gifCarga = findViewById(R.id.imgCargando)


        Glide.with(this).load(R.drawable.cargando).into(gifCarga)
        gifCarga.visibility = View.GONE

        pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()){
                uri ->
            if (uri!= null){
                imgBtn.setImageURI(uri)
                val inputStream = contentResolver.openInputStream(uri)
                Pimg = inputStream?.readBytes()!!
                val sharedPref = this.getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
                val editor = sharedPref.edit()
                editor.putString("fotoPerfil", uri.toString())
                editor.putString("tipoFoto","uri")
                editor.apply()

            }else{

            }
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this,gso)

        //DescargaArchivos
        cFirebase.LeerDatos("Tareas", "tipo", "diaria", this)

        //AsignaTareas
        actividades.AsignarTareas(this) //Se lee el archivo y se extrae la tarea en el momento

        rellenaSpin()


    }


    fun seleccionaImagen(view: View){

        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    }

    fun continuar(view: View){

        gifCarga.visibility = View.VISIBLE
        val chipsSeleccionados = chGrop.checkedChipIds.map { id ->
            val chip = findViewById<Chip>(id)
            chip.text.toString()
        }

        if(chipsSeleccionados.isNotEmpty() && esAut && esSpn){

            val jsonObject = JSONObject()

// Agregar las propiedades del mapa al objeto JSON
            jsonObject.put("coleccion", "Usuarios")
            jsonObject.put("documento", Puid)
            jsonObject.put("Fperfil", Pimg)
            jsonObject.put("Notificaciones", cbCorreo.isChecked)
            jsonObject.put("Puesto", Pspinner)
            jsonObject.put("Ddescanso", chipsSeleccionados)
            jsonObject.put("monedas",0)

// Convertir el objeto JSON a una cadena JSON
            val jsonDatos = jsonObject.toString()

// Imprimir el JSON (puedes guardarlo en un archivo o enviarlo a un servidor)
            Log.d("Datos en JSON", jsonDatos)
            cFirebase.PostData(jsonDatos)

            val sharedPref = this.getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.putString("UID", Puid)
            editor.apply()

            Thread.sleep(5000)

            val intent = Intent(this, MenuPrincipal::class.java)
            startActivity(intent)

        }else{
            Toast.makeText(this,"Selecciona todos los campos.", Toast.LENGTH_SHORT).show()
            gifCarga.visibility = View.GONE
        }
    }

    fun SelectedChip(view: View){
        val chipsSeleccionados = chGrop.checkedChipIds // Obtiene los IDs de los Chips seleccionados

        for (id in chipsSeleccionados) {
            val chip = findViewById<Chip>(id)
            Log.d("ChipGroup", "Chip seleccionado: ${chip.text}")
        }
    }

    fun rellenaSpin() {

// Crear un adaptador para el spinner con el array de recursos
        val adapter = ArrayAdapter.createFromResource(this, R.array.combo_puestos, android.R.layout.simple_spinner_item)
        SpnPuesto.adapter = adapter

        SpnPuesto.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val item = adapter.getItem(position)

                Pspinner = item.toString()
                Toast.makeText(this@PrimeraVez, "Item seleccionado: $Pspinner", Toast.LENGTH_SHORT).show()
                if (position != 0) esSpn = true
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }

        }
    }


    fun SingingGoogle(view: View){
        val signIntent = googleSignInClient.signInIntent
        launcher.launch(signIntent)

    }

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){
        result->
        if(result.resultCode == RESULT_OK){
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            manageResule(task)
            Log.d("Google", "No error: ${result.resultCode}")
            Log.d("Google", "No error: ${result.data}")
            Toast.makeText(this,"Si Autenticado, ${result.resultCode}", Toast.LENGTH_SHORT).show()
            Toast.makeText(this,"Si Autenticado, ${result.data}", Toast.LENGTH_SHORT).show()
        }else{
            Log.d("Google", "Error: ${result.resultCode}")
            Log.d("Google", "Error: ${result.data}")
            Toast.makeText(this,"No Autenticado, ${result.resultCode}", Toast.LENGTH_SHORT).show()
            Toast.makeText(this,"No Autenticado, ${result.data}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun manageResule(task: Task<GoogleSignInAccount>) {
        if (task.isSuccessful){
            val account:GoogleSignInAccount? = task.result
            if (account!=null){
                updateUi(account)
            }
        }
    }

    private fun updateUi(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener {

            if (it.isSuccessful){
                Toast.makeText(this,"Autenticado", Toast.LENGTH_SHORT).show()
                verifyUser()
            }else{
                Toast.makeText(this,"?????????", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun verifyUser(){
        val user = Firebase.auth.currentUser
        user?.let {
            val name = it.displayName
            val email = it.email
            val photoURL = it.photoUrl
            val emailVerified = it.isEmailVerified
            Puid = it.uid

            //Pase de parametros

            if(emailVerified) {
                Toast.makeText(this, "Correo Autenticado", Toast.LENGTH_SHORT).show()
                esAut = true
            }

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
                        binding.ImButtonFotoPerfil.setImageBitmap(image)

                        val sharedPref = this.getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
                        val editor = sharedPref.edit()
                        editor.putString("fotoPerfil", image.toString())
                        editor.putString("UserName",name)
                        editor.putString("tipoFoto","bitmap")
                        editor.apply()

                        // Supongamos que tienes un Bitmap llamado "image"
                        val stream = ByteArrayOutputStream()
                        image?.compress(Bitmap.CompressFormat.PNG, 100, stream)
                        Pimg = stream.toByteArray()



                    }catch (e:InterruptedException){
                        Toast.makeText(this,"Error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser!=null) verifyUser()
    }

}

/*
* Hacer consultas de tareas
* (Dos veces por si tiene que descargar el archivo)
* Calcular workmanager para 7 am
* Rellenar datos
*   Guardar dia actual
*   Dia de descanso
*   Datos personales
*
* Iniciar sesion con google
* */