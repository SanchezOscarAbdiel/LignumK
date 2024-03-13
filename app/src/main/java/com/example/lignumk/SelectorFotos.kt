package com.example.lignumk

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SelectorFotos : AppCompatActivity() {
    private lateinit var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>

    lateinit var imgBtn: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_selector_fotos)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        imgBtn = findViewById(R.id.myImageButton)



        imgBtn.setOnClickListener {

        }


    }

    fun seleccionaFoto(){
        lateinit var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>
        pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()){
                uri ->
            if (uri!= null){
                imgBtn.setImageURI(uri)
                val sharedPref = this.getSharedPreferences("MI_APP", Context.MODE_PRIVATE)
                val editor = sharedPref.edit()
                editor.putString("fotoTarea", uri.toString())
                editor.apply()
            }
        }
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }


}