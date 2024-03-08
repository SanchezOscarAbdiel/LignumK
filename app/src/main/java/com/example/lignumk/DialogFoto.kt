package com.example.lignumk

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts

class DialogFoto : ComponentActivity() {
    private lateinit var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>

    lateinit var imgBtn: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imgBtn = findViewById(R.id.myImageButton)

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
    }

    fun seleccionaFoto(view: View){
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    }
}