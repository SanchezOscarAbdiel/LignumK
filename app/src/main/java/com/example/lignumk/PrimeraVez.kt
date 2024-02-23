package com.example.lignumk

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.concurrent.Executors
import kotlin.random.Random


class PrimeraVez : AppCompatActivity() {
val actividades = Actividades()

    lateinit var BtnGoogle: Button

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_primera_vez)
        BtnGoogle = findViewById(R.id.SingingGoogle)
        auth = Firebase.auth
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this,gso)

        //DescargaArchivos
        //tvA.text ="Descargando archivos..."
        cFirebase.LeerDatos("Tareas", "tipo", "diaria", this)
        Thread.sleep(2_000)

        //Inicia sesion

        //Dias de descanso

        //AsignaTareas
        val delay = actividades.SincronizaTareas()
        Log.d("Primera vez", "delay de la aplicacion: ${delay}")
        actividades.oneTimeR(applicationContext,15,"AsignarTareas") //Se lee el archivo y se extrae la tarea en el momento
       // tvA.text ="Asignando primera tareas"
        actividades.oneTimeR(applicationContext,delay,"AsignarTareas") //Se lee el archivo y se extrae la a las 7 am
        //tvA.text ="Asignando Ciclos"
      actividades.oneTimeR(applicationContext,delay,"EstablecerCiclo") //Se establece el ciclo una vez que haya pasado el primer delay a las 7 de la mañana


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

            //Pase de parametros

            if(emailVerified) Toast.makeText(this,"Correo Autenticado", Toast.LENGTH_SHORT).show()

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